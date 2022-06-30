package com.github.ekenstein.sgf

import com.github.ekenstein.sgf.parser.Marker

/**
 * Represents an exception that occurs while doing some sort of operations associated with SGF.
 */
sealed class SgfException : Exception() {
    sealed class ParseError : SgfException() {
        abstract val marker: Marker
        class MalformedPropertyValue(
            private val reason: String,
            override val marker: Marker,
            override val cause: Throwable?
        ) : ParseError() {
            constructor(reason: String, marker: Marker) : this(reason, marker, null)

            override val message: String
                get() = "There was an error parsing a property value due to '$reason'"
        }

        class SyntaxError(private val reason: String, override val marker: Marker) : ParseError() {
            override val message: String
                get() = "There was an error parsing the SGF due to a syntax error, the reason was '$reason'"
        }

        class Other(private val reason: String, override val marker: Marker) : ParseError() {
            override val message: String
                get() = "There was an error parsing the SGF due to '$reason'"
        }
    }

//    /**
//     * Represents a parse error. Occurs when deserializing an SGF file to an [SgfCollection].
//     */
//    class ParseError(private val reason: String, val marker: Marker, override val cause: Throwable?) : SgfException() {
//        constructor(reason: String, marker: Marker) : this(reason, marker, null)
//
//        override val message: String
//            get() = "There was an error parsing the SGF due to '$reason'."
//    }

    /**
     * Represents an illegal move. Occurs when adding an illegal move to a [SgfGameTree].
     */
    class IllegalMove(reason: String, override val cause: Throwable?) : SgfException() {
        constructor(reason: String) : this(reason, null)

        override val message: String = "There was an error playing a move due to '$reason'."
    }
}
