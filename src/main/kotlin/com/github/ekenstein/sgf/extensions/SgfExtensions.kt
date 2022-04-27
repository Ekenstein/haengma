package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.utils.prepend
import com.github.ekenstein.sgf.utils.replace
import com.github.ekenstein.sgf.utils.replaceFirst
import com.github.ekenstein.sgf.utils.replaceLast

operator fun SgfNode.plus(other: SgfNode): SgfNode {
    val allProperties = properties + other.properties
    return allProperties.fold(SgfNode()) { node, property -> node.addProperty(property) }
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

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToRootNode(property: T) =
    when (val rootNode = sequence.firstOrNull()?.takeIf { !it.hasMoveProperties() }) {
        null -> copy(sequence = sequence.prepend(SgfNode(property)))
        else -> copy(sequence = sequence.replaceFirst(rootNode.addProperty(property)))
    }

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToGameInfoNode(property: T) =
    when (val gameInfoNode = sequence.singleOrNull { it.hasGameInfoProperties() }) {
        null -> addPropertyToRootNode(property)
        else -> copy(sequence = sequence.replace(sequence.indexOf(gameInfoNode), gameInfoNode.addProperty(property)))
    }

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToLastNode(
    property: T,
    addPropertyToNode: (SgfNode) -> SgfNode? = { it.addProperty(property) }
) = when (val lastNode = sequence.lastOrNull()?.let(addPropertyToNode)) {
    null -> appendNode(property)
    else -> copy(sequence = sequence.replaceLast(lastNode))
}

private inline fun <reified T : SgfProperty.Move> SgfGameTree.addMoveProperty(property: T): SgfGameTree =
    addPropertyToLastNode(property) { node ->
        val condition = !node.hasRootProperties() && !node.hasSetupProperties()
        val canPropertyBeAddedToNode = condition && when (property) {
            is SgfProperty.Move.B,
            is SgfProperty.Move.W -> !node.hasProperty<T>()
            else -> true
        }

        if (canPropertyBeAddedToNode) {
            node.addProperty(property)
        } else {
            null
        }
    }

private fun SgfGameTree.addSetupProperty(property: SgfProperty.Setup) = addPropertyToLastNode(property) {
    if (!it.hasMoveProperties()) {
        it.addProperty(property)
    } else {
        null
    }
}

private fun SgfGameTree.addNodeAnnotationProperty(property: SgfProperty.NodeAnnotation) =
    addPropertyToLastNode(property) { node ->
        val removeProperties = when (property) {
            is SgfProperty.NodeAnnotation.DM, // must not be mixed with UC, GB or GW within a node.
            is SgfProperty.NodeAnnotation.GB, // must not be mixed with GW, DM or UC within a node.
            is SgfProperty.NodeAnnotation.GW, // must not be mixed with GB, DM or UC within a node.
            is SgfProperty.NodeAnnotation.UC -> true // must not be mixed with DM, GB or GW
            is SgfProperty.NodeAnnotation.C,
            is SgfProperty.NodeAnnotation.V,
            is SgfProperty.NodeAnnotation.HO,
            is SgfProperty.NodeAnnotation.N -> false
        }

        val newNode = if (removeProperties) {
            node.removeProperty<SgfProperty.NodeAnnotation.DM>()
                .removeProperty<SgfProperty.NodeAnnotation.GB>()
                .removeProperty<SgfProperty.NodeAnnotation.GW>()
                .removeProperty<SgfProperty.NodeAnnotation.UC>()
        } else {
            node
        }

        newNode.addProperty(property)
    }

private fun SgfGameTree.addMoveAnnotationProperty(property: SgfProperty.MoveAnnotation) =
    addPropertyToLastNode(property) { node ->
        when (property) {
            is SgfProperty.MoveAnnotation.BM -> node.removeProperty<SgfProperty.MoveAnnotation.DO>()
                .removeProperty<SgfProperty.MoveAnnotation.IT>()
                .removeProperty<SgfProperty.MoveAnnotation.TE>()
                .addProperty(property)
            SgfProperty.MoveAnnotation.DO -> node.removeProperty<SgfProperty.MoveAnnotation.BM>()
                .removeProperty<SgfProperty.MoveAnnotation.IT>()
                .removeProperty<SgfProperty.MoveAnnotation.TE>()
                .addProperty(property)
            SgfProperty.MoveAnnotation.IT -> node.removeProperty<SgfProperty.MoveAnnotation.DO>()
                .removeProperty<SgfProperty.MoveAnnotation.BM>()
                .removeProperty<SgfProperty.MoveAnnotation.TE>()
                .addProperty(property)
            is SgfProperty.MoveAnnotation.TE -> node.removeProperty<SgfProperty.MoveAnnotation.DO>()
                .removeProperty<SgfProperty.MoveAnnotation.IT>()
                .removeProperty<SgfProperty.MoveAnnotation.BM>()
                .addProperty(property)
        }
    }

private inline fun <reified T : SgfProperty> SgfNode.hasProperty() = properties.filterIsInstance<T>().any()
private fun SgfNode.hasSetupProperties() = hasProperty<SgfProperty.Setup>()
private fun SgfNode.hasRootProperties() = hasProperty<SgfProperty.Root>()
private fun SgfNode.hasMoveProperties() = hasProperty<SgfProperty.Move>()
private fun SgfNode.hasGameInfoProperties() = hasProperty<SgfProperty.GameInfo>()

/**
 * Will add the given [property] to the game tree. The property will be added
 * to its appropriate node according to the appropriate style.
 * If that appropriate node isn't part of the game tree, the node will
 * be added to the tree in its appropriate position. E.g. root properties will be added to the root node,
 * and if a root node does not exist a root node will be added.
 *
 * Note that some properties may replace other properties, e.g. if they must not be mixed.
 * This case applies to:
 * * [SgfProperty.NodeAnnotation] properties, such as DM, GB, GW, UC.
 * * [SgfProperty.MoveAnnotation] properties, such as BM, DO, IT, TE.
 *
 * @param property The property that will be added to the game tree.
 * @return A new tree containing the added [property]
 */
fun SgfGameTree.addProperty(property: SgfProperty) = when (property) {
    is SgfProperty.GameInfo -> addPropertyToGameInfoNode(property)
    is SgfProperty.Move -> addMoveProperty(property)
    is SgfProperty.Root -> addPropertyToRootNode(property)
    is SgfProperty.Setup -> addSetupProperty(property)
    is SgfProperty.NodeAnnotation -> addNodeAnnotationProperty(property)
    is SgfProperty.MoveAnnotation -> addMoveAnnotationProperty(property)
    is SgfProperty.Timing,
    is SgfProperty.Markup,
    is SgfProperty.Misc,
    is SgfProperty.Private -> addPropertyToLastNode(property)
}
