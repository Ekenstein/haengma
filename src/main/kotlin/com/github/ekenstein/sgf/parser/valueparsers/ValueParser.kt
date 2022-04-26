package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.parser.Marker

internal fun interface ValueParser<T> {
    fun parse(marker: Marker, string: String): T
}
