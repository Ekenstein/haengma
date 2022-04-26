package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.Move

internal val moveParser = ValueParser { marker, value ->
    when (value.length) {
        0 -> Move.Pass
        2 -> Move.Stone(pointParser.parse(marker, value))
        else -> marker.throwParseException("Expected a move, but got $value")
    }
}
