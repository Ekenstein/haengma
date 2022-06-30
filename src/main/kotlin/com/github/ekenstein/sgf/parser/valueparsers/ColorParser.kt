package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException

internal val colorParser = ValueParser { marker, value ->
    when (value) {
        "W" -> SgfColor.White
        "B" -> SgfColor.Black
        else -> marker.throwMalformedPropertyValueException("Expected a color, but got $value")
    }
}
