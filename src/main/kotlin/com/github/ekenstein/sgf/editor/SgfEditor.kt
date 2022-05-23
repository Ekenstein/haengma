package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.asPointOrNull
import com.github.ekenstein.sgf.utils.LinkedList
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.NonEmptyList
import com.github.ekenstein.sgf.utils.TreeZipper
import com.github.ekenstein.sgf.utils.Unzip
import com.github.ekenstein.sgf.utils.Zipper
import com.github.ekenstein.sgf.utils.commit
import com.github.ekenstein.sgf.utils.commitAtCurrentPosition
import com.github.ekenstein.sgf.utils.flatMap
import com.github.ekenstein.sgf.utils.get
import com.github.ekenstein.sgf.utils.goRight
import com.github.ekenstein.sgf.utils.goRightUnsafe
import com.github.ekenstein.sgf.utils.goUp
import com.github.ekenstein.sgf.utils.insertDownLeft
import com.github.ekenstein.sgf.utils.insertRight
import com.github.ekenstein.sgf.utils.linkedListOfNotNull
import com.github.ekenstein.sgf.utils.nelOf
import com.github.ekenstein.sgf.utils.orElse
import com.github.ekenstein.sgf.utils.orError
import com.github.ekenstein.sgf.utils.orNull
import com.github.ekenstein.sgf.utils.toLinkedList
import com.github.ekenstein.sgf.utils.toNel
import com.github.ekenstein.sgf.utils.toZipper
import com.github.ekenstein.sgf.utils.update
import com.github.ekenstein.sgf.utils.withOrigin

private object GameTreeUnzip : Unzip<SgfGameTree> {
    override fun unzip(node: SgfGameTree): LinkedList<SgfGameTree> = node.trees.toLinkedList()

    override fun zip(node: SgfGameTree, children: LinkedList<SgfGameTree>): SgfGameTree = node.copy(
        trees = children
    )
}

data class SgfEditor(
    val currentSequence: Zipper<SgfNode>,
    val currentTree: TreeZipper<SgfGameTree>
) {
    constructor(gameTree: SgfGameTree) : this(
        gameTree.sequence.toZipper(),
        TreeZipper.ofNode(gameTree, GameTreeUnzip)
    )
    constructor(gameInfo: GameInfo) : this(gameInfo.toGameTree())
    constructor(block: GameInfo.() -> Unit = { }) : this(GameInfo.default.apply(block))

    val currentNode: SgfNode = currentSequence.focus
}

fun SgfEditor.getGameInfo(): GameInfo = goToRootNode().currentNode.getGameInfo()

private fun SgfEditor.insertBranch(node: SgfNode): SgfEditor {
    val mainVariation = SgfGameTree(nelOf(node))
    val restOfSequence = currentSequence.right.toNel()?.let {
        SgfGameTree(
            sequence = it,
            trees = currentTree.focus.trees
        )
    }
    val newTree = currentTree.update {
        it.copy(
            sequence = currentSequence.commitAtCurrentPosition(),
            trees = if (restOfSequence == null) {
                it.trees
            } else {
                emptyList()
            }
        )
    }

    return copy(
        currentSequence = mainVariation.sequence.toZipper(),
        currentTree = newTree.insertDownLeft(linkedListOfNotNull(mainVariation, restOfSequence))
    )
}

private fun SgfEditor.insertNodeToTheRight(node: SgfNode): SgfEditor {
    val sequence = currentSequence.insertRight(node).goRight().orError {
        "Expected there to be a node to the right"
    }

    return copy(
        currentSequence = sequence,
        currentTree = currentTree.update {
            it.copy(sequence = sequence.commit())
        }
    )
}

fun SgfEditor.updateCurrentNode(block: (SgfNode) -> SgfNode): SgfEditor {
    val newSequence = currentSequence.update(block)
    return copy(
        currentSequence = newSequence,
        currentTree = currentTree.update {
            it.copy(sequence = newSequence.commit())
        }
    )
}

/**
 * Saves the state of the editor to a [SgfGameTree]
 */
fun SgfEditor.commit() = currentTree.commit()

private fun SgfNode.hasSetupProperties() = hasProperty<SgfProperty.Setup>()
private fun SgfNode.hasRootProperties() = hasProperty<SgfProperty.Root>()
private fun SgfNode.hasMoveProperties() = hasProperty<SgfProperty.Move>()

private fun SgfEditor.getFullSequence(): NonEmptyList<SgfNode> {
    tailrec fun TreeZipper<SgfGameTree>.nodes(result: NonEmptyList<SgfNode>): NonEmptyList<SgfNode> =
        when (val parent = goUp()) {
            is MoveResult.Failure -> result
            is MoveResult.Success -> parent.value.nodes(focus.sequence + result)
        }

    return currentTree.nodes(currentSequence.commitAtCurrentPosition())
}

private fun Board.applyNodePropertiesToBoard(node: SgfNode): Board = node.properties.fold(this) { board, property ->
    val newBoard = when (property) {
        is SgfProperty.Move.B -> property.move.asPointOrNull?.let {
            board.placeStone(Stone(SgfColor.Black, it))
        }
        is SgfProperty.Move.W -> property.move.asPointOrNull?.let {
            board.placeStone(Stone(SgfColor.White, it))
        }
        is SgfProperty.Setup.AB -> board.copy(
            stones = board.stones + property.points.map { Stone(SgfColor.Black, it) }
        )
        is SgfProperty.Setup.AW -> board.copy(
            stones = board.stones + property.points.map { Stone(SgfColor.White, it) }
        )
        is SgfProperty.Setup.AE -> board.copy(
            stones = board.stones.filter { !property.points.contains(it.point) }
        )
        else -> board
    }

    newBoard ?: board
}

/**
 * Extracts the current board position.
 */
fun SgfEditor.extractBoard(): Board {
    val sequence = getFullSequence()
    val boardSize = when (val size = goToRootNode().currentSequence.focus.property<SgfProperty.Root.SZ>()) {
        null -> 19 to 19
        else -> size.width to size.height
    }

    return sequence.fold(Board.empty(boardSize)) { board, node ->
        board.applyNodePropertiesToBoard(node)
    }
}

private fun SgfEditor.isPositionRepeating(currentBoard: Board, stone: Stone): Boolean {
    val previousPosition = goToPreviousNode().orNull()?.extractBoard()
    val nextPosition = currentBoard.placeStone(stone)

    return previousPosition?.stones?.toSet() == nextPosition.stones.toSet()
}

private fun SgfEditor.startingColor(): SgfColor = if (getGameInfo().rules.handicap >= 2) {
    SgfColor.White
} else {
    SgfColor.Black
}

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
 * Clears the given [points] from stones in the current position.
 */
fun SgfEditor.removeStones(vararg points: SgfPoint) = addSetupProperty(SgfProperty.Setup.AE(points.toSet()))

/**
 * Adds the given stones to the position regardless of what was at the position before.
 */
fun SgfEditor.addStones(color: SgfColor, vararg point: SgfPoint) = when (color) {
    SgfColor.Black -> addSetupProperty(SgfProperty.Setup.AB(point.toSet()))
    SgfColor.White -> addSetupProperty(SgfProperty.Setup.AW(point.toSet()))
}

/**
 * Sets whose turn it is to play at the current position.
 */
fun SgfEditor.setNextToPlay(color: SgfColor) = addSetupProperty(SgfProperty.Setup.PL(color))

/**
 * Places a stone at the given point at the current position.
 *
 * Will throw [SgfException.IllegalMove] if:
 *  - It's not the [color]'s turn to play.
 *  - The stone is placed outside the board.
 *  - The point is occupied by another stone.
 *  - The placed stone results in repetition of the position (ko).
 *  - The stone immediately dies when placed on the board (suicide).
 *
 *  If you wish to place a stone without checking rules, look at [addStones].
 */
fun SgfEditor.placeStone(color: SgfColor, x: Int, y: Int): SgfEditor {
    val currentBoard = extractBoard()
    checkMove(x in 1..currentBoard.boardSize.first && x in 1..currentBoard.boardSize.second) {
        "The stone is placed outside of the board"
    }

    checkMove(nextToPlay() == color) {
        "It's not ${color.asString}'s turn to play"
    }

    checkMove(!currentBoard.isOccupied(x, y)) {
        "The point $x, $y is occupied"
    }

    val stone = Stone(color, SgfPoint(x, y))
    checkMove(!isPositionRepeating(currentBoard, stone)) {
        "The position is repeating"
    }

    checkMove(!currentBoard.isSuicide(stone)) {
        "It is suicide to play at the point $x, $y"
    }

    val property = when (color) {
        SgfColor.Black -> SgfProperty.Move.B(Move.Stone(stone.point))
        SgfColor.White -> SgfProperty.Move.W(Move.Stone(stone.point))
    }

    return addMoveProperty(property).get()
}

/**
 * The player of [color] passes at the current position.
 *
 * Will throw [SgfException.IllegalMove] if it's not [color]'s turn to play.
 */
fun SgfEditor.pass(color: SgfColor): SgfEditor {
    checkMove(nextToPlay() == color) {
        "It's not ${color.asString}'s turn to play"
    }

    val property = when (color) {
        SgfColor.Black -> SgfProperty.Move.B(Move.Pass)
        SgfColor.White -> SgfProperty.Move.W(Move.Pass)
    }

    return addMoveProperty(property).get()
}

private val SgfNode.move: SgfProperty?
    get() = properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B,
            is SgfProperty.Move.W -> it
            else -> null
        }
    }.singleOrNull()

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

private fun SgfEditor.addSetupProperty(property: SgfProperty.Setup): SgfEditor {
    return if (!currentNode.hasMoveProperties()) {
        updateCurrentNode { node ->
            val updatedProperty = when (property) {
                is SgfProperty.Setup.AB -> property.copy(
                    points = property.points + node.property<SgfProperty.Setup.AB>()?.points.orEmpty()
                )
                is SgfProperty.Setup.AE -> property.copy(
                    points = property.points + node.property<SgfProperty.Setup.AE>()?.points.orEmpty()
                )
                is SgfProperty.Setup.AW -> property.copy(
                    points = property.points + node.property<SgfProperty.Setup.AW>()?.points.orEmpty()
                )
                is SgfProperty.Setup.PL -> property
            }

            node.copy(
                properties = node.properties + updatedProperty
            )
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

private fun SgfEditor.addMoveProperty(property: SgfProperty.Move): MoveResult<SgfEditor> {
    val result = if (currentNode.hasRootProperties() || currentNode.hasSetupProperties()) {
        insertInNextNodeOrBranchOut(property)
    } else {
        when (currentSequence.focus.move) {
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
