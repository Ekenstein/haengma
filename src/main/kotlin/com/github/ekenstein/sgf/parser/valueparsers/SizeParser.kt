package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException

internal val sizeParser = ValueParser { marker, value ->
    val numberParser = numberParser(1..52)
    try {
        val number = numberParser.parse(marker, value)
        number to number
    } catch (ex: SgfException.ParseError) {
        try {
            composed(numberParser, numberParser).parse(marker, value)
        } catch (ex: SgfException.ParseError) {
            marker.throwMalformedPropertyValueException(
                "Expected a number or a composed value containing numbers, " +
                    "but got $value"
            )
        }
    }
}
