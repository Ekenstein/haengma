package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.SgfDouble

internal val doubleParser = ValueParser { marker, value ->
    when (value) {
        "1" -> SgfDouble.Normal
        "2" -> SgfDouble.Emphasized
        else -> marker.throwParseException("Expected a double, but got $value")
    }
}
