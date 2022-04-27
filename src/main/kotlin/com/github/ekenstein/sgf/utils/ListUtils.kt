package com.github.ekenstein.sgf.utils

import java.lang.Integer.min

fun <T> List<T>.replace(index: Int, with: T): List<T> = when (index) {
    size - 1 -> replaceLast(with)
    0 -> replaceFirst(with)
    else -> subList(0, index) + with + subList(min(size, index + 1), size)
}
fun <T> List<T>.replaceLast(with: T): List<T> = take(size - 1) + with
fun <T> List<T>.replaceFirst(with: T): List<T> = drop(1).prepend(with)
fun <T> List<T>.prepend(item: T) = listOf(item) + this
