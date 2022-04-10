package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import org.antlr.v4.runtime.ParserRuleContext

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
    values = TEXT()?.map { it.textStrippedFromBrackets } ?: emptyList()
)

private fun SgfParser.MoveContext.extract(): SgfProperty.Move = when (this) {
    is SgfParser.BlackMoveContext -> SgfProperty.Move.B(
        NONE()?.let { Move.Pass } ?: TEXT().asMove()
    )
    is SgfParser.WhiteMoveContext -> SgfProperty.Move.W(
        NONE()?.let { Move.Pass } ?: TEXT().asMove()
    )
    is SgfParser.KoContext -> SgfProperty.Move.KO
    is SgfParser.MoveNumberContext -> SgfProperty.Move.MN(TEXT().asNumber())
    else -> throw SgfParseException("Unrecognized move property $text", toMarker())
}

private fun SgfParser.SetupContext.extract(): SgfProperty.Setup = when (this) {
    is SgfParser.AddEmptyContext -> SgfProperty.Setup.AE(TEXT().map { it.asPoint() }.toSet())
    is SgfParser.AddBlackContext -> SgfProperty.Setup.AB(TEXT().map { it.asPoint() }.toSet())
    is SgfParser.AddWhiteContext -> SgfProperty.Setup.AW(TEXT().map { it.asPoint() }.toSet())
    is SgfParser.PlayerToPlayContext -> SgfProperty.Setup.PL(TEXT().asColor(true))
    else -> throw SgfParseException("Unrecognized setup property $text", toMarker())
}

private fun SgfParser.NodeAnnotationContext.extract(): SgfProperty.NodeAnnotation = when (this) {
    is SgfParser.CommentContext -> SgfProperty.NodeAnnotation.C(TEXT().asText())
    is SgfParser.EvenPositionContext -> SgfProperty.NodeAnnotation.DM(TEXT().asDouble())
    is SgfParser.GoodForBlackContext -> SgfProperty.NodeAnnotation.GB(TEXT().asDouble())
    is SgfParser.GoodForWhiteContext -> SgfProperty.NodeAnnotation.GW(TEXT().asDouble())
    is SgfParser.HotspotContext -> SgfProperty.NodeAnnotation.HO(TEXT().asDouble())
    is SgfParser.NodeNameContext -> SgfProperty.NodeAnnotation.N(TEXT().asSimpleText())
    is SgfParser.UnclearPositionContext -> SgfProperty.NodeAnnotation.UC(TEXT().asDouble())
    is SgfParser.ValueContext -> SgfProperty.NodeAnnotation.V(TEXT().asReal())
    else -> throw SgfParseException("Unrecognized node annotation property $text", toMarker())
}

private fun SgfParser.MoveAnnotationContext.extract(): SgfProperty.MoveAnnotation =
    when (this) {
        is SgfParser.BadMoveContext -> SgfProperty.MoveAnnotation.BM(TEXT().asDouble())
        is SgfParser.DoubtfulContext -> SgfProperty.MoveAnnotation.DO
        is SgfParser.InterestingContext -> SgfProperty.MoveAnnotation.IT
        is SgfParser.TesujiContext -> SgfProperty.MoveAnnotation.TE(TEXT().asDouble())
        else -> throw SgfParseException("Unrecognized move annotation property $text", toMarker())
    }

private fun SgfParser.MarkupContext.extract(): SgfProperty.Markup = when (this) {
    is SgfParser.CircleContext -> SgfProperty.Markup.CR(TEXT().map { it.asPoint() })
    is SgfParser.ArrowContext -> SgfProperty.Markup.AR(TEXT().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.LabelContext -> SgfProperty.Markup.LB(TEXT().map { it.asComposed(pointParser, simpleTextParser) })
    is SgfParser.LineContext -> SgfProperty.Markup.LN(TEXT().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.MarkContext -> SgfProperty.Markup.MA(TEXT().map { it.asPoint() })
    is SgfParser.SelectedContext -> SgfProperty.Markup.SL(TEXT().map { it.asPoint() })
    is SgfParser.SquareContext -> SgfProperty.Markup.SQ(TEXT().map { it.asPoint() })
    is SgfParser.TriangleContext -> SgfProperty.Markup.TR(TEXT().map { it.asPoint() })
    is SgfParser.DimPointsContext -> SgfProperty.Markup.DD(TEXT()?.map { it.asPoint() } ?: emptyList())
    else -> throw SgfParseException("Unrecognized markup property $text", toMarker())
}

private fun SgfParser.RootContext.extract(): SgfProperty.Root = when (this) {
    is SgfParser.ApplicationContext -> {
        val (name, version) = TEXT().asComposed(simpleTextParser, simpleTextParser)
        SgfProperty.Root.AP(name, version)
    }
    is SgfParser.FileFormatContext -> SgfProperty.Root.FF(TEXT().asNumber())
    is SgfParser.GameContext -> SgfProperty.Root.GM(TEXT().asNumber())
    is SgfParser.SizeContext -> SgfProperty.Root.SZ(TEXT().asNumber())
    is SgfParser.CharsetContext -> SgfProperty.Root.CA(TEXT().asSimpleText())
    is SgfParser.StyleContext -> SgfProperty.Root.ST(TEXT().asNumber())
    else -> throw SgfParseException("Unrecognized root property $text", toMarker())
}

private fun SgfParser.GameInfoContext.extract(): SgfProperty.GameInfo = when (this) {
    is SgfParser.HandicapContext -> SgfProperty.GameInfo.HA(TEXT().asNumber())
    is SgfParser.KomiContext -> SgfProperty.GameInfo.KM(TEXT().asReal())
    is SgfParser.EventContext -> SgfProperty.GameInfo.EV(TEXT().asSimpleText())
    is SgfParser.PlayerBlackContext -> SgfProperty.GameInfo.PB(TEXT().asSimpleText())
    is SgfParser.PlayerWhiteContext -> SgfProperty.GameInfo.PW(TEXT().asSimpleText())
    is SgfParser.WhiteRankContext -> SgfProperty.GameInfo.WR(TEXT().asSimpleText())
    is SgfParser.BlackRankContext -> SgfProperty.GameInfo.BR(TEXT().asSimpleText())
    is SgfParser.DateContext -> SgfProperty.GameInfo.DT(TEXT().asSimpleText())
    is SgfParser.ResultContext -> SgfProperty.GameInfo.RE(TEXT().asSimpleText())
    is SgfParser.TimeLimitContext -> SgfProperty.GameInfo.TM(TEXT().asReal())
    is SgfParser.SourceContext -> SgfProperty.GameInfo.SO(TEXT().asSimpleText())
    is SgfParser.GameNameContext -> SgfProperty.GameInfo.GN(TEXT().asSimpleText())
    is SgfParser.GameCommentContext -> SgfProperty.GameInfo.GC(TEXT().asText())
    is SgfParser.OpeningContext -> SgfProperty.GameInfo.ON(TEXT().asSimpleText())
    is SgfParser.OvertimeContext -> SgfProperty.GameInfo.OT(TEXT().asSimpleText())
    is SgfParser.RoundContext -> SgfProperty.GameInfo.RO(TEXT().asSimpleText())
    is SgfParser.RulesContext -> SgfProperty.GameInfo.RU(TEXT().asSimpleText())
    is SgfParser.UserContext -> SgfProperty.GameInfo.US(TEXT().asSimpleText())
    is SgfParser.WhiteTeamContext -> SgfProperty.GameInfo.WT(TEXT().asSimpleText())
    is SgfParser.BlackTeamContext -> SgfProperty.GameInfo.BT(TEXT().asSimpleText())
    is SgfParser.AnnotationContext -> SgfProperty.GameInfo.AN(TEXT().asSimpleText())
    is SgfParser.CopyrightContext -> SgfProperty.GameInfo.CP(TEXT().asSimpleText())
    is SgfParser.PlaceContext -> SgfProperty.GameInfo.PC(TEXT().asSimpleText())
    else -> throw SgfParseException("Unrecognized game info property $text", toMarker())
}

private fun SgfParser.TimingContext.extract(): SgfProperty.Timing = when (this) {
    is SgfParser.BlackTimeLeftContext -> SgfProperty.Timing.BL(TEXT().asReal())
    is SgfParser.WhiteTimeLeftContext -> SgfProperty.Timing.WL(TEXT().asReal())
    is SgfParser.OtStonesBlackContext -> SgfProperty.Timing.OB(TEXT().asNumber())
    is SgfParser.OtStonesWhiteContext -> SgfProperty.Timing.OW(TEXT().asNumber())
    else -> throw SgfParseException("Unrecognized timing property $text", toMarker())
}

private fun SgfParser.MiscContext.extract(): SgfProperty.Misc = when (this) {
    is SgfParser.FigureContext -> SgfProperty.Misc.FG(TEXT().asComposed(numberParser, simpleTextParser))
    is SgfParser.PrintMoveModeContext -> SgfProperty.Misc.PM(TEXT().asNumber())
    is SgfParser.ViewContext -> SgfProperty.Misc.VW(TEXT().map { it.asPoint() })
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

private fun <T> withParseExceptionHandler(isLenient: Boolean, block: () -> T): T? = try {
    block()
} catch (ex: SgfParseException) {
    if (isLenient) {
        null
    } else {
        throw ex
    }
}
