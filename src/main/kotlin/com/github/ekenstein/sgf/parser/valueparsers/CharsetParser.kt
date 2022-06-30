package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException

internal val charsetParser = ValueParser { marker, value ->
    try {
        Charset.forName(value)
    } catch (ex: UnsupportedCharsetException) {
        marker.throwMalformedPropertyValueException("Expected a charset, but got $value", ex)
    }
}
