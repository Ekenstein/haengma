package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfPoint
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

private val escapedCharRegex = Regex("""\\([\\:\]])""")
private val whitespaceExceptNewLineRegex = Regex("""[^\S\r\n]""")
private val whitespaceExceptSpaceRegex = Regex("""[^\S ]+""")

internal fun interface ValueParser<T> {
    fun parse(marker: Marker, string: String): T
}

private fun <L, R> composed(
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
            endColumn = marker.endColumn - leftPart.length
        ),
        leftPart
    )

    val right = rightParser.parse(
        marker.copy(
            startColumn = marker.startColumn + leftPart.length
        ),
        rightPart
    )

    left to right
}

internal val colorParser = ValueParser { marker, value ->
    when (value) {
        "W" -> SgfColor.White
        "B" -> SgfColor.Black
        else -> marker.throwParseException("Expected a color, but got $value")
    }
}

internal val doubleParser = ValueParser { marker, value ->
    when (value) {
        "1" -> SgfDouble.Normal
        "2" -> SgfDouble.Emphasized
        else -> marker.throwParseException("Expected a double, but got $value")
    }
}

internal val realParser = ValueParser { marker, value ->
    value.toDoubleOrNull()
        ?: marker.throwParseException("Expected a real value, but got $value")
}

internal val numberParser = ValueParser { marker, value ->
    value.toIntOrNull()
        ?: marker.throwParseException("Expected a number, but got $value")
}

internal val simpleTextParser = ValueParser { _, value ->
    value
        .replace(escapedCharRegex, "$1")
        .replace(whitespaceExceptSpaceRegex, " ")
}
internal val textParser = ValueParser { _, value ->
    value
        .replace(escapedCharRegex, "$1")
        .replace(whitespaceExceptNewLineRegex, " ")
}

internal val pointParser = ValueParser { marker, value ->
    fun fromCharToInt(char: Char): Int = when (char) {
        in 'a'..'z' -> char - 'a' + 1
        in 'A'..'Z' -> char - 'A' + 27
        else -> marker.throwParseException("Expected a point, but got $value")
    }

    when (value.length) {
        2 -> SgfPoint(
            x = fromCharToInt(value[0]),
            y = fromCharToInt(value[1])
        )
        else -> marker.throwParseException("Expected a point, but got $value")
    }
}

internal val moveParser = ValueParser { marker, value ->
    when (value.length) {
        0 -> Move.Pass
        2 -> Move.Stone(pointParser.parse(marker, value))
        else -> marker.throwParseException("Expected a move, but got $value")
    }
}

internal fun <L, R> TerminalNode.asComposed(left: ValueParser<L>, right: ValueParser<R>) = composed(left, right)
    .parse(symbol.toMarker(), textStrippedFromBrackets)

internal fun TerminalNode.asColor(stripBrackets: Boolean) = colorParser.parse(
    symbol.toMarker(),
    if (stripBrackets) textStrippedFromBrackets else text
)

internal fun TerminalNode.asDouble() = doubleParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asNumber() = numberParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asReal() = realParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asSimpleText() = simpleTextParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asText() = textParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asMove() = moveParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asPoint() = pointParser.parse(symbol.toMarker(), textStrippedFromBrackets)

internal val TerminalNode.textStrippedFromBrackets
    get() = stripBrackets(text)

private fun stripBrackets(string: String) = string.substring(1, string.length - 1)

private fun Token.toMarker(): Marker = Marker(
    startLineNumber = line - 1,
    startColumn = charPositionInLine,
    endLineNumber = line - 1,
    endColumn = charPositionInLine + text.length - 1
)

private fun Marker.throwParseException(message: String): Nothing = throw SgfParseException(message, this)
