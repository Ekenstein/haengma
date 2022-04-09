package com.github.ekenstein.sgf

import kotlin.test.fail

fun <T> assertDoesNotFail(block: () -> T) = try {
    block()
} catch (ex: Exception) {
    fail("Unexpectedly failed with $ex")
}
