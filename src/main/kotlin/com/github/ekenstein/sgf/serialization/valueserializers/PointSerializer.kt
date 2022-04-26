package com.github.ekenstein.sgf.serialization.valueserializers

import com.github.ekenstein.sgf.SgfPoint

internal fun pointSerializer(point: SgfPoint) = ValueSerializer { appendable ->
    fun intToChar(n: Int) = when {
        n > 26 -> ((n % 27) + 'A'.code).toChar()
        else -> ((n - 1) + 'a'.code).toChar()
    }

    appendable.append(intToChar(point.x))
    appendable.append(intToChar(point.y))
}
