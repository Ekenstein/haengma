package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.GameResult
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException

internal val gameResultParser = ValueParser { marker, value ->
    val parts = value.split("+").map { it.trim() }
    when (parts.size) {
        1 -> {
            when (parts[0].lowercase()) {
                "0" -> GameResult.Draw
                "?" -> GameResult.Unknown
                "void" -> GameResult.Suspended
                else -> marker.throwMalformedPropertyValueException("Expected a game result, but got $value")
            }
        }
        2 -> {
            val color = when (parts[0].lowercase()) {
                "b" -> SgfColor.Black
                "w" -> SgfColor.White
                else -> marker.throwMalformedPropertyValueException(
                    "Expected a winner in the game result, " +
                        "but got $value"
                )
            }

            when (parts[1].lowercase()) {
                in listOf("f", "forfeit") -> GameResult.Forfeit(color)
                in listOf("t", "time") -> GameResult.Time(color)
                in listOf("r", "resign") -> GameResult.Resignation(color)
                in listOf("") -> GameResult.Wins(color)
                else -> {
                    val score = parts[1].toDoubleOrNull()
                        ?: marker.throwMalformedPropertyValueException(
                            "Expected a score in the game result, " +
                                "but got $value"
                        )
                    GameResult.Score(color, score)
                }
            }
        }
        else -> {
            marker.throwMalformedPropertyValueException("Expected a game result, but got $value")
        }
    }
}
