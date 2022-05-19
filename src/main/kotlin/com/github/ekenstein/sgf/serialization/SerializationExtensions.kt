package com.github.ekenstein.sgf.serialization

import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.serialization.valueserializers.ValueSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.colorSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.composedSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.doubleSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.gameDateSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.gameResultSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.moveSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.noneSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.numberSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.pointSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.simpleTextSerializer
import com.github.ekenstein.sgf.serialization.valueserializers.textSerializer
import com.github.ekenstein.sgf.utils.nelOf
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream

fun SgfCollection.encode(outputStream: OutputStream) {
    val printStream = PrintStream(outputStream)
    encode(printStream as Appendable)
}

fun SgfGameTree.encode(outputStream: OutputStream) = SgfCollection(nelOf(this)).encode(outputStream)

fun SgfCollection.encodeToString(): String = ByteArrayOutputStream().use {
    encode(it)
    String(it.toByteArray())
}

fun SgfGameTree.encodeToString(): String = SgfCollection(nelOf(this)).encodeToString()

fun SgfCollection.encode(appendable: Appendable) {
    trees.forEach { it.encode(appendable) }
}

private fun SgfGameTree.encode(appendable: Appendable) {
    appendable.append('(')
    sequence.forEach { it.encode(appendable) }
    trees.forEach { it.encode(appendable) }
    appendable.append(')')
}

private fun SgfNode.encode(appendable: Appendable) {
    appendable.append(';')
    properties.forEach { it.encode(appendable) }
}

private fun SgfProperty.encode(appendable: Appendable) {
    appendable.append(identifier)
    valueSerializer.serialize(appendable)
}

private fun valueSerializer(serializer: ValueSerializer) = valueSerializer(listOf(serializer))
private fun valueSerializer(serializers: List<ValueSerializer>) = ValueSerializer { appendable ->
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

private val SgfProperty.valueSerializer: ValueSerializer
    get() = when (this) {
        is SgfProperty.GameInfo.AN -> valueSerializer(simpleTextSerializer(annotation, false))
        is SgfProperty.GameInfo.BR -> valueSerializer(simpleTextSerializer(rank, false))
        is SgfProperty.GameInfo.BT -> valueSerializer(simpleTextSerializer(team, false))
        is SgfProperty.GameInfo.CP -> valueSerializer(simpleTextSerializer(copyright, false))
        is SgfProperty.GameInfo.DT -> valueSerializer(gameDateSerializer(dates))
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
        is SgfProperty.GameInfo.RE -> valueSerializer(gameResultSerializer(result))
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
        is SgfProperty.Root.CA -> valueSerializer(simpleTextSerializer(charset.displayName(), false))
        is SgfProperty.Root.FF -> valueSerializer(numberSerializer(format))
        is SgfProperty.Root.GM -> valueSerializer(numberSerializer(game.value))
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
