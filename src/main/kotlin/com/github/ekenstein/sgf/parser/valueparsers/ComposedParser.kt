package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.parser.throwParseException

internal fun <L, R> composed(
    leftParser: ValueParser<L>,
    rightParser: ValueParser<R>
) = ValueParser { marker, value ->
    val regex = Regex("""(?<=[^\\]):""")
    val parts = value.split(regex)

    if (parts.size != 2) {
        marker.throwParseException("Expected a composed value, but got $value")
    }

    val (leftPart, rightPart) = parts

    val left = leftParser.parse(
        marker.copy(
            endColumn = marker.endColumn - leftPart.length - 1
        ),
        leftPart
    )

    val right = rightParser.parse(
        marker.copy(
            startColumn = marker.startColumn + leftPart.length + 1,
            endColumn = marker.endColumn - 1
        ),
        rightPart
    )

    left to right
}
