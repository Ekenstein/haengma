package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.GameInfo
import com.github.ekenstein.sgf.GameInfoBuilder
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.asPointOrNull
import com.github.ekenstein.sgf.gameInfo
import com.github.ekenstein.sgf.getGameInfo
import com.github.ekenstein.sgf.toSgfProperties
import com.github.ekenstein.sgf.utils.LinkedList
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.NonEmptyList
import com.github.ekenstein.sgf.utils.TreeZipper
import com.github.ekenstein.sgf.utils.Unzip
import com.github.ekenstein.sgf.utils.Zipper
import com.github.ekenstein.sgf.utils.commit
import com.github.ekenstein.sgf.utils.commitAtCurrentPosition
import com.github.ekenstein.sgf.utils.get
import com.github.ekenstein.sgf.utils.goRightUnsafe
import com.github.ekenstein.sgf.utils.goUp
import com.github.ekenstein.sgf.utils.indexOfCurrent
import com.github.ekenstein.sgf.utils.insertDownLeft
import com.github.ekenstein.sgf.utils.insertRight
import com.github.ekenstein.sgf.utils.linkedListOfNotNull
import com.github.ekenstein.sgf.utils.map
import com.github.ekenstein.sgf.utils.nelOf
import com.github.ekenstein.sgf.utils.onSuccess
import com.github.ekenstein.sgf.utils.orElse
import com.github.ekenstein.sgf.utils.toLinkedList
import com.github.ekenstein.sgf.utils.toNel
import com.github.ekenstein.sgf.utils.toZipper
import com.github.ekenstein.sgf.utils.update
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private object GameTreeUnzip : Unzip<SgfGameTree> {
    override fun unzip(node: SgfGameTree): LinkedList<SgfGameTree> = node.trees.toLinkedList()

    override fun zip(node: SgfGameTree, children: LinkedList<SgfGameTree>): SgfGameTree = node.copy(
        trees = children
    )
}

/**
 * An editor for a [SgfGameTree]. Enables navigation and alteration of a tree.
 *
 * You can navigate through sequences with:
 * - [SgfEditor.goToNextNode]
 * - [SgfEditor.goToPreviousNode]
 *
 * Traverse the trees with:
 * - [SgfEditor.goToParentTree]
 * - [SgfEditor.goToNextTree]
 * - [SgfEditor.goToPreviousTree]
 * - [SgfEditor.goToLeftMostChildTree]
 *
 * You can quick-move through the tree with:
 * - [SgfEditor.goToRootNode]
 * - [SgfEditor.goToLastNode]
 * - [SgfEditor.tryRepeat]
 * - [SgfEditor.tryRepeatWhile]
 * - [SgfEditor.tryRepeatWhileNot]
 *
 * You can alter the tree by using:
 * - [SgfEditor.placeStone]
 * - [SgfEditor.pass],
 * - [SgfEditor.addStones]
 * - [SgfEditor.setNextToPlay]
 * - [SgfEditor.updateCurrentNode]
 * - [SgfEditor.updateGameInfo]
 *
 * You can query the tree by using:
 * - [SgfEditor.isRootNode]
 * - [SgfEditor.nextToPlay]
 * - [SgfEditor.getComment]
 * - [SgfEditor.getGameInfo]
 *
 * You can save your changes by using:
 * - [SgfEditor.commit]
 */
data class SgfEditor(
    val currentSequence: Zipper<SgfNode>,
    val currentTree: TreeZipper<SgfGameTree>
) {
    constructor(gameTree: SgfGameTree) : this(
        gameTree.sequence.toZipper(),
        TreeZipper.ofNode(gameTree, GameTreeUnzip)
    )
    constructor(gameInfo: GameInfo) : this(SgfGameTree(nelOf(SgfNode(gameInfo.toSgfProperties()))))
    constructor(block: GameInfoBuilder.() -> Unit = { }) : this(gameInfo(block))

    val currentNode: SgfNode = currentSequence.focus
}

/**
 * Will update the game info of the tree regardless of the position the editor is currently at.
 * Will always return an updated editor located at the same position as the given editor.
 */
@OptIn(ExperimentalContracts::class)
fun SgfEditor.updateGameInfo(block: GameInfoBuilder.() -> Unit): SgfEditor {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val backtracking = mutableListOf<(SgfEditor) -> SgfEditor>()

    fun SgfEditor.goToPreviousNodeWithBackTracking() = goToPreviousNodeInSequence()
        .onSuccess { _ ->
            backtracking.add {
                it.goToNextNodeInSequence().get()
            }
        }.orElse { _ ->
            goToParentTree().onSuccess { _ ->
                val index = currentTree.indexOfCurrent()
                backtracking.add { parent ->
                    parent.goToLeftMostChildTree().map { child ->
                        child.repeat(index) { it.goToNextTree() }
                    }.get()
                }
            }
        }

    tailrec fun SgfEditor.goToRootNodeWithBackTracking(): SgfEditor = when (val previous = goToPreviousNodeWithBackTracking()) {
        is MoveResult.Failure -> this
        is MoveResult.Success -> previous.position.goToRootNodeWithBackTracking()
    }

    val root = goToRootNodeWithBackTracking()
    val gameInfo = gameInfo(root.currentNode.getGameInfo(), block)
    val properties = gameInfo.toSgfProperties()

    val newRoot = root.updateCurrentNode { rootNode ->
        rootNode.copy(properties = rootNode.properties + properties)
    }

    return backtracking.reversed().fold(newRoot) { r, b -> b(r) }
}

/**
 * Executes the [block] and updates the current node to the resulting node.
 */
@OptIn(ExperimentalContracts::class)
fun SgfEditor.updateCurrentNode(block: (SgfNode) -> SgfNode): SgfEditor {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

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

/**
 * Extracts the current board position.
 */
fun SgfEditor.extractBoard(): Board {
    val sequence = getFullSequence()
    val boardSize = when (val size = goToRootNode().currentSequence.focus.property<SgfProperty.Root.SZ>()) {
        null -> 19 to 19
        else -> size.width to size.height
    }

    return sequence.fold(Board.empty(boardSize), ::applyNodePropertiesToBoard)
}

private fun SgfEditor.getFullSequence(): NonEmptyList<SgfNode> {
    tailrec fun TreeZipper<SgfGameTree>.nodes(result: NonEmptyList<SgfNode>): NonEmptyList<SgfNode> =
        when (val parent = goUp()) {
            is MoveResult.Failure -> result
            is MoveResult.Success -> parent.position.nodes(parent.position.focus.sequence + result)
        }

    return currentTree.nodes(currentSequence.commitAtCurrentPosition())
}

private fun applyNodePropertiesToBoard(
    board: Board,
    node: SgfNode
) = node.properties.fold(board) { b, property ->
    val newBoard = when (property) {
        is SgfProperty.Move.B -> property.move.asPointOrNull?.let {
            b.placeStone(SgfColor.Black, it)
        }
        is SgfProperty.Move.W -> property.move.asPointOrNull?.let {
            b.placeStone(SgfColor.White, it)
        }
        is SgfProperty.Setup.AB -> b.copy(
            stones = b.stones + property.points.map { it to SgfColor.Black }
        )
        is SgfProperty.Setup.AW -> b.copy(
            stones = b.stones + property.points.map { it to SgfColor.White }
        )
        is SgfProperty.Setup.AE -> b.copy(
            stones = b.stones - property.points
        )
        else -> b
    }

    newBoard ?: b
}

internal fun SgfEditor.insertBranch(node: SgfNode): SgfEditor {
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

internal fun SgfEditor.insertNodeToTheRight(node: SgfNode): SgfEditor {
    val sequence = currentSequence.insertRight(node).goRightUnsafe()

    return copy(
        currentSequence = sequence,
        currentTree = currentTree.update {
            it.copy(sequence = sequence.commit())
        }
    )
}
