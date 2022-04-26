package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.SgfColor

internal val colorParser = ValueParser { marker, value ->
    when (value) {
        "W" -> SgfColor.White
        "B" -> SgfColor.Black
        else -> marker.throwParseException("Expected a color, but got $value")
    }
}
