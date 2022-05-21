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
) {
    companion object {
        fun <T> ofNode(node: T, unzip: Unzip<T>) = TreeZipper(
            left = LinkedList.Nil,
            focus = node,
            right = LinkedList.Nil,
            top = null,
            unzip = unzip
        )
    }
}

fun <T> TreeZipper<T>.goLeft() = when (left) {
    is LinkedList.Cons -> {
        val (head, tail) = left
        MoveResult.Success(
            value = copy(
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
            value = copy(
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
            value = copy(
                left = emptyLinkedList(),
                focus = head,
                right = tail,
                top = this
            ),
            origin = this
        )
    }
    LinkedList.Nil -> MoveResult.Failure(this)
}

fun <T> TreeZipper<T>.goUp() = when (top) {
    null -> MoveResult.Failure(this)
    else -> {
        val children = (linkedListOf(focus) + left).reverse(right)
        MoveResult.Success(
            top.copy(
                focus = unzip.zip(top.focus, children)
            ),
            this
        )
    }
}

tailrec fun <T> TreeZipper<T>.goToRoot(): TreeZipper<T> = when (val parent = goUp()) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> parent.value.goToRoot()
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
    is LinkedList.Cons -> {
        copy(left = emptyLinkedList(), focus = children.head, right = children.tail, top = this)
    }
    LinkedList.Nil -> error("There are no nodes to insert")
}

fun <T> TreeZipper<T>.commit() = goToRoot().focus
