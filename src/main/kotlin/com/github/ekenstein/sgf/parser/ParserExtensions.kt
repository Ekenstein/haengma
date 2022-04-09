package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import org.antlr.v4.runtime.ParserRuleContext

internal fun parseSgf(parser: SgfParser): SgfCollection {
    val collection = parser.collection()
    val gameTrees = collection.gameTree().map { extractGameTree(it) }
    return SgfCollection(gameTrees)
}

private fun extractGameTree(ctx: SgfParser.GameTreeContext): SgfGameTree {
    val sequence = extractSequence(ctx.sequence())
    val variations = ctx.gameTree().map { extractGameTree(it) }

    return SgfGameTree(sequence, variations)
}

private fun extractSequence(ctx: SgfParser.SequenceContext): List<SgfNode> = ctx.node().map { extractNode(it) }

private fun extractNode(ctx: SgfParser.NodeContext): SgfNode {
    val props = ctx.prop().map { extractProperty(it) }.toSet()
    return SgfNode(props)
}

private fun extractProperty(ctx: SgfParser.PropContext): SgfProperty = if (ctx.move() != null) {
    extractMoveProperty(ctx.move())
} else if (ctx.markup() != null) {
    extractMarkupProperty(ctx.markup())
} else if (ctx.misc() != null) {
    extractMiscProperty(ctx.misc())
} else if (ctx.gameInfo() != null) {
    extractGameInfoProperty(ctx.gameInfo())
} else if (ctx.root() != null) {
    extractRootProperty(ctx.root())
} else if (ctx.moveAnnotation() != null) {
    extractMoveAnnotationProperty(ctx.moveAnnotation())
} else if (ctx.nodeAnnotation() != null) {
    extractNodeAnnotationProperty(ctx.nodeAnnotation())
} else if (ctx.setup() != null) {
    extractSetupProperty(ctx.setup())
} else if (ctx.timing() != null) {
    extractTimingProperty(ctx.timing())
} else if (ctx.privateProp() != null) {
    extractPrivateProperty(ctx.privateProp())
} else {
    throw SgfParseException("Unrecognized property", ctx.toMarker())
}

private fun extractPrivateProperty(ctx: SgfParser.PrivatePropContext): SgfProperty = SgfProperty.Private(
    identifier = ctx.PROP_IDENTIFIER().text,
    values = ctx.TEXT()?.map { it.textStrippedFromBrackets } ?: emptyList()
)

private fun extractMoveProperty(ctx: SgfParser.MoveContext): SgfProperty.Move = when (ctx) {
    is SgfParser.BlackMoveContext -> SgfProperty.Move.B(
        ctx.NONE()?.let { Move.Pass } ?: ctx.TEXT().asMove()
    )
    is SgfParser.WhiteMoveContext -> SgfProperty.Move.W(
        ctx.NONE()?.let { Move.Pass } ?: ctx.TEXT().asMove()
    )
    is SgfParser.KoContext -> SgfProperty.Move.KO
    is SgfParser.MoveNumberContext -> SgfProperty.Move.MN(ctx.TEXT().asNumber())
    else -> throw SgfParseException("Unrecognized move property ${ctx.text}", ctx.toMarker())
}

private fun extractSetupProperty(ctx: SgfParser.SetupContext): SgfProperty.Setup = when (ctx) {
    is SgfParser.AddEmptyContext -> SgfProperty.Setup.AE(ctx.TEXT().map { it.asPoint() }.toSet())
    is SgfParser.AddBlackContext -> SgfProperty.Setup.AB(ctx.TEXT().map { it.asPoint() }.toSet())
    is SgfParser.AddWhiteContext -> SgfProperty.Setup.AW(ctx.TEXT().map { it.asPoint() }.toSet())
    is SgfParser.PlayerToPlayContext -> SgfProperty.Setup.PL(ctx.TEXT().asColor(true))
    else -> throw SgfParseException("Unrecognized setup property ${ctx.text}", ctx.toMarker())
}

private fun extractNodeAnnotationProperty(ctx: SgfParser.NodeAnnotationContext): SgfProperty.NodeAnnotation = when (ctx) {
    is SgfParser.CommentContext -> SgfProperty.NodeAnnotation.C(ctx.TEXT().asText())
    is SgfParser.EvenPositionContext -> SgfProperty.NodeAnnotation.DM(ctx.TEXT().asDouble())
    is SgfParser.GoodForBlackContext -> SgfProperty.NodeAnnotation.GB(ctx.TEXT().asDouble())
    is SgfParser.GoodForWhiteContext -> SgfProperty.NodeAnnotation.GW(ctx.TEXT().asDouble())
    is SgfParser.HotspotContext -> SgfProperty.NodeAnnotation.HO(ctx.TEXT().asDouble())
    is SgfParser.NodeNameContext -> SgfProperty.NodeAnnotation.N(ctx.TEXT().asSimpleText())
    is SgfParser.UnclearPositionContext -> SgfProperty.NodeAnnotation.UC(ctx.TEXT().asDouble())
    is SgfParser.ValueContext -> SgfProperty.NodeAnnotation.V(ctx.TEXT().asReal())
    else -> throw SgfParseException("Unrecognized node annotation property ${ctx.text}", ctx.toMarker())
}

private fun extractMoveAnnotationProperty(ctx: SgfParser.MoveAnnotationContext): SgfProperty.MoveAnnotation =
    when (ctx) {
        is SgfParser.BadMoveContext -> SgfProperty.MoveAnnotation.BM(ctx.TEXT().asDouble())
        is SgfParser.DoubtfulContext -> SgfProperty.MoveAnnotation.DO
        is SgfParser.InterestingContext -> SgfProperty.MoveAnnotation.IT
        is SgfParser.TesujiContext -> SgfProperty.MoveAnnotation.TE(ctx.TEXT().asDouble())
        else -> throw SgfParseException("Unrecognized move annotation property ${ctx.text}", ctx.toMarker())
    }

private fun extractMarkupProperty(ctx: SgfParser.MarkupContext): SgfProperty.Markup = when (ctx) {
    is SgfParser.CircleContext -> SgfProperty.Markup.CR(ctx.TEXT().map { it.asPoint() })
    is SgfParser.ArrowContext -> SgfProperty.Markup.AR(ctx.TEXT().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.LabelContext -> SgfProperty.Markup.LB(ctx.TEXT().map { it.asComposed(pointParser, simpleTextParser) })
    is SgfParser.LineContext -> SgfProperty.Markup.LN(ctx.TEXT().map { it.asComposed(pointParser, pointParser) })
    is SgfParser.MarkContext -> SgfProperty.Markup.MA(ctx.TEXT().map { it.asPoint() })
    is SgfParser.SelectedContext -> SgfProperty.Markup.SL(ctx.TEXT().map { it.asPoint() })
    is SgfParser.SquareContext -> SgfProperty.Markup.SQ(ctx.TEXT().map { it.asPoint() })
    is SgfParser.TriangleContext -> SgfProperty.Markup.TR(ctx.TEXT().map { it.asPoint() })
    is SgfParser.DimPointsContext -> SgfProperty.Markup.DD(ctx.TEXT()?.map { it.asPoint() } ?: emptyList())
    else -> throw SgfParseException("Unrecognized markup property ${ctx.text}", ctx.toMarker())
}

private fun extractRootProperty(ctx: SgfParser.RootContext): SgfProperty.Root = when (ctx) {
    is SgfParser.ApplicationContext -> {
        val (name, version) = ctx.TEXT().asComposed(simpleTextParser, simpleTextParser)
        SgfProperty.Root.AP(name, version)
    }
    is SgfParser.FileFormatContext -> SgfProperty.Root.FF(ctx.TEXT().asNumber())
    is SgfParser.GameContext -> SgfProperty.Root.GM(ctx.TEXT().asNumber())
    is SgfParser.SizeContext -> SgfProperty.Root.SZ(ctx.TEXT().asNumber())
    is SgfParser.CharsetContext -> SgfProperty.Root.CA(ctx.TEXT().asSimpleText())
    is SgfParser.StyleContext -> SgfProperty.Root.ST(ctx.TEXT().asNumber())
    else -> throw SgfParseException("Unrecognized root property ${ctx.text}", ctx.toMarker())
}

private fun extractGameInfoProperty(ctx: SgfParser.GameInfoContext): SgfProperty.GameInfo = when (ctx) {
    is SgfParser.HandicapContext -> SgfProperty.GameInfo.HA(ctx.TEXT().asNumber())
    is SgfParser.KomiContext -> SgfProperty.GameInfo.KM(ctx.TEXT().asReal())
    is SgfParser.EventContext -> SgfProperty.GameInfo.EV(ctx.TEXT().asSimpleText())
    is SgfParser.PlayerBlackContext -> SgfProperty.GameInfo.PB(ctx.TEXT().asSimpleText())
    is SgfParser.PlayerWhiteContext -> SgfProperty.GameInfo.PW(ctx.TEXT().asSimpleText())
    is SgfParser.WhiteRankContext -> SgfProperty.GameInfo.WR(ctx.TEXT().asSimpleText())
    is SgfParser.BlackRankContext -> SgfProperty.GameInfo.BR(ctx.TEXT().asSimpleText())
    is SgfParser.DateContext -> SgfProperty.GameInfo.DT(ctx.TEXT().asSimpleText())
    is SgfParser.ResultContext -> SgfProperty.GameInfo.RE(ctx.TEXT().asSimpleText())
    is SgfParser.TimeLimitContext -> SgfProperty.GameInfo.TM(ctx.TEXT().asReal())
    is SgfParser.SourceContext -> SgfProperty.GameInfo.SO(ctx.TEXT().asSimpleText())
    is SgfParser.GameNameContext -> SgfProperty.GameInfo.GN(ctx.TEXT().asSimpleText())
    is SgfParser.GameCommentContext -> SgfProperty.GameInfo.GC(ctx.TEXT().asText())
    is SgfParser.OpeningContext -> SgfProperty.GameInfo.ON(ctx.TEXT().asSimpleText())
    is SgfParser.OvertimeContext -> SgfProperty.GameInfo.OT(ctx.TEXT().asSimpleText())
    is SgfParser.RoundContext -> SgfProperty.GameInfo.RO(ctx.TEXT().asSimpleText())
    is SgfParser.RulesContext -> SgfProperty.GameInfo.RU(ctx.TEXT().asSimpleText())
    is SgfParser.UserContext -> SgfProperty.GameInfo.US(ctx.TEXT().asSimpleText())
    is SgfParser.WhiteTeamContext -> SgfProperty.GameInfo.WT(ctx.TEXT().asSimpleText())
    is SgfParser.BlackTeamContext -> SgfProperty.GameInfo.BT(ctx.TEXT().asSimpleText())
    is SgfParser.AnnotationContext -> SgfProperty.GameInfo.AN(ctx.TEXT().asSimpleText())
    is SgfParser.CopyrightContext -> SgfProperty.GameInfo.CP(ctx.TEXT().asSimpleText())
    is SgfParser.PlaceContext -> SgfProperty.GameInfo.PC(ctx.TEXT().asSimpleText())
    else -> throw SgfParseException("Unrecognized game info property ${ctx.text}", ctx.toMarker())
}

private fun extractTimingProperty(ctx: SgfParser.TimingContext): SgfProperty.Timing = when (ctx) {
    is SgfParser.BlackTimeLeftContext -> SgfProperty.Timing.BL(ctx.TEXT().asReal())
    is SgfParser.WhiteTimeLeftContext -> SgfProperty.Timing.WL(ctx.TEXT().asReal())
    is SgfParser.OtStonesBlackContext -> SgfProperty.Timing.OB(ctx.TEXT().asNumber())
    is SgfParser.OtStonesWhiteContext -> SgfProperty.Timing.OW(ctx.TEXT().asNumber())
    else -> throw SgfParseException("Unrecognized timing property ${ctx.text}", ctx.toMarker())
}

private fun extractMiscProperty(ctx: SgfParser.MiscContext): SgfProperty.Misc = when (ctx) {
    is SgfParser.FigureContext -> SgfProperty.Misc.FG(ctx.TEXT().asComposed(numberParser, simpleTextParser))
    is SgfParser.PrintMoveModeContext -> SgfProperty.Misc.PM(ctx.TEXT().asNumber())
    is SgfParser.ViewContext -> SgfProperty.Misc.VW(ctx.TEXT().map { it.asPoint() })
    else -> throw SgfParseException("Unrecognized misc property ${ctx.text}", ctx.toMarker())
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
