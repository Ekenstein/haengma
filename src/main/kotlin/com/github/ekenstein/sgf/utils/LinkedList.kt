package com.github.ekenstein.sgf.utils

sealed class LinkedList<out T>(open val tail: LinkedList<T>) : AbstractList<T>() {
    override val size: Int
        get() = when (this) {
            is Cons -> 1 + tail.size
            Nil -> 0
        }

    override fun get(index: Int): T = when (index) {
        0 -> when (this) {
            is Cons -> head
            Nil -> throw IndexOutOfBoundsException(index)
        }
        else -> tail[index - 1]
    }

    object Nil : LinkedList<Nothing>(Nil)
    data class Cons<T>(val head: T, override val tail: LinkedList<T>) : LinkedList<T>(tail)

    companion object {
        fun <T> fromList(list: List<T>): LinkedList<T> {
            return if (list.isEmpty()) {
                Nil
            } else {
                val (head, tail) = list.headAndTail()
                Cons(head, fromList(tail))
            }
        }
    }

    fun reverse(result: LinkedList<@UnsafeVariance T> = emptyLinkedList()): LinkedList<T> = when (this) {
        is Cons -> {
            tail.reverse(linkedListOf(head) + result)
        }
        Nil -> result
    }

    operator fun plus(item: @UnsafeVariance T): LinkedList<T> = plus(linkedListOf(item))

    operator fun plus(other: LinkedList<@UnsafeVariance T>): LinkedList<T> = when (this) {
        is Cons -> Cons(head, tail + other)
        Nil -> other
    }

    override fun isEmpty(): Boolean = when (this) {
        is Cons -> false
        Nil -> true
    }

    fun removeFirst(): Pair<T?, LinkedList<T>> = when (this) {
        is Cons -> head to tail
        Nil -> null to this
    }
}

fun <T> linkedListOf(vararg items: T): LinkedList<T> = LinkedList.fromList(items.toList())
fun <T> emptyLinkedList(): LinkedList<T> = LinkedList.Nil

fun <T> List<T>.toLinkedList(): LinkedList<T> = when (this) {
    is LinkedList -> this
    else -> LinkedList.fromList(this)
}
