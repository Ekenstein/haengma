package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException

private data class Composed(val left: String, val right: String)

private fun split(value: String): List<String> {
    val ret = mutableListOf<String>()
    val current = mutableListOf<Char>()

    val iterator = value.iterator()
    while (iterator.hasNext()) {
        when (val char = iterator.nextChar()) {
            '\\' -> {
                current.add(char)
                if (iterator.hasNext()) {
                    current.add(iterator.nextChar())
                } else {
                    break
                }
            }
            ':' -> {
                ret.add(current.joinToString(""))
                current.clear()
            }
            else -> current.add(char)
        }
    }

    ret.add(current.joinToString(""))
    return ret
}

internal fun <L, R> composed(
    leftParser: ValueParser<L>,
    rightParser: ValueParser<R>
) = ValueParser { marker, value ->
    val parts = split(value)

    if (parts.isEmpty() || parts.size >= 3) {
        marker.throwMalformedPropertyValueException("Expected a composed value, but got $value")
    }

    val (leftPart, rightPart) = when (parts.size) {
        1 -> {
            val part = parts[0]
            if (part.startsWith(":")) {
                Composed("", part)
            } else if (part.endsWith(":")) {
                Composed(part, "")
            } else {
                marker.throwMalformedPropertyValueException("Expected a composed value, but got $value")
            }
        }
        2 -> Composed(parts[0], parts[1])
        else -> marker.throwMalformedPropertyValueException("Expected a composed value, but got $value")
    }

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
