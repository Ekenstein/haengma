package com.github.ekenstein.sgf.parser.valueparsers

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.parser.Marker
import com.github.ekenstein.sgf.parser.throwMalformedPropertyValueException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private sealed class PartialDate {
    data class Year(val year: Int) : PartialDate()
    data class Month(val month: Int) : PartialDate()
    data class Day(val day: Int) : PartialDate()
    data class YearAndMonth(val year: Int, val month: Int) : PartialDate()
    data class MonthAndDay(val month: Int, val day: Int) : PartialDate()
    data class Date(val date: LocalDate) : PartialDate()
}

internal val dateParser = ValueParser { marker, value ->
    val partialDates = value.split(",").fold(emptyList<PartialDate>()) { parsedDates, string ->
        parsedDates + toPartialDate(parsedDates, string.trim(), marker)
    }

    partialDates.fold(emptyList<GameDate>()) { dates, partialDate ->
        dates + when (partialDate) {
            is PartialDate.Date -> GameDate.Date(partialDate.date)
            is PartialDate.Day -> when (val lastDate = dates.last()) {
                is GameDate.Date -> GameDate.of(lastDate.year, lastDate.month, partialDate.day)
                is GameDate.Year,
                is GameDate.YearAndMonth -> error("Expected a date, but got $lastDate")
            }
            is PartialDate.Month -> when (val lastDate = dates.last()) {
                is GameDate.YearAndMonth -> GameDate.of(lastDate.year, partialDate.month)
                is GameDate.Date,
                is GameDate.Year -> error("Expected a year and month, but got $lastDate")
            }
            is PartialDate.MonthAndDay -> when (val lastDate = dates.last()) {
                is GameDate.Date -> GameDate.of(lastDate.year, partialDate.month, partialDate.day)
                is GameDate.Year -> GameDate.of(lastDate.year, partialDate.month, partialDate.day)
                is GameDate.YearAndMonth -> GameDate.of(lastDate.year, partialDate.month, partialDate.day)
            }
            is PartialDate.Year -> GameDate.of(partialDate.year)
            is PartialDate.YearAndMonth -> GameDate.of(partialDate.year, partialDate.month)
        }
    }
}

/**
 * "MM-DD" - if preceded by YYYY-MM-DD, YYYY-MM, MM-DD, MM or DD
 * "MM" - if preceded by YYYY-MM or MM
 * "DD" - if preceded by YYYY-MM-DD, MM-DD or DD
 */
private fun toPartialDate(parsedDates: List<PartialDate>, string: String, marker: Marker): PartialDate =
    when (string.length) {
        2 -> {
            val dayOrMonth = string.toIntOrNull()
                ?: marker.throwMalformedPropertyValueException("Expected a day or month, but got $string")

            val lastParsedDate = parsedDates.lastOrNull()
                ?: marker.throwMalformedPropertyValueException(
                    "A day or a month must be preceded by one of YYYY-MM-DD, YYYY-MM, MM-DD, MM or DD"
                )

            val parsedDate = when (lastParsedDate) {
                is PartialDate.Date,
                is PartialDate.Day,
                is PartialDate.MonthAndDay -> PartialDate.Day(dayOrMonth)
                is PartialDate.YearAndMonth,
                is PartialDate.Month -> PartialDate.Month(dayOrMonth)
                is PartialDate.Year -> marker.throwMalformedPropertyValueException(
                    "A day or a month must be preceded by one of YYYY-MM-DD, YYYY-MM, MM-DD, MM or DD, " +
                        "but last parsed date was YYYY: '${lastParsedDate.year}'"
                )
            }

            parsedDate
        }
        4 -> {
            val year = string.toIntOrNull()
                ?: marker.throwMalformedPropertyValueException("Expected a year, but got $string")
            PartialDate.Year(year)
        }
        5 -> {
            val lastDate = parsedDates.lastOrNull()
                ?: marker.throwMalformedPropertyValueException(
                    "Expected YYYY-MM-DD, YYYY-MM, MM-DD, MM or DD " +
                        "but got $string"
                )

            val partials = string.split("-").takeIf { it.size == 2 }
                ?: marker.throwMalformedPropertyValueException("Expected MM-DD but got $string")

            val (sMonth, sDay) = partials
            val month = sMonth.toIntOrNull()
                ?: marker.throwMalformedPropertyValueException("Expected a month, but got $sMonth")
            val day = sDay.toIntOrNull()
                ?: marker.throwMalformedPropertyValueException("Expected a day, but got $sDay")

            when (lastDate) {
                is PartialDate.Year -> marker.throwMalformedPropertyValueException(
                    "MM-DD must be preceded by YYYY-MM-DD, YYYY-MM, MM-DD, MM or DD"
                )
                else -> Unit
            }

            PartialDate.MonthAndDay(month, day)
        }
        7 -> {
            val partials = string.split("-").takeIf { it.size == 2 }
                ?: marker.throwMalformedPropertyValueException("Expected YYYY-MM but got $string")

            val (sYear, sMonth) = partials
            val year = sYear.toIntOrNull()
                ?: marker.throwMalformedPropertyValueException("Expected a year, but got $sYear")
            val month = sMonth.toIntOrNull()
                ?: marker.throwMalformedPropertyValueException("Expected a month, but got $sMonth")

            PartialDate.YearAndMonth(year, month)
        }
        else -> try {
            val date = LocalDate.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            PartialDate.Date(date)
        } catch (ex: DateTimeParseException) {
            marker.throwMalformedPropertyValueException("Expected YYYY-MM-DD but got $string")
        }
    }
