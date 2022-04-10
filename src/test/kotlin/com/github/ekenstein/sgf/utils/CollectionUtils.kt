package com.github.ekenstein.sgf.utils

fun <T> List<T>.firstAndTail() = first() to drop(1)
