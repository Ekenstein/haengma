package com.github.ekenstein.sgf.utils

data class Zipper<T>(val left: LinkedList<T>, val focus: T, val right: LinkedList<T>) {
    companion object {
        fun <T> fromList(list: NonEmptyList<T>): Zipper<T> {
            return Zipper(LinkedList.Nil, list.head, LinkedList.fromList(list.tail))
        }

        fun <T> of(value: T) = Zipper(LinkedList.Nil, value, LinkedList.Nil)
    }
}

fun <T> NonEmptyList<T>.toZipper() = Zipper.fromList(this)

fun <T> Zipper<T>.goLeft(): MoveResult<Zipper<T>> = when (left) {
    is LinkedList.Nil -> MoveResult.Failure(this)
    is LinkedList.Cons -> {
        val (head, tail) = left
        MoveResult.Success(
            value = copy(tail, head, linkedListOf(focus) + right),
            origin = this
        )
    }
}

fun <T> Zipper<T>.goRight(): MoveResult<Zipper<T>> = when (right) {
    LinkedList.Nil -> MoveResult.Failure(this)
    is LinkedList.Cons -> {
        val (head, tail) = right
        MoveResult.Success(
            value = copy(
                left = linkedListOf(focus) + left,
                focus = head,
                right = tail
            ),
            origin = this
        )
    }
}

fun <T> Zipper<T>.goRightUnsafe(): Zipper<T> = goRight().orNull()
    ?: error("The right-most item has already been reached")

tailrec fun <T> Zipper<T>.goToLast(): Zipper<T> = when (val next = goRight()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> next.value.goToLast()
}

tailrec fun <T> Zipper<T>.goToFirst(): Zipper<T> = when (val prev = goLeft()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> prev.value.goToFirst()
}

fun <T> Zipper<T>.insertLeft(item: T) = copy(
    left = linkedListOf(item) + left
)

fun <T> Zipper<T>.insertRight(item: T) = copy(
    right = linkedListOf(item) + right
)

fun <T> Zipper<T>.commit() = NonEmptyList.fromListUnsafe(left.reverse(linkedListOf(focus) + right))

fun <T> Zipper<T>.commitAtCurrentPosition() = NonEmptyList.fromListUnsafe(left.reverse(linkedListOf(focus)))

fun <T> Zipper<T>.update(f: (T) -> T) = copy(
    focus = f(focus)
)
