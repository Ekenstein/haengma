package com.github.ekenstein.sgf.serialization.valueserializers

import com.github.ekenstein.sgf.GameResult
import com.github.ekenstein.sgf.SgfColor
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

private val numberFormatter = DecimalFormat().apply {
    decimalFormatSymbols = DecimalFormatSymbols().apply {
        decimalSeparator = '.'
    }
}

internal fun gameResultSerializer(gameResult: GameResult) = ValueSerializer { appendable ->
    fun asString(color: SgfColor, label: String) = when (color) {
        SgfColor.Black -> "B+$label"
        SgfColor.White -> "W+$label"
    }

    when (gameResult) {
        GameResult.Draw -> appendable.append("0")
        is GameResult.Forfeit -> appendable.append(asString(gameResult.winner, "F"))
        is GameResult.Resignation -> appendable.append(asString(gameResult.winner, "R"))
        is GameResult.Score -> appendable.append(asString(gameResult.winner, numberFormatter.format(gameResult.score)))
        GameResult.Suspended -> appendable.append("Void")
        is GameResult.Time -> appendable.append(asString(gameResult.winner, "T"))
        GameResult.Unknown -> appendable.append("?")
        is GameResult.Wins -> appendable.append(asString(gameResult.winner, ""))
    }
}
