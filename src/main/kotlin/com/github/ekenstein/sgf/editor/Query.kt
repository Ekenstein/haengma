package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.utils.orNull

/**
 * Checks if the current node is the root node.
 */
fun SgfEditor.isRootNode() = currentTree.top.isEmpty() && currentSequence.left.isEmpty()

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

private fun SgfEditor.startingColor(): SgfColor = if (getGameInfo().rules.handicap >= 2) {
    SgfColor.White
} else {
    SgfColor.Black
}
