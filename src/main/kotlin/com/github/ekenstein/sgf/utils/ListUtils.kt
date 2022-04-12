package com.github.ekenstein.sgf.utils

import java.lang.Integer.min

fun <T> List<T>.replace(index: Int, with: T): List<T> = subList(0, index) + with + subList(min(size, index + 1), size)
fun <T> List<T>.replaceLast(with: T): List<T> = take(size - 1) + with
