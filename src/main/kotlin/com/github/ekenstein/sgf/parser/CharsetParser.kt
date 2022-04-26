package com.github.ekenstein.sgf.parser

import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException

internal val charsetParser = ValueParser { marker, value ->
    try {
        Charset.forName(value)
    } catch (ex: UnsupportedCharsetException) {
        marker.throwParseException("Expected a charset, but got $value", ex)
    }
}
