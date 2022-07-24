package com.github.ekenstein.sgf.serialization.valueserializers

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

private val numberFormatter: NumberFormat = DecimalFormat().apply {
    decimalFormatSymbols = DecimalFormatSymbols().apply {
        decimalSeparator = '.'
    }
    isGroupingUsed = false
    roundingMode = RoundingMode.FLOOR
    maximumFractionDigits = 340
}

internal fun numberSerializer(number: Number) = ValueSerializer { appendable ->
    appendable.append(numberFormatter.format(number))
}
