package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.SgfException

data class Marker(
    val startLineNumber: Int,
    val startColumn: Int,
    val endLineNumber: Int,
    val endColumn: Int
)

internal fun Marker.throwMalformedPropertyValueException(message: String, cause: Throwable? = null): Nothing =
    throw SgfException.ParseError.MalformedPropertyValue(message, this, cause)
