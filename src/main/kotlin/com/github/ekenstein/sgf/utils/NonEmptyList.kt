package com.github.ekenstein.sgf.utils

/**
 * Represents a non-empty list containing a [head] and a possibly empty [tail].
 *
 * To create a non-empty list, use either [nelOf], [List.toNel] or [List.toNelUnsafe].
 * @param head the first item in the list
 * @param tail the possibly empty tail.
 */
class NonEmptyList<out T>(
    val head: T,
    val tail: List<T>
) : AbstractList<T>() {
    private val all: List<T> = listOf(head) + tail

    operator fun plus(nel: NonEmptyList<@UnsafeVariance T>): NonEmptyList<T> = nelOf(head, tail + nel.all)
    operator fun plus(item: @UnsafeVariance T): NonEmptyList<T> = nelOf(head, tail + item)
    operator fun plus(list: List<@UnsafeVariance T>): NonEmptyList<T> = nelOf(head, tail + list)

    /**
     * Returns always false.
     */
    override fun isEmpty(): Boolean = false

    override val size: Int = 1 + tail.size

    override fun get(index: Int): T = when (index) {
        0 -> head
        else -> tail[index - 1]
    }
}

/**
 * Creates a [NonEmptyList] where [head] will be the first item in the list, and the tail
 * the rest of the list.
 */
fun <T> nelOf(head: T, tail: List<T>): NonEmptyList<T> = NonEmptyList(head, tail)

/**
 * Creates a [NonEmptyList] where [head] will be the first item in the list, and the tail
 * the rest of the list.
 */
fun <T> nelOf(head: T, vararg tail: T): NonEmptyList<T> = nelOf(head, tail.toList())

/**
 * Converts the given list to a [NonEmptyList] or returns null if the list is empty.
 */
fun <T> List<T>.toNel(): NonEmptyList<T>? = when (this) {
    is NonEmptyList -> this
    is LinkedList -> when (this) {
        is LinkedList.Cons -> NonEmptyList(head, tail)
        LinkedList.Nil -> null
    }
    else -> if (isEmpty()) {
        null
    } else {
        nelOf(get(0), drop(1))
    }
}

fun <T> List<T>.toNelUnsafe(): NonEmptyList<T> = toNel() ?: error("The list is empty.")
