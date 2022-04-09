package com.github.ekenstein.sgf.serialization

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import java.text.DecimalFormat
import java.text.NumberFormat

internal fun SgfCollection.serialize(appendable: Appendable) {
    trees.forEach { it.serialize(appendable) }
}

private fun SgfGameTree.serialize(appendable: Appendable) {
    appendable.append('(')
    sequence.forEach { it.serialize(appendable) }
    trees.forEach { it.serialize(appendable) }
    appendable.append(')')
}

private fun SgfNode.serialize(appendable: Appendable) {
    appendable.append(';')
    properties.forEach { it.serialize(appendable) }
}

private fun SgfProperty.serialize(appendable: Appendable) {
    appendable.append(identifier)
    valueSerializer.serialize(appendable)
}

private fun interface SgfSerializer {
    fun serialize(appendable: Appendable)
}

private fun valueSerializer(serializer: SgfSerializer) = valueSerializer(listOf(serializer))
private fun valueSerializer(serializers: List<SgfSerializer>) = SgfSerializer { appendable ->
    if (serializers.isEmpty()) {
        appendable.append("[]")
    } else {
        serializers.forEach {
            appendable.append('[')
            it.serialize(appendable)
            appendable.append(']')
        }
    }
}

private fun colorSerializer(color: SgfColor) = SgfSerializer { appendable ->
    when (color) {
        SgfColor.Black -> appendable.append('B')
        SgfColor.White -> appendable.append('W')
    }
}

private fun doubleSerializer(double: SgfDouble) = SgfSerializer { appendable ->
    when (double) {
        SgfDouble.Normal -> appendable.append('1')
        SgfDouble.Emphasized -> appendable.append('2')
    }
}

private fun pointSerializer(point: SgfPoint) = SgfSerializer { appendable ->
    fun intToChar(n: Int) = when {
        n > 26 -> ((n % 27) + 'A'.code).toChar()
        else -> ((n - 1) + 'a'.code).toChar()
    }

    appendable.append(intToChar(point.x))
    appendable.append(intToChar(point.y))
}

private fun moveSerializer(move: Move) = SgfSerializer { appendable ->
    when (move) {
        Move.Pass -> Unit
        is Move.Stone -> pointSerializer(move.point).serialize(appendable)
    }
}

private fun numberFormatter(serializeSign: Boolean): NumberFormat = DecimalFormat().apply {
    when (serializeSign) {
        true -> {
            negativePrefix = "-"
            positivePrefix = "+"
        }
        false -> {
            negativePrefix = ""
            positivePrefix = ""
        }
    }
}

private fun numberSerializer(number: Number, serializeSign: Boolean = false) = SgfSerializer { appendable ->
    appendable.append(numberFormatter(serializeSign).format(number))
}

private val whitespaceExceptNewLineRegex = Regex("""[^\S\r\n]""")
private val whitespaceExceptSpaceRegex = Regex("""[^\S ]+""")
private fun escapeRegex(isComposed: Boolean): Regex {
    val needsEscapingIfComposed = listOf(':').filter { isComposed }
    val needsEscaping = listOf("\\]", "\\\\") + needsEscapingIfComposed

    val escapeChars = needsEscaping.joinToString("")

    return Regex("""[$escapeChars]""")
}

private fun simpleTextSerializer(string: String, isComposed: Boolean) = SgfSerializer { appendable ->
    val serialized = string.replace(whitespaceExceptSpaceRegex, " ")
        .replace(escapeRegex(isComposed), "\\$0")

    appendable.append(serialized)
}

private fun textSerializer(string: String, isComposed: Boolean) = SgfSerializer { appendable ->
    val serialized = string.replace(whitespaceExceptNewLineRegex, " ")
        .replace(escapeRegex(isComposed), "\\$0")

    appendable.append(serialized)
}

private fun composedSerializer(left: SgfSerializer, right: SgfSerializer) = SgfSerializer { appendable ->
    left.serialize(appendable)
    appendable.append(':')
    right.serialize(appendable)
}

private val SgfProperty.identifier: String
    get() = when (this) {
        is SgfProperty.GameInfo.AN -> "AN"
        is SgfProperty.GameInfo.BR -> "BR"
        is SgfProperty.GameInfo.BT -> "BT"
        is SgfProperty.GameInfo.CP -> "CP"
        is SgfProperty.GameInfo.DT -> "DT"
        is SgfProperty.GameInfo.EV -> "EV"
        is SgfProperty.GameInfo.GC -> "GC"
        is SgfProperty.GameInfo.GN -> "GN"
        is SgfProperty.GameInfo.HA -> "HA"
        is SgfProperty.GameInfo.KM -> "KM"
        is SgfProperty.GameInfo.ON -> "ON"
        is SgfProperty.GameInfo.OT -> "OT"
        is SgfProperty.GameInfo.PB -> "PB"
        is SgfProperty.GameInfo.PC -> "PC"
        is SgfProperty.GameInfo.PW -> "PW"
        is SgfProperty.GameInfo.RE -> "RE"
        is SgfProperty.GameInfo.RO -> "RO"
        is SgfProperty.GameInfo.RU -> "RU"
        is SgfProperty.GameInfo.SO -> "SO"
        is SgfProperty.GameInfo.TM -> "TM"
        is SgfProperty.GameInfo.US -> "US"
        is SgfProperty.GameInfo.WR -> "WR"
        is SgfProperty.GameInfo.WT -> "WT"
        is SgfProperty.Markup.AR -> "AR"
        is SgfProperty.Markup.CR -> "CR"
        is SgfProperty.Markup.DD -> "DD"
        is SgfProperty.Markup.LB -> "LB"
        is SgfProperty.Markup.LN -> "LN"
        is SgfProperty.Markup.MA -> "MA"
        is SgfProperty.Markup.SL -> "SL"
        is SgfProperty.Markup.SQ -> "SQ"
        is SgfProperty.Markup.TR -> "TR"
        is SgfProperty.Misc.FG -> "FG"
        is SgfProperty.Misc.PM -> "PM"
        is SgfProperty.Misc.VW -> "VW"
        is SgfProperty.Move.B -> "B"
        SgfProperty.Move.KO -> "KO"
        is SgfProperty.Move.MN -> "MN"
        is SgfProperty.Move.W -> "W"
        is SgfProperty.MoveAnnotation.BM -> "BM"
        SgfProperty.MoveAnnotation.DO -> "DO"
        SgfProperty.MoveAnnotation.IT -> "IT"
        is SgfProperty.MoveAnnotation.TE -> "TE"
        is SgfProperty.NodeAnnotation.C -> "C"
        is SgfProperty.NodeAnnotation.DM -> "DM"
        is SgfProperty.NodeAnnotation.GB -> "GB"
        is SgfProperty.NodeAnnotation.GW -> "GW"
        is SgfProperty.NodeAnnotation.HO -> "HO"
        is SgfProperty.NodeAnnotation.N -> "N"
        is SgfProperty.NodeAnnotation.UC -> "UC"
        is SgfProperty.NodeAnnotation.V -> "V"
        is SgfProperty.Private -> identifier
        is SgfProperty.Root.AP -> "AP"
        is SgfProperty.Root.CA -> "CA"
        is SgfProperty.Root.FF -> "FF"
        is SgfProperty.Root.GM -> "GM"
        is SgfProperty.Root.ST -> "ST"
        is SgfProperty.Root.SZ -> "SZ"
        is SgfProperty.Setup.AB -> "AB"
        is SgfProperty.Setup.AE -> "AE"
        is SgfProperty.Setup.AW -> "AW"
        is SgfProperty.Setup.PL -> "PL"
        is SgfProperty.Timing.BL -> "BL"
        is SgfProperty.Timing.OB -> "OB"
        is SgfProperty.Timing.OW -> "OW"
        is SgfProperty.Timing.WL -> "WL"
    }

private val noneSerializer: SgfSerializer = SgfSerializer { }

private val SgfProperty.valueSerializer: SgfSerializer
    get() = when (this) {
        is SgfProperty.GameInfo.AN -> valueSerializer(simpleTextSerializer(annotation, false))
        is SgfProperty.GameInfo.BR -> valueSerializer(simpleTextSerializer(rank, false))
        is SgfProperty.GameInfo.BT -> valueSerializer(simpleTextSerializer(team, false))
        is SgfProperty.GameInfo.CP -> valueSerializer(simpleTextSerializer(copyright, false))
        is SgfProperty.GameInfo.DT -> valueSerializer(simpleTextSerializer(date, false))
        is SgfProperty.GameInfo.EV -> valueSerializer(simpleTextSerializer(event, false))
        is SgfProperty.GameInfo.GC -> valueSerializer(textSerializer(comment, false))
        is SgfProperty.GameInfo.GN -> valueSerializer(simpleTextSerializer(name, false))
        is SgfProperty.GameInfo.HA -> valueSerializer(numberSerializer(numberOfStones))
        is SgfProperty.GameInfo.KM -> valueSerializer(numberSerializer(komi))
        is SgfProperty.GameInfo.ON -> valueSerializer(simpleTextSerializer(opening, false))
        is SgfProperty.GameInfo.OT -> valueSerializer(simpleTextSerializer(overtime, false))
        is SgfProperty.GameInfo.PB -> valueSerializer(simpleTextSerializer(name, false))
        is SgfProperty.GameInfo.PC -> valueSerializer(simpleTextSerializer(place, false))
        is SgfProperty.GameInfo.PW -> valueSerializer(simpleTextSerializer(name, false))
        is SgfProperty.GameInfo.RE -> valueSerializer(simpleTextSerializer(result, false))
        is SgfProperty.GameInfo.RO -> valueSerializer(simpleTextSerializer(round, false))
        is SgfProperty.GameInfo.RU -> valueSerializer(simpleTextSerializer(rules, false))
        is SgfProperty.GameInfo.SO -> valueSerializer(simpleTextSerializer(source, false))
        is SgfProperty.GameInfo.TM -> valueSerializer(numberSerializer(timeLimit))
        is SgfProperty.GameInfo.US -> valueSerializer(simpleTextSerializer(user, false))
        is SgfProperty.GameInfo.WR -> valueSerializer(simpleTextSerializer(rank, false))
        is SgfProperty.GameInfo.WT -> valueSerializer(simpleTextSerializer(team, false))
        is SgfProperty.Markup.AR -> valueSerializer(
            points.map { (p1, p2) -> composedSerializer(pointSerializer(p1), pointSerializer(p2)) }
        )
        is SgfProperty.Markup.CR -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Markup.DD -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Markup.LB -> valueSerializer(
            label.map { (point, label) ->
                composedSerializer(pointSerializer(point), simpleTextSerializer(label, true))
            }
        )
        is SgfProperty.Markup.LN -> valueSerializer(
            line.map { (p1, p2) ->
                composedSerializer(pointSerializer(p1), pointSerializer(p2))
            }
        )
        is SgfProperty.Markup.MA -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Markup.SL -> valueSerializer(selected.map { pointSerializer(it) })
        is SgfProperty.Markup.SQ -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Markup.TR -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Misc.FG -> valueSerializer(
            when (value) {
                null -> noneSerializer
                else -> {
                    val (flag, name) = value
                    composedSerializer(numberSerializer(flag), simpleTextSerializer(name, true))
                }
            }
        )
        is SgfProperty.Misc.PM -> valueSerializer(numberSerializer(printMoveMode))
        is SgfProperty.Misc.VW -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Move.B -> valueSerializer(moveSerializer(move))
        is SgfProperty.Move.MN -> valueSerializer(numberSerializer(number))
        is SgfProperty.Move.W -> valueSerializer(moveSerializer(move))
        is SgfProperty.MoveAnnotation.BM -> valueSerializer(doubleSerializer(value))
        is SgfProperty.MoveAnnotation.TE -> valueSerializer(doubleSerializer(value))
        is SgfProperty.NodeAnnotation.C -> valueSerializer(textSerializer(comment, false))
        is SgfProperty.NodeAnnotation.DM -> valueSerializer(doubleSerializer(value))
        is SgfProperty.NodeAnnotation.GB -> valueSerializer(doubleSerializer(value))
        is SgfProperty.NodeAnnotation.GW -> valueSerializer(doubleSerializer(value))
        is SgfProperty.NodeAnnotation.HO -> valueSerializer(doubleSerializer(value))
        is SgfProperty.NodeAnnotation.N -> valueSerializer(textSerializer(name, false))
        is SgfProperty.NodeAnnotation.UC -> valueSerializer(doubleSerializer(value))
        is SgfProperty.NodeAnnotation.V -> valueSerializer(numberSerializer(value))
        is SgfProperty.Private -> valueSerializer { appendable -> values.forEach { appendable.append(it) } }
        is SgfProperty.Root.AP -> valueSerializer(
            composedSerializer(simpleTextSerializer(name, true), simpleTextSerializer(version, true))
        )
        is SgfProperty.Root.CA -> valueSerializer(simpleTextSerializer(charset, false))
        is SgfProperty.Root.FF -> valueSerializer(numberSerializer(format))
        is SgfProperty.Root.GM -> valueSerializer(numberSerializer(game))
        is SgfProperty.Root.ST -> valueSerializer(numberSerializer(style))
        is SgfProperty.Root.SZ -> {
            val serializer = if (width == height) {
                numberSerializer(width)
            } else {
                composedSerializer(numberSerializer(width), numberSerializer(height))
            }

            valueSerializer(serializer)
        }
        is SgfProperty.Setup.AB -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Setup.AE -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Setup.AW -> valueSerializer(points.map { pointSerializer(it) })
        is SgfProperty.Setup.PL -> valueSerializer(colorSerializer(color))
        is SgfProperty.Timing.BL -> valueSerializer(numberSerializer(timeLeft))
        is SgfProperty.Timing.OB -> valueSerializer(numberSerializer(overtimeStones))
        is SgfProperty.Timing.OW -> valueSerializer(numberSerializer(overtimeStones))
        is SgfProperty.Timing.WL -> valueSerializer(numberSerializer(timeLeft))
        SgfProperty.Move.KO,
        SgfProperty.MoveAnnotation.DO,
        SgfProperty.MoveAnnotation.IT -> valueSerializer(noneSerializer)
    }
