package com.github.ekenstein.sgf.parser

internal fun interface ValueParser<T> {
    fun parse(marker: Marker, string: String): T
}
