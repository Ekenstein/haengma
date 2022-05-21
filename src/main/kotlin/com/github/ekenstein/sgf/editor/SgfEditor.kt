package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.asPointOrNull
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.extensions.property
import com.github.ekenstein.sgf.utils.LinkedList
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.NonEmptyList
import com.github.ekenstein.sgf.utils.TreeZipper
import com.github.ekenstein.sgf.utils.Unzip
import com.github.ekenstein.sgf.utils.Zipper
import com.github.ekenstein.sgf.utils.commit
import com.github.ekenstein.sgf.utils.commitAtCurrentPosition
import com.github.ekenstein.sgf.utils.flatMap
import com.github.ekenstein.sgf.utils.goRightUnsafe
import com.github.ekenstein.sgf.utils.goUp
import com.github.ekenstein.sgf.utils.insertDownLeft
import com.github.ekenstein.sgf.utils.insertRight
import com.github.ekenstein.sgf.utils.linkedListOf
import com.github.ekenstein.sgf.utils.nelOf
import com.github.ekenstein.sgf.utils.orNull
import com.github.ekenstein.sgf.utils.toLinkedList
import com.github.ekenstein.sgf.utils.toZipper
import com.github.ekenstein.sgf.utils.update

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
    constructor(block: GameInfo.() -> Unit) : this(GameInfo.default.apply(block))

    val currentNode: SgfNode = currentSequence.focus
}

fun SgfEditor.updateGameInfo(block: GameInfo.() -> Unit): SgfEditor {
    val gameInfo = getGameInfo()
    gameInfo.block()

    goToRootNode().updateCurrentNode {
        it.updateGameInfo(gameInfo)
    }
    return this
}

fun SgfEditor.getGameInfo(): GameInfo = goToRootNode().currentNode.getGameInfo()

/**
 * Saves the state of the editor to a [SgfGameTree]
 */
fun SgfEditor.commit() = currentTree.commit()

private fun SgfEditor.updateCurrentNode(block: (SgfNode) -> SgfNode): SgfEditor {
    val newSequence = currentSequence.update(block)
    return copy(
        currentSequence = newSequence,
        currentTree = currentTree.update {
            it.copy(sequence = newSequence.commit())
        }
    )
}

private inline fun <reified T : SgfProperty> SgfNode.hasProperty() = properties.filterIsInstance<T>().any()
private fun SgfNode.hasSetupProperties() = hasProperty<SgfProperty.Setup>()
private fun SgfNode.hasRootProperties() = hasProperty<SgfProperty.Root>()
private fun SgfNode.hasMoveProperties() = hasProperty<SgfProperty.Move>()
private fun SgfNode.hasGameInfoProperties() = hasProperty<SgfProperty.GameInfo>()

/**
 * Returns the full sequence from this node up to the root.
 */
fun SgfEditor.getFullSequence(): NonEmptyList<SgfNode> {
    fun TreeZipper<SgfGameTree>.nodes(): NonEmptyList<SgfNode> = when (val parent = goUp()) {
        is MoveResult.Failure -> focus.sequence
        is MoveResult.Success -> parent.value.nodes() + focus.sequence
    }

    return currentTree.update {
        it.copy(sequence = currentSequence.commitAtCurrentPosition())
    }.nodes()
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

private fun Board.isOccupied(stone: Stone): Boolean = stones.any { it.point == stone.point }

private fun SgfEditor.isBoardRepeating(currentBoard: Board, stone: Stone): Boolean {
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

fun SgfEditor.placeStone(color: SgfColor, x: Int, y: Int): SgfEditor {
    val currentBoard = extractBoard()
    checkMove(x in 1..currentBoard.boardSize.first && x in 1..currentBoard.boardSize.second) {
        "The stone is placed outside of the board"
    }

    checkMove(nextToPlay() == color) {
        "It's not ${color.asString}'s turn to play"
    }

    val stone = Stone(color, SgfPoint(x, y))
    checkMove(!currentBoard.isOccupied(stone)) {
        "The point is occupied"
    }

    checkMove(!isBoardRepeating(currentBoard, stone)) {
        "The position is repeating"
    }

    val updatedBoard = currentBoard.placeStone(stone)
    checkMove(updatedBoard.stones.contains(stone)) {
        "It is suicide to play at the point $x, $y"
    }

    return when (color) {
        SgfColor.Black -> insertMove(SgfProperty.Move.B(Move.Stone(stone.point)))
        SgfColor.White -> insertMove(SgfProperty.Move.W(Move.Stone(stone.point)))
    }
}

fun SgfEditor.pass(color: SgfColor): SgfEditor {
    checkMove(nextToPlay() == color) {
        "It's not ${color.asString}'s turn to play"
    }

    return when (color) {
        SgfColor.Black -> insertMove(SgfProperty.Move.B(Move.Pass))
        SgfColor.White -> insertMove(SgfProperty.Move.W(Move.Pass))
    }
}

private tailrec fun SgfEditor.goToTreeThatStartsWithProperty(property: SgfProperty): MoveResult<SgfEditor> =
    when (currentSequence.focus.move) {
        property -> MoveResult.Success(this, this)
        else -> when (val result = goToNextTree()) {
            is MoveResult.Failure -> result
            is MoveResult.Success -> result.value.goToTreeThatStartsWithProperty(property)
        }
    }

private fun SgfEditor.insertMove(property: SgfProperty.Move): SgfEditor {
    fun insertInNextNodeOrBranchOut() = when (val right = currentSequence.right) {
        is LinkedList.Cons -> when (right.head.move) {
            property -> copy(currentSequence = currentSequence.goRightUnsafe())
            else -> {
                // check if there are any children that starts with this move
                when (val childTree = goToLeftMostChildTree().flatMap { it.goToTreeThatStartsWithProperty(property) }) {
                    is MoveResult.Failure -> {
                        val node = SgfNode(property)
                        val mainTree = SgfGameTree(nelOf(node))
                        val restOfSequence = SgfGameTree(
                            sequence = nelOf(right.head, right.tail),
                            trees = currentTree.focus.trees
                        )

                        val newTree = currentTree.update {
                            it.copy(
                                sequence = currentSequence.commitAtCurrentPosition(),
                                trees = emptyList()
                            )
                        }

                        copy(
                            currentSequence = mainTree.sequence.toZipper(),
                            currentTree = newTree.insertDownLeft(linkedListOf(mainTree, restOfSequence))!!
                        )
                    }
                    is MoveResult.Success -> childTree.value
                }
            }
        }
        LinkedList.Nil -> {
            // check if there are any children that starts with this move
            when (val childTree = goToLeftMostChildTree().flatMap { it.goToTreeThatStartsWithProperty(property) }) {
                is MoveResult.Failure -> {
                    val node = SgfNode(property)
                    val sequence = currentSequence.insertRight(node).goRightUnsafe()
                    copy(
                        currentSequence = sequence,
                        currentTree = currentTree.update {
                            it.copy(sequence = sequence.commit())
                        }
                    )
                }
                is MoveResult.Success -> childTree.value
            }
        }
    }

    return if (currentNode.hasRootProperties() || currentNode.hasSetupProperties()) {
        insertInNextNodeOrBranchOut()
    } else {
        when (currentSequence.focus.move) {
            property -> this // no-op
            null -> updateCurrentNode { it.addProperty(property) }
            else -> insertInNextNodeOrBranchOut()
        }
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
