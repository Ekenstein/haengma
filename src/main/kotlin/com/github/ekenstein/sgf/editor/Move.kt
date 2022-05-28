package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.utils.LinkedList
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.flatMap
import com.github.ekenstein.sgf.utils.get
import com.github.ekenstein.sgf.utils.goRightUnsafe
import com.github.ekenstein.sgf.utils.orElse
import com.github.ekenstein.sgf.utils.orNull
import com.github.ekenstein.sgf.utils.withOrigin

/**
 * The player of [color] passes at the current position.
 *
 * Will throw [SgfException.IllegalMove] iff [force] is false, and it's not [color]'s turn to play.
 *
 * @param color The color of the player who passes
 * @param force Whether the execution of the move should be forced or not. If true, no validation will occur,
 *              otherwise the move must valid for the current position.
 * @throws [SgfException.IllegalMove] if the move was invalid and the [force] flag was false.
 */
fun SgfEditor.pass(color: SgfColor, force: Boolean = false): SgfEditor {
    val property = when (color) {
        SgfColor.Black -> SgfProperty.Move.B(Move.Pass)
        SgfColor.White -> SgfProperty.Move.W(Move.Pass)
    }

    val result = if (force) {
        addMoveProperty(property).flatMap {
            it.updateCurrentNode { node ->
                node.copy(properties = node.properties + SgfProperty.Move.KO)
            }.stay()
        }
    } else {
        checkMove(nextToPlay() == color) {
            "It's not ${color.asString}'s turn to play"
        }

        addMoveProperty(property)
    }

    return result.get()
}

/**
 * Places a stone at the given point at the current position.
 *
 * Will throw [SgfException.IllegalMove] iff:
 *  - The stone is placed outside the board or ...
 *  - [force] is false and ...
 *  - It's not the [color]'s turn to play.
 *  - The point is occupied by another stone.
 *  - The placed stone results in repetition of the position (ko).
 *  - The stone immediately dies when placed on the board (suicide).
 *  @param color The color of the stone to place
 *  @param x The x-coordinate for the stone
 *  @param y The y-coordinate for the stone.
 *  @param force Whether the execution of the move should be forced or not. If true, no validation will occur,
 *               otherwise the move must be a valid move at the current position.
 *  @throws [SgfException.IllegalMove] If the move would result in placing the stone outside the board, or if the move
 *  was invalid at the current position and the [force] flag was false.
 */
fun SgfEditor.placeStone(color: SgfColor, x: Int, y: Int, force: Boolean = false) = placeStone(
    color = color,
    point = SgfPoint(x, y),
    force = force
)

/**
 * Places a stone at the given point at the current position.
 *
 * Will throw [SgfException.IllegalMove] iff:
 *  - The stone is placed outside the board or ...
 *  - [force] is false and ...
 *  - It's not the [color]'s turn to play.
 *  - The point is occupied by another stone.
 *  - The placed stone results in repetition of the position (ko).
 *  - The stone immediately dies when placed on the board (suicide).
 *  @param color The color of the stone to place
 *  @param point The point to place the stone
 *  @param force Whether the execution of the move should be forced or not. If true, no validation will occur,
 *               otherwise the move must be a valid move at the current position.
 *  @throws [SgfException.IllegalMove] If the move would result in placing the stone outside the board, or if the move
 *  was invalid at the current position and the [force] flag was false.
 */
fun SgfEditor.placeStone(color: SgfColor, point: SgfPoint, force: Boolean = false): SgfEditor {
    val currentBoard = extractBoard()
    checkMove(point.x in 1..currentBoard.width && point.y in 1..currentBoard.height) {
        "The stone is placed outside of the board"
    }
    val property = when (color) {
        SgfColor.Black -> SgfProperty.Move.B(Move.Stone(point))
        SgfColor.White -> SgfProperty.Move.W(Move.Stone(point))
    }

    val result = if (force) {
        addMoveProperty(property).flatMap {
            it.updateCurrentNode { node ->
                node.copy(properties = node.properties + SgfProperty.Move.KO)
            }.stay()
        }
    } else {
        checkIfMoveIsValid(color, point, currentBoard)
        addMoveProperty(property)
    }

    return result.get()
}

private fun SgfEditor.checkIfMoveIsValid(color: SgfColor, point: SgfPoint, currentBoard: Board) {
    checkMove(nextToPlay() == color) {
        "It's not ${color.asString}'s turn to play"
    }

    checkMove(!currentBoard.isOccupied(point)) {
        "The point ${point.x}, ${point.y} is occupied"
    }

    val previousBoard = goToPreviousNode().orNull()?.extractBoard()
    val nextBoard = currentBoard.placeStone(color, point)

    checkMove(nextBoard.stones != previousBoard?.stones) {
        "The position is repeating"
    }

    checkMove(nextBoard.stones.containsKey(point)) {
        "It is suicide to play at the point ${point.x}, ${point.y}"
    }
}

private fun SgfEditor.addMoveProperty(property: SgfProperty.Move): MoveResult<SgfEditor> {
    val result = if (currentNode.hasRootProperties() || currentNode.hasSetupProperties()) {
        insertInNextNodeOrBranchOut(property)
    } else {
        when (currentNode.move) {
            property -> stay() // no-op
            null -> updateCurrentNode {
                it.copy(
                    properties = it.properties + property
                )
            }.stay()
            else -> insertInNextNodeOrBranchOut(property)
        }
    }

    return result.withOrigin(this)
}

private fun SgfEditor.goToTreeThatStartsWithProperty(property: SgfProperty): MoveResult<SgfEditor> {
    fun sequenceStartsWith(editor: SgfEditor) = editor.currentSequence.focus.move == property
    return tryRepeatWhileNot(::sequenceStartsWith) {
        it.goToNextTree()
    }
}

private fun SgfEditor.insertInNextNodeOrBranchOut(
    property: SgfProperty.Move
) = when (val right = currentSequence.right) {
    is LinkedList.Cons -> when (right.head.move) {
        property -> copy(currentSequence = currentSequence.goRightUnsafe()).stay()
        else -> goToLeftMostChildTree()
            .flatMap { it.goToTreeThatStartsWithProperty(property) }
            .withOrigin(this)
            .orElse {
                val node = SgfNode(property)
                it.insertBranch(node).stay()
            }
    }
    LinkedList.Nil -> goToLeftMostChildTree()
        .flatMap { it.goToTreeThatStartsWithProperty(property) }
        .withOrigin(this)
        .orElse {
            val node = SgfNode(property)
            if (it.currentTree.focus.trees.isEmpty()) {
                it.insertNodeToTheRight(node).stay()
            } else {
                it.insertBranch(node).stay()
            }
        }
}

private val SgfColor.asString
    get() = when (this) {
        SgfColor.Black -> "black"
        SgfColor.White -> "white"
    }

private fun checkMove(value: Boolean, reason: () -> String) {
    if (!value) {
        throw SgfException.IllegalMove(reason())
    }
}

private val SgfNode.move: SgfProperty?
    get() = properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B,
            is SgfProperty.Move.W -> it
            else -> null
        }
    }.singleOrNull()
