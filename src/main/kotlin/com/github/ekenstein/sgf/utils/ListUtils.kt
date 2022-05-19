package com.github.ekenstein.sgf.utils

import java.lang.Integer.min

val <T> List<T>.head: T
    get() = first()

val <T> List<T>.tail: List<T>
    get() = drop(1)

fun <T> List<T>.replace(index: Int, with: T): List<T> = when (index) {
    size - 1 -> replaceLast(with)
    0 -> replaceFirst(with)
    else -> subList(0, index) + with + subList(min(size, index + 1), size)
}
fun <T> List<T>.replaceLast(with: T): List<T> = take(size - 1) + with
fun <T> List<T>.replaceFirst(with: T): List<T> = listOf(with) + drop(1)

fun <T> List<T>.headAndTail(): Pair<T, List<T>> = head to tail
