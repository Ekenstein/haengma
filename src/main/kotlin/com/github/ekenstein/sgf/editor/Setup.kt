package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.toPropertySet
import com.github.ekenstein.sgf.utils.LinkedList
import com.github.ekenstein.sgf.utils.NonEmptySet
import com.github.ekenstein.sgf.utils.nonEmptySetOf
import com.github.ekenstein.sgf.utils.toNonEmptySetUnsafe

/**
 * Sets whose turn it is to play at the current position.
 */
fun SgfEditor.setNextToPlay(color: SgfColor) = addSetupProperty(SgfProperty.Setup.PL(color))

/**
 * Clears the given [points] from stones in the current position.
 */
fun SgfEditor.removeStones(point: SgfPoint, vararg points: SgfPoint) = removeStones(nonEmptySetOf(point, *points))

/**
 * Clears the given set of [points] from stones in the current position.
 */
fun SgfEditor.removeStones(points: NonEmptySet<SgfPoint>) = addSetupProperty(SgfProperty.Setup.AE(points))

/**
 * Adds the given stones to the position regardless of what was at the position before.
 */
fun SgfEditor.addStones(color: SgfColor, point: SgfPoint, vararg points: SgfPoint) =
    addStones(color, nonEmptySetOf(point, *points))

/**
 * Adds the given set of stones to the position regardless of what was at the position before.
 */
fun SgfEditor.addStones(color: SgfColor, points: NonEmptySet<SgfPoint>) = when (color) {
    SgfColor.Black -> addSetupProperty(SgfProperty.Setup.AB(points))
    SgfColor.White -> addSetupProperty(SgfProperty.Setup.AW(points))
}

private fun SgfEditor.addSetupProperty(property: SgfProperty.Setup): SgfEditor =
    if (!currentNode.hasMoveProperties()) {
        updateCurrentNode { node ->
            when (property) {
                is SgfProperty.Setup.AB -> {
                    val updatedProperty = node.property<SgfProperty.Setup.AB>()
                        ?.let { it.copy(points = it.points + property.points) }
                        ?: property

                    node.updateSetupProperties(
                        updatedProperty,
                        listOfNotNull(
                            node.property<SgfProperty.Setup.AE>(),
                            node.property<SgfProperty.Setup.AW>()
                        )
                    )
                }
                is SgfProperty.Setup.AE -> {
                    val updatedProperty = node.property<SgfProperty.Setup.AE>()
                        ?.let { it.copy(points = it.points + property.points) }
                        ?: property

                    node.updateSetupProperties(
                        updatedProperty,
                        listOfNotNull(
                            node.property<SgfProperty.Setup.AB>(),
                            node.property<SgfProperty.Setup.AW>()
                        )
                    )
                }
                is SgfProperty.Setup.AW -> {
                    val updatedProperty = node.property<SgfProperty.Setup.AW>()
                        ?.let { it.copy(points = it.points + property.points) }
                        ?: property

                    node.updateSetupProperties(
                        updatedProperty,
                        listOfNotNull(
                            node.property<SgfProperty.Setup.AE>(),
                            node.property<SgfProperty.Setup.AB>()
                        )
                    )
                }
                is SgfProperty.Setup.PL -> node.copy(properties = node.properties + property)
            }
        }
    } else {
        val node = SgfNode(property)
        when (currentSequence.right) {
            is LinkedList.Cons -> insertBranch(node)
            LinkedList.Nil -> if (currentTree.focus.trees.isEmpty()) {
                insertNodeToTheRight(node)
            } else {
                insertBranch(node)
            }
        }
    }

private fun SgfNode.updateSetupProperties(
    property: SgfProperty.Setup,
    opposing: List<SgfProperty.Setup>
): SgfNode {
    val pointsByProperty = opposing.associateWith { it.getPoints() - property.getPoints() }
    val removedProperties = pointsByProperty.filterValues { it.isEmpty() }.keys
    val updatedProperties = (pointsByProperty - removedProperties).map { (property, points) ->
        property.setPoints(points.toNonEmptySetUnsafe())
    }

    val newProperties = properties + property + updatedProperties - removedProperties

    return copy(
        properties = newProperties.toPropertySet()
    )
}

private fun SgfProperty.Setup.getPoints() = when (this) {
    is SgfProperty.Setup.AB -> points
    is SgfProperty.Setup.AE -> points
    is SgfProperty.Setup.AW -> points
    is SgfProperty.Setup.PL -> emptySet()
}

private fun SgfProperty.Setup.setPoints(points: NonEmptySet<SgfPoint>) = when (this) {
    is SgfProperty.Setup.AB -> copy(points = points)
    is SgfProperty.Setup.AE -> copy(points = points)
    is SgfProperty.Setup.AW -> copy(points = points)
    is SgfProperty.Setup.PL -> this
}
