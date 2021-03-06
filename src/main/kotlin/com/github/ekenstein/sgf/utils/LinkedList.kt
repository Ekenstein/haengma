package com.github.ekenstein.sgf.utils

/**
 * Represents a linked list which can either be [Cons] or [Nil]
 * [Cons] represents a non-empty list with at least one item that has a tail.
 * [Nil] represents an empty list.
 */
sealed class LinkedList<out T> : AbstractList<T>() {
    override val size: Int
        get() {
            tailrec fun inner(result: Int, acc: LinkedList<T>): Int = when (acc) {
                is Cons -> inner(result + 1, acc.tail)
                Nil -> result
            }
            return inner(0, this)
        }

    override fun get(index: Int): T {
        tailrec fun inner(n: Int, acc: LinkedList<T>): T {
            if (n < 0) {
                throw IndexOutOfBoundsException(index)
            }

            return when (acc) {
                is Cons -> when (n) {
                    0 -> acc.head
                    else -> inner(n - 1, acc.tail)
                }
                Nil -> throw IndexOutOfBoundsException(index)
            }
        }

        return inner(index, this)
    }

    /**
     * Represents an empty [LinkedList]. It has no head or tail.
     */
    object Nil : LinkedList<Nothing>()

    /**
     * Represents a non-empty list with a [head] and a [tail]. The tail can be empty.
     */
    data class Cons<T>(val head: T, val tail: LinkedList<T>) : LinkedList<T>()

    companion object {
        /**
         * Converts the given [list] to a [LinkedList]. If the list is a [LinkedList], the list will be returned,
         * otherwise it will be converted to a [LinkedList].
         */
        fun <T> fromList(list: List<T>): LinkedList<T> = when (list) {
            is LinkedList -> list
            else -> if (list.isEmpty()) {
                Nil
            } else {
                Cons(list[0], fromList(list.drop(1)))
            }
        }
    }

    /**
     * Reverses the current list and appends the [result] at the end of the reversed list.
     */
    fun reverse(result: LinkedList<@UnsafeVariance T> = emptyLinkedList()): LinkedList<T> {
        tailrec fun inner(list: LinkedList<T>, result: LinkedList<T>): LinkedList<T> = when (list) {
            is Cons -> inner(list.tail, Cons(list.head, result))
            Nil -> result
        }

        return inner(this, result)
    }

    /**
     * Returns a [LinkedList] where the given [item] is appended to the current list at the end.
     */
    operator fun plus(item: @UnsafeVariance T): LinkedList<T> = plus(linkedListOf(item))

    /**
     * Returns a [LinkedList] where the given list is appended to the current list at the end.
     */
    operator fun plus(other: LinkedList<@UnsafeVariance T>): LinkedList<T> = when (this) {
        is Cons -> Cons(head, tail + other)
        Nil -> other
    }

    override fun isEmpty(): Boolean = when (this) {
        is Cons -> false
        Nil -> true
    }

    /**
     * Returns a pair of the head in this [LinkedList] and the tail of the [LinkedList].
     * If the [LinkedList] represents [Nil], a pair of null to [Nil] will be returned.
     */
    fun removeFirst(): Pair<T?, LinkedList<T>> = when (this) {
        is Cons -> head to tail
        Nil -> null to Nil
    }

    override fun iterator(): Iterator<T> = LinkedListIterator(this)

    private class LinkedListIterator<T>(private var head: LinkedList<T>) : Iterator<T> {
        override fun hasNext(): Boolean = when (head) {
            is Cons -> true
            Nil -> false
        }

        override fun next(): T {
            val (first, tail) = head.removeFirst()
            head = tail
            return first ?: throw NoSuchElementException()
        }
    }
}

/**
 * Converts the non-null items of the given collection to a [LinkedList]. If the given collection
 * is empty or that it doesn't contain any non-null items, [LinkedList.Nil] will be returned, otherwise
 * [LinkedList.Cons] where the first non-null item will be the head of the [LinkedList].
 */
fun <T> linkedListOfNotNull(vararg items: T?): LinkedList<T> = LinkedList.fromList(items.filterNotNull())

/**
 * Converts the given [items] to a [LinkedList] where the first item will be the head of the [LinkedList]
 * and the rest of the items are the tail of the first item.
 *
 * If the given [items] are empty, [LinkedList.Nil] will be returned, otherwise [LinkedList.Cons]
 */
fun <T> linkedListOf(vararg items: T): LinkedList<T> = LinkedList.fromList(items.toList())

/**
 * Returns [LinkedList.Nil].
 */
fun <T> emptyLinkedList(): LinkedList<T> = LinkedList.Nil

fun <T> List<T>.toLinkedList(): LinkedList<T> = when (this) {
    is LinkedList -> this
    else -> LinkedList.fromList(this)
}
