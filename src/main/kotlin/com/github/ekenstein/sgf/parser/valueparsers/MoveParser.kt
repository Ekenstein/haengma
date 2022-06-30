package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException

internal val moveParser = ValueParser { marker, value ->
    when (value.length) {
        0 -> Move.Pass
        2 -> Move.Stone(pointParser.parse(marker, value))
        else -> marker.throwMalformedPropertyValueException("Expected a move, but got $value")
    }
}
