package com.github.ekenstein.sgf.utils

val <T> List<T>.head: T
    get() = first()

val <T> List<T>.tail: List<T>
    get() = drop(1)

fun <T> List<T>.headAndTail(): Pair<T, List<T>> = head to tail
