package com.github.ekenstein.sgf

import com.github.ekenstein.sgf.parser.Marker

sealed class SgfException : Exception() {
    class ParseError(private val reason: String, val marker: Marker, override val cause: Throwable?) : SgfException() {
        constructor(reason: String, marker: Marker) : this(reason, marker, null)

        override val message: String
            get() = "There was an error parsing the SGF due to '$reason'."
    }

    class IllegalMove(private val reason: String, override val cause: Throwable?) : SgfException() {
        constructor(reason: String) : this(reason, null)

        override val message: String = "There was an error playing a move due to '$reason'."
    }
}
