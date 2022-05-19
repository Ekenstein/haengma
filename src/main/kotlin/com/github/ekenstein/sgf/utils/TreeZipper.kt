package com.github.ekenstein.sgf.utils

interface Unzip<T> {
    fun unzip(node: T): LinkedList<T>
    fun zip(node: T, children: LinkedList<T>): T
}

data class TreeZipper<T>(
    val left: LinkedList<T>,
    val focus: T,
    val right: LinkedList<T>,
    val top: TreeZipper<T>?,
    val unzip: Unzip<T>
)

fun <T> TreeZipper<T>.goLeft() = when (left) {
    is LinkedList.Cons -> {
        val (head, tail) = left
        copy(
            left = tail,
            focus = head,
            right = linkedListOf(focus) + right
        )
    }
    LinkedList.Nil -> null
}

fun <T> TreeZipper<T>.goRight() = when (right) {
    is LinkedList.Cons -> {
        val (head, tail) = right
        copy(
            left = linkedListOf(focus) + left,
            focus = head,
            right = tail
        )
    }
    LinkedList.Nil -> null
}

tailrec fun <T> TreeZipper<T>.goRightUntil(predicate: (T) -> Boolean): TreeZipper<T>? = if (predicate(focus)) {
    this
} else {
    goRight()?.goRightUntil(predicate)
}

fun <T> TreeZipper<T>.goDownLeft() = when (val children = unzip.unzip(focus)) {
    is LinkedList.Cons -> {
        val (head, tail) = children
        copy(
            left = emptyLinkedList(),
            focus = head,
            right = tail,
            top = this
        )
    }
    LinkedList.Nil -> null
}

fun <T> TreeZipper<T>.goUp() = top?.let {
    val children = (linkedListOf(focus) + left).reverse(right)
    it.copy(
        focus = unzip.zip(it.focus, children)
    )
}

tailrec fun <T> TreeZipper<T>.goToRoot(): TreeZipper<T> = when (val parent = goUp()) {
    null -> this
    else -> parent.goToRoot()
}

fun <T> TreeZipper<T>.set(value: T) = copy(
    focus = value
)

fun <T> TreeZipper<T>.update(f: (T) -> T) = copy(
    focus = f(focus)
)

fun <T> TreeZipper<T>.insertLeft(value: T) = copy(
    left = linkedListOf(value) + left
)

fun <T> TreeZipper<T>.insertRight(value: T) = copy(
    right = linkedListOf(value) + right
)

fun <T> TreeZipper<T>.insertDownLeft(
    values: LinkedList<T>
) = when (val children = values + unzip.unzip(focus)) {
    is LinkedList.Cons -> {
        copy(left = emptyLinkedList(), focus = children.head, right = children.tail, top = this)
    }
    LinkedList.Nil -> null
}

fun <T> TreeZipper<T>.commit() = goToRoot().focus

fun <T> TreeZipper<T>.deleteAndMoveLeft() = when (left) {
    is LinkedList.Cons -> copy(left = left.tail, focus = left.head)
    LinkedList.Nil -> null
}
