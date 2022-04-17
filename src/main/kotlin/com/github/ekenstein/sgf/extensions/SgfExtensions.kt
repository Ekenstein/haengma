package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.utils.replace
import com.github.ekenstein.sgf.utils.replaceLast

operator fun SgfNode.plus(other: SgfNode): SgfNode {
    val allProperties = properties + other.properties
    return allProperties.fold(SgfNode.empty) { node, property -> node.addProperty(property) }
}

private fun SgfGameTree.appendNode(vararg properties: SgfProperty): SgfGameTree = copy(
    sequence = sequence + SgfNode(properties.toSet())
)

internal inline fun <reified T : SgfProperty> SgfNode.property() = properties.filterIsInstance<T>().singleOrNull()

inline fun <reified T : SgfProperty> SgfNode.addProperty(property: T) = copy(
    properties = properties.filter { it::class != property::class }.toSet() + property
)

inline fun <reified T : SgfProperty> SgfNode.removeProperty() = copy(
    properties = properties.filter { it::class != T::class }.toSet()
)

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToRootNode(property: T): SgfGameTree {
    val rootNode = sequence.firstOrNull()?.takeIf { !it.hasMoveProperties() }
    return if (rootNode != null) {
        copy(
            sequence = listOf(rootNode.addProperty(property)) + sequence.drop(1)
        )
    } else {
        copy(
            sequence = listOf(SgfNode(setOf(property))) + sequence
        )
    }
}

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToGameInfoNode(property: T): SgfGameTree {
    val gameInfoNode = sequence.singleOrNull() { it.hasGameInfoProperties() }

    return if (gameInfoNode != null) {
        val index = sequence.indexOf(gameInfoNode)
        copy(
            sequence = sequence.replace(index, gameInfoNode.addProperty(property))
        )
    } else {
        addPropertyToRootNode(property)
    }
}

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToLastNode(
    property: T,
    condition: (SgfNode) -> Boolean = { true }
): SgfGameTree {
    val lastNode = sequence.lastOrNull()?.takeIf(condition)
    return if (lastNode != null) {
        copy(
            sequence = sequence.replaceLast(lastNode.addProperty(property))
        )
    } else {
        appendNode(property)
    }
}

private inline fun <reified T : SgfProperty.Move> SgfGameTree.addMoveProperty(property: T): SgfGameTree =
    addPropertyToLastNode(property) {
        val condition = !it.hasRootProperties() && !it.hasSetupProperties()

        condition && when (property) {
            is SgfProperty.Move.B,
            is SgfProperty.Move.W -> !it.hasProperty<T>()
            else -> true
        }
    }

private fun SgfGameTree.addSetupProperty(property: SgfProperty.Setup): SgfGameTree {
    return when (sequence.lastOrNull()?.takeIf { !it.hasMoveProperties() }) {
        null -> appendNode(property)
        else -> addPropertyToLastNode(property)
    }
}

private inline fun <reified T : SgfProperty> SgfNode.hasProperty() = properties.filterIsInstance<T>().any()
private fun SgfNode.hasSetupProperties() = hasProperty<SgfProperty.Setup>()
private fun SgfNode.hasRootProperties() = hasProperty<SgfProperty.Root>()
private fun SgfNode.hasMoveProperties() = hasProperty<SgfProperty.Move>()
private fun SgfNode.hasGameInfoProperties() = hasProperty<SgfProperty.GameInfo>()

fun SgfGameTree.addProperty(property: SgfProperty): SgfGameTree {
    return when (property) {
        is SgfProperty.GameInfo -> addPropertyToGameInfoNode(property)
        is SgfProperty.Move -> addMoveProperty(property)
        is SgfProperty.Root -> addPropertyToRootNode(property)
        is SgfProperty.Setup -> addSetupProperty(property)
        is SgfProperty.Timing,
        is SgfProperty.Markup,
        is SgfProperty.Misc,
        is SgfProperty.MoveAnnotation,
        is SgfProperty.Private,
        is SgfProperty.NodeAnnotation -> addPropertyToLastNode(property)
    }
}
