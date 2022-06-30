package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException
import com.github.ekenstein.sgf.utils.Either
import com.github.ekenstein.sgf.utils.mapLeft
import com.github.ekenstein.sgf.utils.mapRight
import com.github.ekenstein.sgf.utils.match

internal val pointParser = ValueParser { marker, value ->
    fun fromCharToInt(char: Char): Int = when (char) {
        in 'a'..'z' -> char - 'a' + 1
        in 'A'..'Z' -> char - 'A' + 27
        else -> marker.throwMalformedPropertyValueException("Expected a point, but got $value")
    }

    when (value.length) {
        2 -> SgfPoint(
            x = fromCharToInt(value[0]),
            y = fromCharToInt(value[1])
        )
        else -> marker.throwMalformedPropertyValueException("Expected a point, but got $value")
    }
}

internal val compressedPointParser = ValueParser { marker, value ->
    val point = value.split(":")
    val pointOrRectangle = when (point.size) {
        1 -> Either.Left(pointParser.parse(marker, value))
        2 -> Either.Right(composed(pointParser, pointParser).parse(marker, value))
        else -> marker.throwMalformedPropertyValueException("Expected either a point or a rectangle.")
    }

    pointOrRectangle.mapLeft { setOf(it) }.mapRight { (topLeft, bottomRight) ->
        val rectangle = Rectangle(topLeft.x to topLeft.y, bottomRight.x to bottomRight.y)
        val points = rectangle.points()
        points.map { (x, y) -> SgfPoint(x, y) }.toSet()
    }.match()
}

private data class Rectangle(val topLeft: Pair<Int, Int>, val bottomRight: Pair<Int, Int>) {
    fun points() = (topLeft.first..bottomRight.first).flatMap { x ->
        (topLeft.second..bottomRight.second).map { y ->
            x to y
        }
    }.toSet()
}
