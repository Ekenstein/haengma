package com.github.ekenstein.sgf.parser

data class Marker(
    val startLineNumber: Int,
    val startColumn: Int,
    val endLineNumber: Int,
    val endColumn: Int
)

internal fun Marker.throwParseException(message: String, cause: Throwable? = null): Nothing =
    throw SgfParseException(message, this, cause)

class SgfParseException(
    override val message: String,
    val marker: Marker,
    override val cause: Throwable?
) : Exception() {
    constructor(message: String, marker: Marker) : this(message, marker, null)
}
