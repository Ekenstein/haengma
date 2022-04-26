package com.github.ekenstein.sgf.parser

internal val realParser = ValueParser { marker, value ->
    value.toDoubleOrNull()
        ?: marker.throwParseException("Expected a real value, but got $value")
}

internal fun numberParser(range: IntRange? = null) = ValueParser { marker, value ->
    val number = value.toIntOrNull()
        ?: marker.throwParseException("Expected a number, but got $value")

    if (range != null && number !in range) {
        marker.throwParseException("The number $number must be within the range ${range.first} - ${range.last}")
    }

    number
}
