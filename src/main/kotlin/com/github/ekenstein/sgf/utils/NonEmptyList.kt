package com.github.ekenstein.sgf.utils

class NonEmptyList<out T>(
    val head: T,
    val tail: List<T>
) : AbstractList<T>() {
    private val all: List<T> = listOf(head) + tail

    operator fun plus(nel: NonEmptyList<@UnsafeVariance T>): NonEmptyList<T> = nelOf(head, tail + nel.all)
    operator fun plus(item: @UnsafeVariance T): NonEmptyList<T> = nelOf(head, tail + item)
    operator fun plus(list: List<@UnsafeVariance T>): NonEmptyList<T> = nelOf(head, tail + list)

    companion object {
        fun <T> fromList(list: List<T>): NonEmptyList<T>? = if (list.isEmpty()) {
            null
        } else {
            nelOf(list.head, list.tail)
        }

        fun <T> fromListUnsafe(list: List<T>) = checkNotNull(fromList(list)) {
            "The list is empty."
        }
    }

    override val size: Int = 1 + tail.size

    override fun get(index: Int): T = when (index) {
        0 -> head
        else -> tail[index - 1]
    }
}

fun <T> nelOf(head: T, tail: List<T>): NonEmptyList<T> = NonEmptyList(head, tail)
fun <T> nelOf(head: T, vararg tail: T): NonEmptyList<T> = nelOf(head, tail.toList())

fun <T> List<T>.toNel(): NonEmptyList<T>? = NonEmptyList.fromList(this)
fun <T> List<T>.toNelUnsafe(): NonEmptyList<T> = NonEmptyList.fromListUnsafe(this)

fun <T> LinkedList<T>.toNel(): NonEmptyList<T>? = when (this) {
    is LinkedList.Cons -> nelOf(head, tail)
    LinkedList.Nil -> null
}
fun <T> LinkedList<T>.toNelUnsafe(): NonEmptyList<T> = toNel() ?: error("The list is empty")
