package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException

internal val gameTypeParser = ValueParser { marker, value ->
    val allGameTypes = GameType.values().associateBy { it.value }
    val max = allGameTypes.maxOf { it.key }
    val min = allGameTypes.minOf { it.key }
    val numberParser = numberParser(IntRange(min, max))

    val number = numberParser.parse(marker, value)
    try {
        allGameTypes.getValue(number)
    } catch (ex: NoSuchElementException) {
        marker.throwMalformedPropertyValueException("Couldn't recognize the game type $number", ex)
    }
}
