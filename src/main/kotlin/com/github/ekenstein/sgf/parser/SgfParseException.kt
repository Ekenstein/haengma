package com.github.ekenstein.sgf.parser

data class Marker(
    val startLineNumber: Int,
    val startColumn: Int,
    val endLineNumber: Int,
    val endColumn: Int
)

class SgfParseException(override val message: String, val marker: Marker) : Exception()
