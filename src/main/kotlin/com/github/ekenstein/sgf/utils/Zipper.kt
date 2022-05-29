package com.github.ekenstein.sgf.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents a zipper that makes it easy and fast to navigate and splice in items in a list.
 * [focus] represents the current item you're at, [left] are the items to the left of the [focus]
 * and [right] are the items to the right of the [focus].
 *
 * When you're done you can commit the list by calling the function [Zipper.commit].
 *
 * @param focus The current item you're at
 * @param left The items to the left of the [focus]. Note that the left items should be in reversed order.
 * @param right The items to the right of the [focus].
 */
data class Zipper<T>(val left: LinkedList<T>, val focus: T, val right: LinkedList<T>)

/**
 * Converts a [NonEmptyList] to a [Zipper] where the [NonEmptyList.head] is the focus of the [Zipper] and the
 * [NonEmptyList.tail] to the right of the focus.
 */
fun <T> NonEmptyList<T>.toZipper() = Zipper(LinkedList.Nil, head, LinkedList.fromList(tail))

/**
 * Returns a [Zipper] that is positioned to the left of the current focus.
 *
 * If there are no items to the left of the focus, a [MoveResult.Failure] will be returned,
 * otherwise a [MoveResult.Success] containing the new [Zipper].
 */
fun <T> Zipper<T>.goLeft(): MoveResult<Zipper<T>> = when (left) {
    is LinkedList.Nil -> MoveResult.Failure(this)
    is LinkedList.Cons -> {
        val (head, tail) = left
        MoveResult.Success(
            position = copy(
                left = tail,
                focus = head,
                right = linkedListOf(focus) + right
            ),
            origin = this
        )
    }
}

/**
 * Returns a [Zipper] that is positioned to the right of the current focus.
 *
 * If there are no items to the right of the focus, a [MoveResult.Failure] containing this zipper as origin, is
 * returned, otherwise a [MoveResult.Success] containing the new [Zipper] and this zipper as origin.
 */
fun <T> Zipper<T>.goRight(): MoveResult<Zipper<T>> = when (right) {
    LinkedList.Nil -> MoveResult.Failure(this)
    is LinkedList.Cons -> {
        val (head, tail) = right
        MoveResult.Success(
            position = copy(
                left = linkedListOf(focus) + left,
                focus = head,
                right = tail
            ),
            origin = this
        )
    }
}

/**
 * Returns a [Zipper] that is positioned to the right of the current focus. If there are no items to
 * the right of the focus, an [IllegalStateException] will be thrown.
 *
 * @throws IllegalStateException if there are no items to the right of the focus.
 */
fun <T> Zipper<T>.goRightUnsafe(): Zipper<T> = goRight().orError {
    "The right-most item has already been reached"
}

/**
 * Goes to the last item of the given zipper. The current focus will be the last item in the left items iff
 * there are any items to the right of the current focus. Otherwise, the current focus will remain the same.
 */
tailrec fun <T> Zipper<T>.goToLast(): Zipper<T> = when (val next = goRight()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> next.position.goToLast()
}

/**
 * Goes to the first item of the given zipper. The current focus will be the last item in the right items
 * iff there are any items to the left of the current focus. Otherwise, the current focus will remain the same.
 */
tailrec fun <T> Zipper<T>.goToFirst(): Zipper<T> = when (val prev = goLeft()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> prev.position.goToFirst()
}

/**
 * Inserts the given [item] to the left of the current focus.
 */
fun <T> Zipper<T>.insertLeft(item: T) = copy(
    left = linkedListOf(item) + left
)

/**
 * Inserts the given [item] to the right of the current focus.
 */
fun <T> Zipper<T>.insertRight(item: T) = copy(
    right = linkedListOf(item) + right
)

/**
 * Commits the given zipper to a [NonEmptyList].
 */
fun <T> Zipper<T>.commit() = left.reverse(linkedListOf(focus) + right).toNelUnsafe()

/**
 * Commits the given zipper to a [NonEmptyList] from the current position.
 * Meaning that the [NonEmptyList.head] will be the left-most item in the zipper and
 * the last item in [NonEmptyList.tail] will be the current focus of the zipper.
 */
fun <T> Zipper<T>.commitAtCurrentPosition() = left.reverse(linkedListOf(focus)).toNelUnsafe()

/**
 *  Returns a [Zipper] where the focus is the updated focus. Left and right remains the same.
 */
@OptIn(ExperimentalContracts::class)
fun <T> Zipper<T>.update(f: (T) -> T): Zipper<T> {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    return copy(
        focus = f(focus)
    )
}
