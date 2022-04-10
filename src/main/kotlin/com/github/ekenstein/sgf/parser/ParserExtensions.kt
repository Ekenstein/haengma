package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

private fun <T> withParseExceptionHandler(isLenient: Boolean, block: () -> T): T? = try {
    block()
} catch (ex: SgfParseException) {
    if (isLenient) {
        null
    } else {
        throw ex
    }
}

internal fun SgfParser.parse(lenient: Boolean): SgfCollection {
    val collection = collection()
    val gameTrees = collection.gameTree().map { it.extract(lenient) }
    return SgfCollection(gameTrees)
}

private fun SgfParser.GameTreeContext.extract(lenient: Boolean): SgfGameTree {
    val sequence = sequence().extract(lenient)
    val variations = gameTree().map { it.extract(lenient) }

    return SgfGameTree(sequence, variations)
}

private fun SgfParser.SequenceContext.extract(lenient: Boolean) = node().map { it.extract(lenient) }

private fun SgfParser.NodeContext.extract(lenient: Boolean): SgfNode {
    val props = prop().mapNotNull { it.extract(lenient) }.toSet()
    return SgfNode(props)
}

private fun SgfParser.PropContext.extract(lenient: Boolean) = withParseExceptionHandler(lenient) {
    when {
        move() != null -> move().extract()
        markup() != null -> markup().extract()
        misc() != null -> misc().extract()
        gameInfo() != null -> gameInfo().extract()
        root() != null -> root().extract()
        moveAnnotation() != null -> moveAnnotation().extract()
        nodeAnnotation() != null -> nodeAnnotation().extract()
        setup() != null -> setup().extract()
        timing() != null -> timing().extract()
        privateProp() != null -> privateProp().extract()
        else -> throw SgfParseException("Unrecognized property", toMarker())
    }
}

private fun SgfParser.PrivatePropContext.extract(): SgfProperty = SgfProperty.Private(
    identifier = PROP_IDENTIFIER().text,
    values = VALUE()?.map { it.textStrippedFromBrackets } ?: emptyList()
)

private fun SgfParser.MoveContext.extract(): SgfProperty.Move = when (this) {
    is SgfParser.BlackMoveContext -> SgfProperty.Move.B(
        NONE()?.let { Move.Pass } ?: VALUE().asMove()
    )
    is SgfParser.WhiteMoveContext -> SgfProperty.Move.W(
        NONE()?.let { Move.Pass } ?: VALUE().asMove()
    )
    is SgfParser.KoContext -> SgfProperty.Move.KO
    is SgfParser.MoveNumberContext -> SgfProperty.Move.MN(VALUE().asNumber())
    else -> throw SgfParseException("Unrecognized move property $text", toMarker())
}

private fun SgfParser.SetupContext.extract(): SgfProperty.Setup = when (this) {
    is SgfParser.AddEmptyContext -> SgfProperty.Setup.AE(VALUE().map { it.asPoint() }.toSet())
    is SgfParser.AddBlackContext -> SgfProperty.Setup.AB(VALUE().map { it.asPoint() }.toSet())
    is SgfParser.AddWhiteContext -> SgfProperty.Setup.AW(VALUE().map { it.asPoint() }.toSet())
    is SgfParser.PlayerToPlayContext -> SgfProperty.Setup.PL(VALUE().asColor(true))
    else -> throw SgfParseException("Unrecognized setup property $text", toMarker())
}

private fun SgfParser.NodeAnnotationContext.extract(): SgfProperty.NodeAnnotation = when (this) {
    is SgfParser.CommentContext -> SgfProperty.NodeAnnotation.C(VALUE().asText())
    is SgfParser.EvenPositionContext -> SgfProperty.NodeAnnotation.DM(VALUE().asDouble())
    is SgfParser.GoodForBlackContext -> SgfProperty.NodeAnnotation.GB(VALUE().asDouble())
    is SgfParser.GoodForWhiteContext -> SgfProperty.NodeAnnotation.GW(VALUE().asDouble())
    is SgfParser.HotspotContext -> SgfProperty.NodeAnnotation.HO(VALUE().asDouble())
    is SgfParser.NodeNameContext -> SgfProperty.NodeAnnotation.N(VALUE().asSimpleText())
    is SgfParser.UnclearPositionContext -> SgfProperty.NodeAnnotation.UC(VALUE().asDouble())
    is SgfParser.ValueContext -> SgfProperty.NodeAnnotation.V(VALUE().asReal())
    else -> throw SgfParseException("Unrecognized node annotation property $text", toMarker())
}

private fun SgfParser.MoveAnnotationContext.extract(): SgfProperty.MoveAnnotation =
    when (this) {
        is SgfParser.BadMoveContext -> SgfProperty.MoveAnnotation.BM(VALUE().asDouble())
        is SgfParser.DoubtfulContext -> SgfProperty.MoveAnnotation.DO
        is SgfParser.InterestingContext -> SgfProperty.MoveAnnotation.IT
        is SgfParser.TesujiContext -> SgfProperty.MoveAnnotation.TE(VALUE().asDouble())
        else -> throw SgfParseException("Unrecognized move annotation property $text", toMarker())
    }

private fun SgfParser.MarkupContext.extract(): SgfProperty.Markup = when (this) {
    is SgfParser.CircleContext -> SgfProperty.Markup.CR(VALUE().map { it.asPoint() })
    is SgfParser.ArrowContext -> SgfProperty.Markup.AR(VALUE().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.LabelContext -> SgfProperty.Markup.LB(VALUE().map { it.asComposed(pointParser, simpleTextParser) })
    is SgfParser.LineContext -> SgfProperty.Markup.LN(VALUE().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.MarkContext -> SgfProperty.Markup.MA(VALUE().map { it.asPoint() })
    is SgfParser.SelectedContext -> SgfProperty.Markup.SL(VALUE().map { it.asPoint() })
    is SgfParser.SquareContext -> SgfProperty.Markup.SQ(VALUE().map { it.asPoint() })
    is SgfParser.TriangleContext -> SgfProperty.Markup.TR(VALUE().map { it.asPoint() })
    is SgfParser.DimPointsContext -> SgfProperty.Markup.DD(VALUE()?.map { it.asPoint() } ?: emptyList())
    else -> throw SgfParseException("Unrecognized markup property $text", toMarker())
}

private fun SgfParser.RootContext.extract(): SgfProperty.Root = when (this) {
    is SgfParser.ApplicationContext -> {
        val (name, version) = VALUE().asComposed(simpleTextParser, simpleTextParser)
        SgfProperty.Root.AP(name, version)
    }
    is SgfParser.FileFormatContext -> SgfProperty.Root.FF(VALUE().asNumber(1..4))
    is SgfParser.GameContext -> SgfProperty.Root.GM(VALUE().asNumber(1..16))
    is SgfParser.SizeContext -> {
        val raw = VALUE()
        val numberParser = numberParser(1..52)
        val marker = raw.symbol.toMarker()

        val (width, height) = try {
            val number = numberParser.parse(marker, raw.textStrippedFromBrackets)
            number to number
        } catch (ex: SgfParseException) {
            try {
                composed(numberParser, numberParser).parse(marker, raw.textStrippedFromBrackets)
            } catch (ex: SgfParseException) {
                marker.throwParseException("Expected a number or a composed value containing numbers, but got $raw")
            }
        }
        SgfProperty.Root.SZ(width, height)
    }
    is SgfParser.CharsetContext -> SgfProperty.Root.CA(VALUE().asSimpleText())
    is SgfParser.StyleContext -> SgfProperty.Root.ST(VALUE().asNumber(0..3))
    else -> throw SgfParseException("Unrecognized root property $text", toMarker())
}

private fun SgfParser.GameInfoContext.extract(): SgfProperty.GameInfo = when (this) {
    is SgfParser.HandicapContext -> SgfProperty.GameInfo.HA(VALUE().asNumber())
    is SgfParser.KomiContext -> SgfProperty.GameInfo.KM(VALUE().asReal())
    is SgfParser.EventContext -> SgfProperty.GameInfo.EV(VALUE().asSimpleText())
    is SgfParser.PlayerBlackContext -> SgfProperty.GameInfo.PB(VALUE().asSimpleText())
    is SgfParser.PlayerWhiteContext -> SgfProperty.GameInfo.PW(VALUE().asSimpleText())
    is SgfParser.WhiteRankContext -> SgfProperty.GameInfo.WR(VALUE().asSimpleText())
    is SgfParser.BlackRankContext -> SgfProperty.GameInfo.BR(VALUE().asSimpleText())
    is SgfParser.DateContext -> SgfProperty.GameInfo.DT(VALUE().asSimpleText())
    is SgfParser.ResultContext -> SgfProperty.GameInfo.RE(VALUE().asSimpleText())
    is SgfParser.TimeLimitContext -> SgfProperty.GameInfo.TM(VALUE().asReal())
    is SgfParser.SourceContext -> SgfProperty.GameInfo.SO(VALUE().asSimpleText())
    is SgfParser.GameNameContext -> SgfProperty.GameInfo.GN(VALUE().asSimpleText())
    is SgfParser.GameCommentContext -> SgfProperty.GameInfo.GC(VALUE().asText())
    is SgfParser.OpeningContext -> SgfProperty.GameInfo.ON(VALUE().asSimpleText())
    is SgfParser.OvertimeContext -> SgfProperty.GameInfo.OT(VALUE().asSimpleText())
    is SgfParser.RoundContext -> SgfProperty.GameInfo.RO(VALUE().asSimpleText())
    is SgfParser.RulesContext -> SgfProperty.GameInfo.RU(VALUE().asSimpleText())
    is SgfParser.UserContext -> SgfProperty.GameInfo.US(VALUE().asSimpleText())
    is SgfParser.WhiteTeamContext -> SgfProperty.GameInfo.WT(VALUE().asSimpleText())
    is SgfParser.BlackTeamContext -> SgfProperty.GameInfo.BT(VALUE().asSimpleText())
    is SgfParser.AnnotationContext -> SgfProperty.GameInfo.AN(VALUE().asSimpleText())
    is SgfParser.CopyrightContext -> SgfProperty.GameInfo.CP(VALUE().asSimpleText())
    is SgfParser.PlaceContext -> SgfProperty.GameInfo.PC(VALUE().asSimpleText())
    else -> throw SgfParseException("Unrecognized game info property $text", toMarker())
}

private fun SgfParser.TimingContext.extract(): SgfProperty.Timing = when (this) {
    is SgfParser.BlackTimeLeftContext -> SgfProperty.Timing.BL(VALUE().asReal())
    is SgfParser.WhiteTimeLeftContext -> SgfProperty.Timing.WL(VALUE().asReal())
    is SgfParser.OtStonesBlackContext -> SgfProperty.Timing.OB(VALUE().asNumber())
    is SgfParser.OtStonesWhiteContext -> SgfProperty.Timing.OW(VALUE().asNumber())
    else -> throw SgfParseException("Unrecognized timing property $text", toMarker())
}

private fun SgfParser.MiscContext.extract(): SgfProperty.Misc = when (this) {
    is SgfParser.FigureContext -> SgfProperty.Misc.FG(VALUE()?.asComposed(numberParser(), simpleTextParser))
    is SgfParser.PrintMoveModeContext -> SgfProperty.Misc.PM(VALUE().asNumber())
    is SgfParser.ViewContext -> SgfProperty.Misc.VW(VALUE().map { it.asPoint() })
    else -> throw SgfParseException("Unrecognized misc property $text", toMarker())
}

private fun ParserRuleContext.toMarker(): Marker {
    val start = getStart()
    val stop = getStop()
    return Marker(
        startLineNumber = start.line - 1,
        startColumn = start.charPositionInLine,
        endLineNumber = stop.line - 1,
        endColumn = stop.charPositionInLine + stop.text.length - 1
    )
}

private val escapedCharRegex = Regex("""\\([\\:\]])""")
private val whitespaceExceptNewLineRegex = Regex("""[^\S\r\n]""")
private val whitespaceExceptSpaceRegex = Regex("""[^\S ]+""")

internal fun interface ValueParser<T> {
    fun parse(marker: Marker, string: String): T
}

private fun <L, R> composed(
    leftParser: ValueParser<L>,
    rightParser: ValueParser<R>
) = ValueParser { marker, value ->
    val regex = Regex("""(?<=[^\\]):""")
    val parts = value.split(regex)

    if (parts.size != 2) {
        marker.throwParseException("Expected a composed value, but got $value")
    }

    val (leftPart, rightPart) = parts

    val left = leftParser.parse(
        marker.copy(
            endColumn = marker.endColumn - leftPart.length - 1
        ),
        leftPart
    )

    val right = rightParser.parse(
        marker.copy(
            startColumn = marker.startColumn + leftPart.length + 1,
            endColumn = marker.endColumn - 1
        ),
        rightPart
    )

    left to right
}

internal val colorParser = ValueParser { marker, value ->
    when (value) {
        "W" -> SgfColor.White
        "B" -> SgfColor.Black
        else -> marker.throwParseException("Expected a color, but got $value")
    }
}

internal val doubleParser = ValueParser { marker, value ->
    when (value) {
        "1" -> SgfDouble.Normal
        "2" -> SgfDouble.Emphasized
        else -> marker.throwParseException("Expected a double, but got $value")
    }
}

internal val realParser = ValueParser { marker, value ->
    value.toDoubleOrNull()
        ?: marker.throwParseException("Expected a real value, but got $value")
}

internal fun numberParser(range: IntRange? = null) = ValueParser { marker, value ->
    val number = value.toIntOrNull()
        ?: marker.throwParseException("Expected a number, but got $value")

    if (range != null && number !in range) {
        marker.throwParseException("The number $number must be within the range ${range.first} - ${range.last}")
    }

    number
}

internal val simpleTextParser = ValueParser { _, value ->
    value
        .replace(escapedCharRegex, "$1")
        .replace(whitespaceExceptSpaceRegex, " ")
}
internal val textParser = ValueParser { _, value ->
    value
        .replace(escapedCharRegex, "$1")
        .replace(whitespaceExceptNewLineRegex, " ")
}

internal val pointParser = ValueParser { marker, value ->
    fun fromCharToInt(char: Char): Int = when (char) {
        in 'a'..'z' -> char - 'a' + 1
        in 'A'..'Z' -> char - 'A' + 27
        else -> marker.throwParseException("Expected a point, but got $value")
    }

    when (value.length) {
        2 -> SgfPoint(
            x = fromCharToInt(value[0]),
            y = fromCharToInt(value[1])
        )
        else -> marker.throwParseException("Expected a point, but got $value")
    }
}

internal val moveParser = ValueParser { marker, value ->
    when (value.length) {
        0 -> Move.Pass
        2 -> Move.Stone(pointParser.parse(marker, value))
        else -> marker.throwParseException("Expected a move, but got $value")
    }
}

internal fun <L, R> TerminalNode.asComposed(left: ValueParser<L>, right: ValueParser<R>) = composed(left, right)
    .parse(symbol.toMarker(), textStrippedFromBrackets)

internal fun TerminalNode.asColor(stripBrackets: Boolean) = colorParser.parse(
    symbol.toMarker(stripBrackets),
    if (stripBrackets) textStrippedFromBrackets else text
)

internal fun TerminalNode.asDouble() = doubleParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asNumber(range: IntRange? = null) = numberParser(range).parse(
    symbol.toMarker(),
    textStrippedFromBrackets
)
internal fun TerminalNode.asReal() = realParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asSimpleText() = simpleTextParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asText() = textParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asMove() = moveParser.parse(symbol.toMarker(), textStrippedFromBrackets)
internal fun TerminalNode.asPoint() = pointParser.parse(symbol.toMarker(), textStrippedFromBrackets)

internal val TerminalNode.textStrippedFromBrackets
    get() = stripBrackets(text)

private fun stripBrackets(string: String) = string.substring(1, string.length - 1)

private fun Token.toMarker(strippedBrackets: Boolean = true): Marker {
    val startColumn = if (strippedBrackets) {
        charPositionInLine + 2
    } else {
        charPositionInLine + 1
    }

    return Marker(
        startLineNumber = line,
        startColumn = startColumn,
        endLineNumber = line,
        endColumn = startColumn + text.length - 2
    )
}

private fun Marker.throwParseException(message: String): Nothing = throw SgfParseException(message, this)
