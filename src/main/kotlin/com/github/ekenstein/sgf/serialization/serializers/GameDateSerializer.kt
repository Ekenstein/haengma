package com.github.ekenstein.sgf.serialization.serializers

import com.github.ekenstein.sgf.GameDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private sealed class PartialDate {
    data class Year(val year: Int) : PartialDate()
    data class Month(val month: Int, val referringTo: YearAndMonth) : PartialDate()
    data class Day(val day: Int, val referringTo: YearAndMonth) : PartialDate()
    data class YearAndMonth(val year: Int, val month: Int) : PartialDate()
    data class MonthAndDay(val month: Int, val day: Int, val referringTo: YearAndMonth) : PartialDate()
    data class Date(val year: Int, val month: Int, val day: Int) : PartialDate()
}

private fun PartialDate.toLocalDate() = when (this) {
    is PartialDate.Date -> LocalDate.of(year, month, day)
    is PartialDate.Day -> LocalDate.of(year, referringTo.month, day)
    is PartialDate.Month -> LocalDate.of(year, month, 1)
    is PartialDate.MonthAndDay -> LocalDate.of(year, month, day)
    is PartialDate.Year -> LocalDate.of(year, 1, 1)
    is PartialDate.YearAndMonth -> LocalDate.of(year, month, 1)
}

private val PartialDate.asString
    get() = when (this) {
        is PartialDate.Date -> toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        is PartialDate.Day -> toLocalDate().format(DateTimeFormatter.ofPattern("dd"))
        is PartialDate.Month -> toLocalDate().format(DateTimeFormatter.ofPattern("MM"))
        is PartialDate.MonthAndDay -> toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd"))
        is PartialDate.Year -> toLocalDate().format(DateTimeFormatter.ofPattern("yyyy"))
        is PartialDate.YearAndMonth -> toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

private val PartialDate.year
    get() = when (this) {
        is PartialDate.Date -> year
        is PartialDate.Day -> referringTo.year
        is PartialDate.Month -> referringTo.year
        is PartialDate.MonthAndDay -> referringTo.year
        is PartialDate.Year -> year
        is PartialDate.YearAndMonth -> year
    }

internal fun gameDateSerializer(dates: List<GameDate>) = ValueSerializer { appendable ->
    val partials = dates.toPartialDates()
    val string = partials.joinToString(",") { it.asString }
    appendable.append(string)
}

private fun List<GameDate>.toPartialDates() = fold(emptyList<PartialDate>()) { partials, date ->
    val last = partials.lastOrNull()
    val next = if (last == null) {
        when (date) {
            is GameDate.Date -> PartialDate.Date(date.year, date.month, date.day)
            is GameDate.Year -> PartialDate.Year(date.year)
            is GameDate.YearAndMonth -> PartialDate.YearAndMonth(date.year, date.month)
        }
    } else {
        when (date) {
            is GameDate.Date -> when (last) {
                is PartialDate.Date -> if (last.year != date.year) {
                    PartialDate.Date(date.year, date.month, date.day)
                } else if (last.month != date.month) {
                    PartialDate.MonthAndDay(date.month, date.day, PartialDate.YearAndMonth(last.year, last.month))
                } else {
                    PartialDate.Day(date.day, PartialDate.YearAndMonth(last.year, last.month))
                }
                is PartialDate.Day -> if (last.year != date.year) {
                    PartialDate.Date(date.year, date.month, date.day)
                } else if (last.referringTo.month != date.month) {
                    PartialDate.MonthAndDay(date.month, date.day, last.referringTo)
                } else {
                    PartialDate.Day(date.day, last.referringTo)
                }
                is PartialDate.Month -> if (last.year != date.year) {
                    PartialDate.Date(date.year, date.month, date.day)
                } else {
                    PartialDate.MonthAndDay(date.month, date.day, PartialDate.YearAndMonth(date.year, date.month))
                }
                is PartialDate.MonthAndDay -> if (last.year != date.year) {
                    PartialDate.Date(date.year, date.month, date.day)
                } else if (last.month != date.month) {
                    PartialDate.MonthAndDay(date.month, date.day, last.referringTo)
                } else {
                    PartialDate.Day(date.day, last.referringTo)
                }
                is PartialDate.Year -> PartialDate.Date(date.year, date.month, date.day)
                is PartialDate.YearAndMonth -> if (last.year != date.year) {
                    PartialDate.Date(date.year, date.month, date.day)
                } else if (last.month != date.month) {
                    PartialDate.MonthAndDay(date.month, date.day, PartialDate.YearAndMonth(date.year, date.month))
                } else {
                    PartialDate.Day(date.day, PartialDate.YearAndMonth(date.year, date.month))
                }
            }
            is GameDate.Year -> PartialDate.Year(date.year)
            is GameDate.YearAndMonth -> when (last) {
                is PartialDate.Year,
                is PartialDate.Date,
                is PartialDate.MonthAndDay,
                is PartialDate.Day -> PartialDate.YearAndMonth(date.year, date.month)
                is PartialDate.Month -> if (last.year != date.year) {
                    PartialDate.YearAndMonth(date.year, date.month)
                } else {
                    PartialDate.Month(date.month, last.referringTo)
                }
                is PartialDate.YearAndMonth -> if (last.year != date.year) {
                    PartialDate.YearAndMonth(date.year, date.month)
                } else {
                    PartialDate.Month(date.month, last)
                }
            }
        }
    }

    partials + next
}

private fun GameDate.Date.toLocalDate() = LocalDate.of(year, month, day)
private fun GameDate.Date.format(pattern: String) = toLocalDate().format(DateTimeFormatter.ofPattern(pattern))
private fun GameDate.YearAndMonth.toLocalDate() = LocalDate.of(year, month, 1)
private fun GameDate.YearAndMonth.format(pattern: String) = toLocalDate().format(DateTimeFormatter.ofPattern(pattern))

private fun GameDate.Year.format() = LocalDate.of(year, 1, 1).format(DateTimeFormatter.ofPattern("yyyy"))

private fun GameDate.defaultFormat() = when (this) {
    is GameDate.Date -> format("yyyy-MM-dd")
    is GameDate.Year -> format()
    is GameDate.YearAndMonth -> format("yyyy-MM")
}
