package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.parser.valueparsers.ValueParser
import com.github.ekenstein.sgf.parser.valueparsers.charsetParser
import com.github.ekenstein.sgf.parser.valueparsers.colorParser
import com.github.ekenstein.sgf.parser.valueparsers.composed
import com.github.ekenstein.sgf.parser.valueparsers.compressedPointParser
import com.github.ekenstein.sgf.parser.valueparsers.dateParser
import com.github.ekenstein.sgf.parser.valueparsers.doubleParser
import com.github.ekenstein.sgf.parser.valueparsers.gameResultParser
import com.github.ekenstein.sgf.parser.valueparsers.moveParser
import com.github.ekenstein.sgf.parser.valueparsers.numberParser
import com.github.ekenstein.sgf.parser.valueparsers.pointParser
import com.github.ekenstein.sgf.parser.valueparsers.realParser
import com.github.ekenstein.sgf.parser.valueparsers.simpleTextParser
import com.github.ekenstein.sgf.parser.valueparsers.textParser
import com.github.ekenstein.sgf.utils.NonEmptyList
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.InputStream
import java.nio.file.Path

private class SgfErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        throw SgfException.ParseError(msg!!, Marker(line, charPositionInLine, line, charPositionInLine))
    }
}

fun SgfCollection.Companion.from(
    string: String,
    configure: SgfParserConfiguration.() -> Unit = { }
): SgfCollection = from(CharStreams.fromString(string), configure)

fun SgfCollection.Companion.from(
    path: Path,
    configure: SgfParserConfiguration.() -> Unit = { }
): SgfCollection = from(CharStreams.fromPath(path), configure)

fun SgfCollection.Companion.from(
    inputStream: InputStream,
    configure: SgfParserConfiguration.() -> Unit = { }
): SgfCollection = from(CharStreams.fromStream(inputStream), configure)

private fun SgfCollection.Companion.from(
    charStream: CharStream,
    configure: SgfParserConfiguration.() -> Unit = { }
): SgfCollection {
    val config = SgfParserConfiguration()
    val sgfErrorListener = SgfErrorListener()
    config.configure()
    val lexer = SgfLexer(charStream)
    lexer.removeErrorListeners()
    lexer.addErrorListener(sgfErrorListener)

    val tokenStream = CommonTokenStream(lexer)
    val parser = SgfParser(tokenStream)
    parser.removeErrorListeners()
    parser.addErrorListener(sgfErrorListener)

    return parser.collection().extract(config)
}

private fun SgfParser.CollectionContext.extract(configuration: SgfParserConfiguration): SgfCollection {
    val trees = gameTree().map { it.extract(configuration) }
    return SgfCollection(
        NonEmptyList.fromListUnsafe(trees)
    )
}

private fun SgfParser.GameTreeContext.extract(configuration: SgfParserConfiguration): SgfGameTree {
    val sequenceContext = sequence()
    val sequence = sequenceContext.extract(configuration).takeIf {
        it.isNotEmpty()
    } ?: throw SgfException.ParseError("A game tree sequence must not be empty", sequenceContext.toMarker())

    val variations = gameTree().map { it.extract(configuration) }

    return SgfGameTree(NonEmptyList.fromListUnsafe(sequence), variations)
}

private fun SgfParser.SequenceContext.extract(configuration: SgfParserConfiguration) =
    node().map { it.extract(configuration) }

private fun SgfParser.NodeContext.extract(configuration: SgfParserConfiguration): SgfNode {
    val props = prop().mapNotNull { it.extract(configuration) }.toSet()
    return SgfNode(props)
}

private fun SgfParser.PropContext.extract(configuration: SgfParserConfiguration) =
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
        privateProp() != null -> when (configuration.preserveUnknownProperties) {
            true -> privateProp().extract()
            false -> null
        }
        else -> throw SgfException.ParseError("Unrecognized property", toMarker())
    }

private fun SgfParser.PrivatePropContext.extract(): SgfProperty = SgfProperty.Private(
    identifier = PROP_IDENTIFIER().text,
    values = VALUE()?.map { it.textStrippedFromBrackets } ?: emptyList()
)

private fun SgfParser.MoveContext.extract(): SgfProperty.Move = when (this) {
    is SgfParser.BlackMoveContext -> SgfProperty.Move.B(VALUE()?.asMove() ?: Move.Pass)
    is SgfParser.WhiteMoveContext -> SgfProperty.Move.W(VALUE()?.asMove() ?: Move.Pass)
    is SgfParser.KoContext -> SgfProperty.Move.KO
    is SgfParser.MoveNumberContext -> SgfProperty.Move.MN(VALUE().asNumber())
    else -> throw SgfException.ParseError("Unrecognized move property $text", toMarker())
}

private fun SgfParser.SetupContext.extract(): SgfProperty.Setup = when (this) {
    is SgfParser.AddEmptyContext -> SgfProperty.Setup.AE(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.AddBlackContext -> SgfProperty.Setup.AB(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.AddWhiteContext -> SgfProperty.Setup.AW(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.PlayerToPlayContext -> SgfProperty.Setup.PL(VALUE().asColor(true))
    else -> throw SgfException.ParseError("Unrecognized setup property $text", toMarker())
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
    else -> throw SgfException.ParseError("Unrecognized node annotation property $text", toMarker())
}

private fun SgfParser.MoveAnnotationContext.extract(): SgfProperty.MoveAnnotation =
    when (this) {
        is SgfParser.BadMoveContext -> SgfProperty.MoveAnnotation.BM(VALUE().asDouble())
        is SgfParser.DoubtfulContext -> SgfProperty.MoveAnnotation.DO
        is SgfParser.InterestingContext -> SgfProperty.MoveAnnotation.IT
        is SgfParser.TesujiContext -> SgfProperty.MoveAnnotation.TE(VALUE().asDouble())
        else -> throw SgfException.ParseError("Unrecognized move annotation property $text", toMarker())
    }

private fun SgfParser.MarkupContext.extract(): SgfProperty.Markup = when (this) {
    is SgfParser.CircleContext -> SgfProperty.Markup.CR(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.ArrowContext -> SgfProperty.Markup.AR(VALUE().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.LabelContext -> SgfProperty.Markup.LB(VALUE().map { it.asComposed(pointParser, simpleTextParser) })
    is SgfParser.LineContext -> SgfProperty.Markup.LN(VALUE().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.MarkContext -> SgfProperty.Markup.MA(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.SelectedContext -> SgfProperty.Markup.SL(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.SquareContext -> SgfProperty.Markup.SQ(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.TriangleContext -> SgfProperty.Markup.TR(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    is SgfParser.DimPointsContext -> SgfProperty.Markup.DD(
        VALUE()?.flatMap { it.asCompressedPoint() }?.toSet().orEmpty()
    )
    else -> throw SgfException.ParseError("Unrecognized markup property $text", toMarker())
}

private fun SgfParser.RootContext.extract(): SgfProperty.Root = when (this) {
    is SgfParser.ApplicationContext -> {
        val (name, version) = VALUE().asComposed(simpleTextParser, simpleTextParser)
        SgfProperty.Root.AP(name, version)
    }
    is SgfParser.FileFormatContext -> SgfProperty.Root.FF(VALUE().asNumber(1..4))
    is SgfParser.GameContext -> {
        val raw = VALUE()
        val number = raw.asNumber()
        val allGameTypes = GameType.values()
        val gameType = allGameTypes.singleOrNull { it.value == number }
            ?: raw.symbol.toMarker().throwParseException("Expected a game type, but got $number")
        SgfProperty.Root.GM(gameType)
    }
    is SgfParser.SizeContext -> {
        val raw = VALUE()
        val numberParser = numberParser(1..52)
        val marker = raw.symbol.toMarker()

        val (width, height) = try {
            val number = numberParser.parse(marker, raw.textStrippedFromBrackets)
            number to number
        } catch (ex: SgfException.ParseError) {
            try {
                composed(numberParser, numberParser).parse(marker, raw.textStrippedFromBrackets)
            } catch (ex: SgfException.ParseError) {
                marker.throwParseException("Expected a number or a composed value containing numbers, but got $raw")
            }
        }
        SgfProperty.Root.SZ(width, height)
    }
    is SgfParser.CharsetContext -> SgfProperty.Root.CA(VALUE().asCharset())
    is SgfParser.StyleContext -> SgfProperty.Root.ST(VALUE().asNumber(0..3))
    else -> throw SgfException.ParseError("Unrecognized root property $text", toMarker())
}

private fun SgfParser.GameInfoContext.extract(): SgfProperty.GameInfo = when (this) {
    is SgfParser.HandicapContext -> SgfProperty.GameInfo.HA(VALUE().asNumber(2..9))
    is SgfParser.KomiContext -> SgfProperty.GameInfo.KM(VALUE().asReal())
    is SgfParser.EventContext -> SgfProperty.GameInfo.EV(VALUE().asSimpleText())
    is SgfParser.PlayerBlackContext -> SgfProperty.GameInfo.PB(VALUE().asSimpleText())
    is SgfParser.PlayerWhiteContext -> SgfProperty.GameInfo.PW(VALUE().asSimpleText())
    is SgfParser.WhiteRankContext -> SgfProperty.GameInfo.WR(VALUE().asSimpleText())
    is SgfParser.BlackRankContext -> SgfProperty.GameInfo.BR(VALUE().asSimpleText())
    is SgfParser.DateContext -> SgfProperty.GameInfo.DT(VALUE().asGameDates())
    is SgfParser.ResultContext -> SgfProperty.GameInfo.RE(VALUE().asGameResult())
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
    else -> throw SgfException.ParseError("Unrecognized game info property $text", toMarker())
}

private fun SgfParser.TimingContext.extract(): SgfProperty.Timing = when (this) {
    is SgfParser.BlackTimeLeftContext -> SgfProperty.Timing.BL(VALUE().asReal())
    is SgfParser.WhiteTimeLeftContext -> SgfProperty.Timing.WL(VALUE().asReal())
    is SgfParser.OtStonesBlackContext -> SgfProperty.Timing.OB(VALUE().asNumber())
    is SgfParser.OtStonesWhiteContext -> SgfProperty.Timing.OW(VALUE().asNumber())
    else -> throw SgfException.ParseError("Unrecognized timing property $text", toMarker())
}

private fun SgfParser.MiscContext.extract(): SgfProperty.Misc = when (this) {
    is SgfParser.FigureContext -> SgfProperty.Misc.FG(VALUE()?.asComposed(numberParser(), simpleTextParser))
    is SgfParser.PrintMoveModeContext -> SgfProperty.Misc.PM(VALUE().asNumber())
    is SgfParser.ViewContext -> SgfProperty.Misc.VW(VALUE().flatMap { it.asCompressedPoint() }.toSet())
    else -> throw SgfException.ParseError("Unrecognized misc property $text", toMarker())
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

private fun <L, R> TerminalNode.asComposed(left: ValueParser<L>, right: ValueParser<R>) = composed(left, right)
    .parse(symbol.toMarker(), textStrippedFromBrackets)

private fun TerminalNode.asColor(stripBrackets: Boolean) = colorParser.parse(
    symbol.toMarker(stripBrackets),
    if (stripBrackets) textStrippedFromBrackets else text
)

private fun TerminalNode.asDouble() = doubleParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asNumber(range: IntRange? = null) = numberParser(range).parse(
    symbol.toMarker(),
    textStrippedFromBrackets
)
private fun TerminalNode.asReal() = realParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asSimpleText() = simpleTextParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asText() = textParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asMove() = moveParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asCompressedPoint() = compressedPointParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asGameResult() = gameResultParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asGameDates() = dateParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asCharset() = charsetParser.parse(symbol.toMarker(), textStrippedFromBrackets)

private val TerminalNode.textStrippedFromBrackets
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
