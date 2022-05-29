package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.flatMap
import com.github.ekenstein.sgf.utils.get
import com.github.ekenstein.sgf.utils.goDownLeft
import com.github.ekenstein.sgf.utils.goLeft
import com.github.ekenstein.sgf.utils.goRight
import com.github.ekenstein.sgf.utils.goToLast
import com.github.ekenstein.sgf.utils.goUp
import com.github.ekenstein.sgf.utils.map
import com.github.ekenstein.sgf.utils.orElse
import com.github.ekenstein.sgf.utils.orStay
import com.github.ekenstein.sgf.utils.toZipper
import com.github.ekenstein.sgf.utils.withOrigin

/**
 * Returns an editor positioned at the next node. If there are no more nodes in the current sequence
 * after the current node, the editor will try to position itself at the beginning of the left-most
 * child tree. If there are no more nodes in the current sequence and no child trees, the current
 * position will be returned.
 */
fun SgfEditor.goToNextNodeOrStay(): SgfEditor = goToNextNode().orStay()

/**
 * Returns an editor positioned at the next node. If there is no node to the right of the current node,
 * the editor will try to position itself at the beginning of the left-most
 * child tree. If there are no more nodes in the current sequence and no child trees, a [MoveResult.Failure]
 * will be returned, otherwise [MoveResult.Success]
 */
fun SgfEditor.goToNextNode(): MoveResult<SgfEditor> = goToNextNodeInSequence().orElse {
    goToLeftMostChildTree()
}

internal fun SgfEditor.goToNextNodeInSequence(): MoveResult<SgfEditor> = currentSequence.goRight().map(this) {
    copy(currentSequence = it)
}

/**
 * Returns an editor positioned at the last node. This will traverse the current sequence and
 * all the left-most child trees associated with the current sequence.
 */
tailrec fun SgfEditor.goToLastNode(): SgfEditor = when (val next = goToNextNode()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> next.position.goToLastNode()
}

/**
 * Returns an editor positioned at the previous node. If there is no node to the left of the current node,
 * the editor will try to position itself at the end of the parent sequence.
 * If there is no node to the left of the current node or no parent, the current position will be returned.
 */
fun SgfEditor.goToPreviousNodeOrStay(): SgfEditor = goToPreviousNode().orStay()

/**
 * Returns an editor positioned at the previous node. If there is no node to the left of the current node,
 * the editor will try to position itself at the end of the parent sequence.
 * If there is no node to the left of the current node or no parent, a [MoveResult.Failure] will be returned,
 * otherwise [MoveResult.Success]
 */
fun SgfEditor.goToPreviousNode(): MoveResult<SgfEditor> = goToPreviousNodeInSequence().orElse {
    goToParentTree()
}

internal fun SgfEditor.goToPreviousNodeInSequence(): MoveResult<SgfEditor> = currentSequence.goLeft().map(this) {
    copy(currentSequence = it)
}

/**
 * Returns an editor that is positioned at the root node. If the editor is already stationed
 * at the root node, this editor will be returned.
 */
tailrec fun SgfEditor.goToRootNode(): SgfEditor = when (val previous = goToPreviousNode()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> previous.position.goToRootNode()
}

/**
 * Returns an editor that is positioned at the beginning of the left-most child tree. If there are no children
 * to the current tree, the current position will be returned.
 */
fun SgfEditor.goToLeftMostChildTreeOrStay(): SgfEditor = goToLeftMostChildTree().orStay()

/**
 * Returns an editor that is positioned at the beginning of the left-most child tree. If there are no
 * children a [MoveResult.Failure] will be returned, otherwise [MoveResult.Success]
 */
fun SgfEditor.goToLeftMostChildTree() = currentTree.goDownLeft().map(this) {
    copy(
        currentSequence = it.focus.sequence.toZipper(),
        currentTree = it,
    )
}

/**
 * Returns an editor that is positioned at the beginning of the tree to the right of the current tree.
 * If there are no trees to the right of the current tree, [MoveResult.Failure] will be returned,
 * otherwise [MoveResult.Success]
 */
fun SgfEditor.goToNextTree(): MoveResult<SgfEditor> = currentTree.goRight().map(this) {
    copy(
        currentSequence = it.focus.sequence.toZipper(),
        currentTree = it
    )
}

/**
 * Returns an editor that is positioned at the beginning of the tree to the right of the current tree.
 * If there are no trees to the right of the current tree, the current position will be returned.
 */
fun SgfEditor.goToNextTreeOrStay() = goToNextTree().orStay()

/**
 * Returns an editor that is positioned at the beginning of the tree to the left of the current tree.
 * If there are no trees to the left of the current tree, [MoveResult.Failure] will be returned,
 * otherwise [MoveResult.Success]
 */
fun SgfEditor.goToPreviousTree() = currentTree.goLeft().map(this) {
    copy(
        currentSequence = it.focus.sequence.toZipper(),
        currentTree = it
    )
}

/**
 * Returns an editor that is positioned at the beginning of the tree to the left of the current tree.
 * If there are no trees to the left of the current tree, the current position will be returned.
 */
fun SgfEditor.goToPreviousTreeOrStay() = goToPreviousTree().orStay()

/**
 * Returns an editor position at the end of the parent tree. If there is no parent to the given
 * position, the current position will be returned.
 */
fun SgfEditor.goToParentTreeOrStay() = goToParentTree().orStay()

/**
 * Returns an editor position at the end of the parent tree. If there is no parent to the given
 * position, [MoveResult.Failure] will be returned, otherwise [MoveResult.Success]
 */
fun SgfEditor.goToParentTree(): MoveResult<SgfEditor> = currentTree.goUp().map(this) {
    copy(
        currentSequence = it.focus.sequence.toZipper().goToLast(),
        currentTree = it
    )
}

/**
 * Repeats a move [n] number of times.
 */
fun SgfEditor.tryRepeat(n: Int, move: (SgfEditor) -> MoveResult<SgfEditor>): MoveResult<SgfEditor> {
    tailrec fun inner(n: Int, acc: SgfEditor): MoveResult<SgfEditor> = if (n < 1) {
        acc.stay()
    } else {
        when (val result = move(acc)) {
            is MoveResult.Failure -> result
            is MoveResult.Success -> inner(n - 1, result.position)
        }
    }

    return inner(n, this).withOrigin(this)
}

/**
 * Repeats a move [n] number of times or throw if impossible.
 */
fun SgfEditor.repeat(n: Int, move: (SgfEditor) -> MoveResult<SgfEditor>): SgfEditor =
    tryRepeat(n, move).get()

/**
 * Repeats the given [move] while the [condition] returns true.
 */
fun SgfEditor.tryRepeatWhile(
    condition: (SgfEditor) -> Boolean,
    move: (SgfEditor) -> MoveResult<SgfEditor>
): MoveResult<SgfEditor> {
    tailrec fun inner(acc: SgfEditor): MoveResult<SgfEditor> = if (!condition(acc)) {
        acc.stay()
    } else {
        when (val result = move(acc)) {
            is MoveResult.Failure -> result
            is MoveResult.Success -> inner(result.position)
        }
    }

    return inner(this).withOrigin(this)
}

/**
 * Repeats the given [move] while the given [condition] returns false.
 */
fun SgfEditor.tryRepeatWhileNot(
    condition: (SgfEditor) -> Boolean,
    move: (SgfEditor) -> MoveResult<SgfEditor>
): MoveResult<SgfEditor> {
    fun not(focus: SgfEditor) = !condition(focus)
    return tryRepeatWhile(::not, move)
}

fun SgfEditor.stay(): MoveResult<SgfEditor> = MoveResult.Success(this, this)

/**
 * Goes to the next "hotspot" in the tree.
 */
fun SgfEditor.goToNextHotspot(): MoveResult<SgfEditor> {
    fun isHotspot(editor: SgfEditor) = editor.isHotspot()

    return if (isHotspot()) {
        goToNextNode().flatMap { next ->
            next.tryRepeatWhileNot(::isHotspot) {
                it.goToNextNode()
            }
        }
    } else {
        tryRepeatWhileNot(::isHotspot) {
            it.goToNextNode()
        }
    }
}

/**
 * Goes to the previous "hotspot" in the tree.
 */
fun SgfEditor.goToPreviousHotspot(): MoveResult<SgfEditor> {
    fun isHotspot(editor: SgfEditor) = editor.isHotspot()

    return if (isHotspot()) {
        goToPreviousNode().flatMap { next ->
            next.tryRepeatWhileNot(::isHotspot) {
                it.goToPreviousNode()
            }
        }
    } else {
        tryRepeatWhileNot(::isHotspot) {
            it.goToPreviousNode()
        }
    }
}
