package com.github.ekenstein.sgf.utils

interface Unzip<T> {
    fun unzip(node: T): LinkedList<T>
    fun zip(node: T, children: LinkedList<T>): T
}

typealias Forest<T> = LinkedList<T>

data class TreeZipper<T>(
    val left: Forest<T>,
    val focus: T,
    val right: Forest<T>,
    val top: LinkedList<Triple<Forest<T>, T, Forest<T>>>,
    val unzip: Unzip<T>
) {
    companion object {
        fun <T> ofNode(node: T, unzip: Unzip<T>) = TreeZipper(
            left = LinkedList.Nil,
            focus = node,
            right = LinkedList.Nil,
            top = LinkedList.Nil,
            unzip = unzip
        )
    }
}

fun <T> TreeZipper<T>.goLeft() = when (left) {
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
    LinkedList.Nil -> MoveResult.Failure(this)
}

fun <T> TreeZipper<T>.goRight() = when (right) {
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
    LinkedList.Nil -> MoveResult.Failure(this)
}

fun <T> TreeZipper<T>.goDownLeft() = when (val children = unzip.unzip(focus)) {
    is LinkedList.Cons -> {
        val (head, tail) = children
        MoveResult.Success(
            position = copy(
                left = emptyLinkedList(),
                focus = head,
                right = tail,
                top = linkedListOf(Triple(left, focus, right)) + top
            ),
            origin = this
        )
    }
    LinkedList.Nil -> MoveResult.Failure(this)
}

fun <T> TreeZipper<T>.goUp() = when (top) {
    LinkedList.Nil -> MoveResult.Failure(this)
    is LinkedList.Cons -> {
        val children = (linkedListOf(focus) + left).reverse(right)
        val (head, tail) = top
        val (topLeft, topFocus, topRight) = head
        MoveResult.Success(
            TreeZipper(
                left = topLeft,
                focus = unzip.zip(topFocus, children),
                right = topRight,
                top = tail,
                unzip
            ),
            this
        )
    }
}

tailrec fun <T> TreeZipper<T>.goToRoot(): TreeZipper<T> = when (val parent = goUp()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> parent.position.goToRoot()
}

fun <T> TreeZipper<T>.set(value: T) = copy(
    focus = value
)

fun <T> TreeZipper<T>.update(f: (T) -> T) = copy(
    focus = f(focus)
)

fun <T> TreeZipper<T>.insertDownLeft(
    values: LinkedList<T>
) = when (val children = values + unzip.unzip(focus)) {
    is LinkedList.Cons -> copy(
        left = emptyLinkedList(),
        focus = children.head,
        right = children.tail,
        top = linkedListOf(Triple(left, focus, right)) + top
    )
    LinkedList.Nil -> error("There are no nodes to insert")
}

fun <T> TreeZipper<T>.insertDownRight(values: LinkedList<T>) = when (values) {
    is LinkedList.Cons -> copy(
        left = unzip.unzip(focus).reverse(),
        focus = values.head,
        right = values.tail,
        top = linkedListOf(Triple(left, focus, right)) + top
    )
    LinkedList.Nil -> error("There are no nodes to insert")
}

fun <T> TreeZipper<T>.commit() = goToRoot().focus

/**
 * Returns the index of the focus.
 */
fun TreeZipper<*>.indexOfCurrent() = left.size
