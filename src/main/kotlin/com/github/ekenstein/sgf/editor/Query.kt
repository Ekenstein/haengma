package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.GameInfo
import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.getGameInfo
import com.github.ekenstein.sgf.utils.orNull

/**
 * Checks if the current node is the root node.
 */
fun SgfEditor.isRootNode() = currentTree.top.isEmpty() && currentSequence.left.isEmpty()

/**
 * Returns the game information of the tree. If game information is missing, the default values will be used.
 */
fun SgfEditor.getGameInfo(): GameInfo = goToRootNode().currentNode.getGameInfo()

/**
 * Returns the comments on the current node or null if there are no comments on the current node.
 */
fun SgfEditor.getComment(): String? = currentNode.property<SgfProperty.NodeAnnotation.C>()?.comment

/**
 * Returns whose turn it is to play at the current position.
 */
fun SgfEditor.nextToPlay(): SgfColor {
    fun SgfNode.nextToPlay() = properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B -> SgfColor.White
            is SgfProperty.Move.W -> SgfColor.Black
            is SgfProperty.Setup.PL -> it.color
            else -> null
        }
    }.singleOrNull()

    tailrec fun SgfEditor.nextToPlay(): SgfColor? = when (val color = currentSequence.focus.nextToPlay()) {
        null -> goToPreviousNode().orNull()?.nextToPlay()
        else -> color
    }

    return nextToPlay() ?: startingColor()
}

/**
 * Returns a flag indicating whether the current node represents a "hotspot", i.e. something interesting
 * (e.g. node contains a game-deciding move).
 *
 * True if it is a "hotspot", otherwise false.
 */
fun SgfEditor.isHotspot(): Boolean = currentNode.hasProperty<SgfProperty.NodeAnnotation.HO>()

/**
 * Returns the name of the node, if the node has a name, otherwise null.
 */
fun SgfEditor.getName(): String? = currentNode.property<SgfProperty.NodeAnnotation.N>()?.name

private fun SgfEditor.startingColor(): SgfColor = if (getGameInfo().rules.handicap >= 2) {
    SgfColor.White
} else {
    SgfColor.Black
}

/**
 * Returns the move of the current node, if there is a move on the current node, otherwise null will be returned.
 */
fun SgfEditor.getCurrentMove(): Pair<SgfColor, Move>? = currentNode.properties.mapNotNull {
    when (it) {
        is SgfProperty.Move.B -> SgfColor.Black to it.move
        is SgfProperty.Move.W -> SgfColor.White to it.move
        else -> null
    }
}.singleOrNull()
