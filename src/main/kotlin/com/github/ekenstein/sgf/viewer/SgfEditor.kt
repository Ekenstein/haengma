package com.github.ekenstein.sgf.viewer

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.asPointOrNull
import com.github.ekenstein.sgf.extensions.property
import com.github.ekenstein.sgf.flip
import com.github.ekenstein.sgf.utils.LinkedList
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.NonEmptyList
import com.github.ekenstein.sgf.utils.TreeZipper
import com.github.ekenstein.sgf.utils.Unzip
import com.github.ekenstein.sgf.utils.Zipper
import com.github.ekenstein.sgf.utils.commit
import com.github.ekenstein.sgf.utils.commitAtCurrentPosition
import com.github.ekenstein.sgf.utils.emptyLinkedList
import com.github.ekenstein.sgf.utils.goDownLeft
import com.github.ekenstein.sgf.utils.goLeft
import com.github.ekenstein.sgf.utils.goRight
import com.github.ekenstein.sgf.utils.goRightUnsafe
import com.github.ekenstein.sgf.utils.goRightUntil
import com.github.ekenstein.sgf.utils.goToLast
import com.github.ekenstein.sgf.utils.goUp
import com.github.ekenstein.sgf.utils.insertDownLeft
import com.github.ekenstein.sgf.utils.insertRight
import com.github.ekenstein.sgf.utils.linkedListOf
import com.github.ekenstein.sgf.utils.nelOf
import com.github.ekenstein.sgf.utils.toLinkedList
import com.github.ekenstein.sgf.utils.toZipper
import com.github.ekenstein.sgf.utils.update

private object GameTreeUnzip : Unzip<SgfGameTree> {
    override fun unzip(node: SgfGameTree): LinkedList<SgfGameTree> = node.trees.toLinkedList()

    override fun zip(node: SgfGameTree, children: LinkedList<SgfGameTree>): SgfGameTree = node.copy(
        trees = children
    )
}

data class SgfEditor(val currentNode: Zipper<SgfNode>, val currentTree: TreeZipper<SgfGameTree>) {
    constructor(gameTree: SgfGameTree) : this(gameTree.sequence.toZipper(), gameTree.treeZipper)
    val rootNode: SgfNode by lazy {
        goToRootNode().currentNode.focus
    }

    val boardSize: Pair<Int, Int> by lazy {
        when (val size = rootNode.property<SgfProperty.Root.SZ>()) {
            null -> 19 to 19
            else -> size.width to size.height
        }
    }
    val handicap: Int by lazy { rootNode.property<SgfProperty.GameInfo.HA>()?.numberOfStones ?: 0 }
    val startingColor by lazy { if (handicap >= 2) SgfColor.White else SgfColor.Black }

    val fullSequence by lazy {
        fun TreeZipper<SgfGameTree>.nodes(): NonEmptyList<SgfNode> = when (val parent = goUp()) {
            null -> focus.sequence
            else -> parent.nodes() + focus.sequence
        }

        currentTree.update { it.copy(sequence = currentNode.commitAtCurrentPosition()) }.nodes()
    }

    val board: Board by lazy {
        fullSequence.fold(Board.empty(boardSize)) { board, node ->
            node.properties.fold(board) { b, property ->
                val newBoard = when (property) {
                    is SgfProperty.Move.B -> property.move.asPointOrNull?.let {
                        b.placeStone(Stone(SgfColor.Black, it))
                    }
                    is SgfProperty.Move.W -> property.move.asPointOrNull?.let {
                        b.placeStone(Stone(SgfColor.White, it))
                    }
                    is SgfProperty.Setup.AB -> b.copy(
                        stones = b.stones + property.points.map { Stone(SgfColor.Black, it) }
                    )
                    is SgfProperty.Setup.AW -> b.copy(
                        stones = b.stones + property.points.map { Stone(SgfColor.White, it) }
                    )
                    is SgfProperty.Setup.AE -> b.copy(
                        stones = b.stones.filter { !property.points.contains(it.point) }
                    )
                    else -> b
                }

                newBoard ?: b
            }
        }
    }

    val nextToPlay: SgfColor by lazy {
        val nextColor = currentNode.commitAtCurrentPosition().map {
            it.move?.first?.flip() ?: it.property<SgfProperty.Setup.PL>()?.color
        }.lastOrNull()

        nextColor ?: startingColor
    }
}

/**
 * Saves the state of the editor to a [SgfGameTree]
 */
fun SgfEditor.commit() = currentTree.commit()

/**
 * Goes to the next node. If there are no more nodes in the current sequence,
 * the next node will be the first node in the left-most child tree.
 * If we are at the end of the tree, the current position will be returned.
 */
fun SgfEditor.goToNextNodeOrStay(): SgfEditor = goToNextNode() ?: this

/**
 * Goes to the next node. If there are no more nodes in the current sequence,
 * the next node will be the first node in the left-most child tree.
 * Returns null if we are at the end of the tree.
 */
fun SgfEditor.goToNextNode(): SgfEditor? = when (val result = currentNode.goRight()) {
    is MoveResult.Failure -> currentTree.goDownLeft()?.let {
        copy(currentNode = it.focus.sequence.toZipper(), currentTree = it)
    }
    is MoveResult.Success -> copy(currentNode = result.zipper)
}

/**
 * Goes to the last node of the tree. This includes traversing all the left-most child trees.
 */
tailrec fun SgfEditor.goToLastNode(): SgfEditor = when (val next = goToNextNode()) {
    null -> this
    else -> next.goToLastNode()
}

fun SgfEditor.goToPreviousNodeOrStay(): SgfEditor = goToPreviousNode() ?: this

fun SgfEditor.goToPreviousNode(): SgfEditor? = when (val result = currentNode.goLeft()) {
    is MoveResult.Failure -> currentTree.goUp()?.let {
        copy(
            currentNode = it.focus.sequence.toZipper().goToLast(),
            currentTree = it
        )
    }
    is MoveResult.Success -> copy(currentNode = result.zipper)
}

tailrec fun SgfEditor.goToRootNode(): SgfEditor = when (val previous = goToPreviousNode()) {
    null -> this
    else -> previous.goToRootNode()
}

private fun SgfEditor.isOccupied(stone: Stone): Boolean = board.stones.any { it.point == stone.point }

private fun SgfEditor.isBoardRepeating(stone: Stone): Boolean {
    val previousPosition = goToPreviousNode()?.board
    val nextPosition = board.placeStone(stone)

    return previousPosition?.stones?.toSet() == nextPosition.stones.toSet()
}

fun SgfEditor.placeStone(color: SgfColor, x: Int, y: Int): SgfEditor {
    checkMove(x in 1..boardSize.first && x in 1..boardSize.second) {
        "The stone is placed outside of the board"
    }

    checkMove(nextToPlay == color) {
        "It's not ${color.asString}'s turn to play"
    }

    val stone = Stone(color, SgfPoint(x, y))
    checkMove(!isOccupied(stone)) {
        "The point is occupied"
    }

    checkMove(!isBoardRepeating(stone)) {
        "The position is repeating"
    }

    val updatedBoard = board.placeStone(stone)
    checkMove(updatedBoard.stones.contains(stone)) {
        "It is suicide to play at the point $x, $y"
    }

    return insertMove(color, Move.Stone(stone.point))
}

fun SgfEditor.pass(color: SgfColor): SgfEditor {
    require(nextToPlay == color) {
        "It's not ${color.asString}'s turn to play"
    }

    return insertMove(color, Move.Pass)
}

private fun SgfEditor.insertMove(color: SgfColor, move: Move) = when (val right = currentNode.right) {
    is LinkedList.Cons -> {
        when (right.head.move) {
            color to move -> copy(currentNode = currentNode.goRightUnsafe())
            else -> {
                val node = SgfNode(color.toSgfMoveProperty(move))
                val mainTree = SgfGameTree(nelOf(node))
                val restOfSequence = SgfGameTree(
                    sequence = nelOf(right.head, right.tail),
                    trees = currentTree.focus.trees
                )

                val newTree = currentTree.update {
                    it.copy(
                        sequence = currentNode.commitAtCurrentPosition(),
                        trees = emptyList()
                    )
                }

                copy(
                    currentNode = mainTree.sequence.toZipper(),
                    currentTree = newTree.insertDownLeft(linkedListOf(mainTree, restOfSequence))!!
                )
            }
        }
    }
    LinkedList.Nil -> {
        val child = currentTree.goDownLeft()?.goRightUntil {
            it.sequence.head.move == color to move
        }

        if (child != null) {
            copy(currentNode = child.focus.sequence.toZipper(), currentTree = child)
        } else {
            val node = SgfNode(color.toSgfMoveProperty(move))
            val sequence = currentNode.insertRight(node).goRightUnsafe()
            copy(
                currentNode = sequence,
                currentTree = currentTree.update {
                    it.copy(sequence = sequence.commit())
                }
            )
        }
    }
}

private val SgfGameTree.treeZipper
    get() = TreeZipper(
        left = emptyLinkedList(),
        focus = this,
        right = emptyLinkedList(),
        top = null,
        unzip = GameTreeUnzip
    )

private val SgfNode.move: Pair<SgfColor, Move>?
    get() = properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B -> SgfColor.Black to it.move
            is SgfProperty.Move.W -> SgfColor.White to it.move
            else -> null
        }
    }.singleOrNull()

private fun SgfColor.toSgfMoveProperty(move: Move) = when (this) {
    SgfColor.Black -> SgfProperty.Move.B(move)
    SgfColor.White -> SgfProperty.Move.W(move)
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
