package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.SgfPoint

internal val pointParser = ValueParser { marker, value ->
    fun fromCharToInt(char: Char): Int = when (char) {
        in 'a'..'z' -> char - 'a' + 1
        in 'A'..'Z' -> char - 'A' + 27
        else -> marker.throwParseException("Expected a point, but got $value")
    }

    when (value.length) {
        2 -> SgfPoint(
            x = fromCharToInt(value[0]),
            y = fromCharToInt(value[1])
        )
        else -> marker.throwParseException("Expected a point, but got $value")
    }
}
