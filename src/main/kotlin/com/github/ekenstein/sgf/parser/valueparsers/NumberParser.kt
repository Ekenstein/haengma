package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException

internal val realParser = ValueParser { marker, value ->
    value.toDoubleOrNull()
        ?: marker.throwMalformedPropertyValueException("Expected a real value, but got $value")
}

internal fun numberParser(range: IntRange? = null) = ValueParser { marker, value ->
    val number = value.toIntOrNull()
        ?: marker.throwMalformedPropertyValueException("Expected a number, but got $value")

    if (range != null && number !in range) {
        marker.throwMalformedPropertyValueException(
            "The number $number must be within the " +
                "range ${range.first} - ${range.last}"
        )
    }

    number
}
