package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.utils.NonEmptyList
import com.github.ekenstein.sgf.utils.nelOf
import com.github.ekenstein.sgf.utils.replace
import kotlin.math.ceil
import kotlin.reflect.KClass

operator fun SgfNode.plus(other: SgfNode): SgfNode {
    val allProperties = properties + other.properties
    return allProperties.fold(SgfNode()) { node, property -> node.addProperty(property) }
}

operator fun SgfNode.plus(property: SgfProperty): SgfNode = addProperty(property)

private fun SgfGameTree.appendNode(vararg properties: SgfProperty): SgfGameTree = copy(
    sequence = sequence + SgfNode(properties.toSet())
)

internal inline fun <reified T : SgfProperty> SgfNode.property() = properties.filterIsInstance<T>().singleOrNull()

inline fun <reified T : SgfProperty> SgfNode.addProperty(property: T): SgfNode = when (property) {
    is SgfProperty.Private -> copy(
        properties = properties.filter {
            it !is SgfProperty.Private || it.identifier != property.identifier
        }.toSet() + property
    )
    else -> copy(
        properties = properties.filter {
            it::class != property::class
        }.toSet() + property
    )
}

inline fun <reified T : SgfProperty> SgfNode.removeProperty() = removeProperty(T::class)

fun <T : SgfProperty> SgfNode.removeProperty(klass: KClass<T>) = copy(
    properties = properties.filter { it::class != klass }.toSet()
)

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToRootNode(property: T) =
    when (val rootNode = sequence.firstOrNull()?.takeIf { !it.hasMoveProperties() }) {
        null -> copy(sequence = nelOf(SgfNode(property)) + sequence)
        else -> copy(sequence = nelOf(rootNode.addProperty(property)) + sequence.drop(1))
    }

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToGameInfoNode(property: T) =
    when (val gameInfoNode = sequence.singleOrNull { it.hasGameInfoProperties() }) {
        null -> addPropertyToRootNode(property)
        else -> {
            val newSequence = sequence.replace(sequence.indexOf(gameInfoNode), gameInfoNode.addProperty(property))
            copy(
                sequence = NonEmptyList.fromListUnsafe(newSequence)
            )
        }
    }

private inline fun <reified T : SgfProperty> SgfGameTree.addPropertyToLastNode(
    property: T,
    addPropertyToNode: (SgfNode) -> SgfNode? = { it.addProperty(property) }
) = when (val lastNode = addPropertyToNode(sequence.last())) {
    null -> appendNode(property)
    else -> {
        val first = sequence.takeLast(sequence.size - 1)
        val newSequence = if (first.isEmpty()) {
            nelOf(lastNode)
        } else {
            NonEmptyList.fromListUnsafe(first) + lastNode
        }

        copy(
            sequence = newSequence
        )
    }
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

fun SgfGameTree.Companion.newGame(boardSize: Int, komi: Double, handicap: Int): SgfGameTree {
    val maxHandicap = maxHandicapForBoardSize(boardSize)
    require(handicap == 0 || handicap in 2..maxHandicap) {
        "Invalid handicap $handicap. The handicap must be 0 or between 2..$maxHandicap"
    }

    val handicapPoints = handicapPoints(handicap, boardSize)

    val rootNode = SgfNode(
        SgfProperty.Root.SZ(boardSize),
        SgfProperty.GameInfo.KM(komi),
        SgfProperty.Root.FF(4),
        SgfProperty.Root.GM(GameType.Go)
    )

    val node = handicapPoints.takeIf { it.isNotEmpty() }?.let {
        rootNode.addProperty(SgfProperty.GameInfo.HA(handicap)).addProperty(SgfProperty.Setup.AB(handicapPoints))
    } ?: rootNode

    return SgfGameTree(nelOf(node))
}

private fun maxHandicapForBoardSize(boardSize: Int) = when {
    boardSize < 7 -> 0
    boardSize == 7 -> 4
    boardSize % 2 == 0 -> 4
    else -> 9
}

private fun handicapPoints(handicap: Int, boardSize: Int): Set<SgfPoint> {
    val edgeDistance = edgeDistance(boardSize) ?: return emptySet()
    val middle = ceil(boardSize / 2.0).toInt()
    val tengen = SgfPoint(middle, middle)

    fun points(handicap: Int): Set<SgfPoint> = when (handicap) {
        2 -> setOf(
            SgfPoint(x = edgeDistance, y = boardSize - edgeDistance + 1),
            SgfPoint(x = boardSize - edgeDistance + 1, y = edgeDistance)
        )
        3 -> setOf(SgfPoint(x = boardSize - edgeDistance + 1, y = boardSize - edgeDistance + 1)) + points(2)
        4 -> setOf(SgfPoint(x = edgeDistance, y = edgeDistance)) + points(3)
        5 -> setOf(tengen) + points(4)
        6 -> setOf(
            SgfPoint(x = edgeDistance, y = middle),
            SgfPoint(x = boardSize - edgeDistance + 1, y = middle)
        ) + points(4)
        7 -> setOf(tengen) + points(6)
        8 -> setOf(
            SgfPoint(middle, edgeDistance),
            SgfPoint(middle, boardSize - edgeDistance + 1)
        ) + points(6)
        9 -> setOf(tengen) + points(8)
        else -> emptySet()
    }

    return points(handicap)
}

private fun edgeDistance(boardSize: Int) = when {
    boardSize < 7 -> null
    boardSize < 13 -> 3
    else -> 4
}
