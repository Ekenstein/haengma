package com.github.ekenstein.sgf.utils

/**
 * Represents a set containing items of type [T] which can't be empty.
 */
class NonEmptySet<out T>(private val head: T, private val tail: Set<T>) : AbstractSet<T>() {
    private val all = setOf(head) + tail

    override val size: Int = all.size

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<T> = all.iterator()

    operator fun plus(item: @UnsafeVariance T): NonEmptySet<T> = nonEmptySetOf(item, all)

    operator fun plus(items: Set<@UnsafeVariance T>): NonEmptySet<T> = nonEmptySetOf(head, tail + items)
}

/**
 * Returns a [NonEmptySet] containing the given [head] and [tail]
 */
fun <T> nonEmptySetOf(head: T, vararg tail: T) = nonEmptySetOf(head, tail.toSet())

/**
 * Returns a [NonEmptySet] containing the given [head] and [tail]
 */
fun <T> nonEmptySetOf(head: T, tail: Set<T>) = NonEmptySet(head, tail)

/**
 * Converts the collection to a [NonEmptySet] or null if the collection is empty.
 */
fun <T> Collection<T>.toNonEmptySet(): NonEmptySet<T>? = takeIf { it.isNotEmpty() }
    ?.let {
        val head = first()
        val tail = drop(1)
        NonEmptySet(head, tail.toSet())
    }

fun <T> Collection<T>.toNonEmptySetUnsafe(): NonEmptySet<T> = requireNotNull(toNonEmptySet()) {
    "The collection is empty"
}
