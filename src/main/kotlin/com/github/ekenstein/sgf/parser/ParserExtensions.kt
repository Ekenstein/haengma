package com.github.ekenstein.sgf.parser

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
import com.github.ekenstein.sgf.parser.valueparsers.gameTypeParser
import com.github.ekenstein.sgf.parser.valueparsers.moveParser
import com.github.ekenstein.sgf.parser.valueparsers.numberParser
import com.github.ekenstein.sgf.parser.valueparsers.pointParser
import com.github.ekenstein.sgf.parser.valueparsers.realParser
import com.github.ekenstein.sgf.parser.valueparsers.simpleTextParser
import com.github.ekenstein.sgf.parser.valueparsers.sizeParser
import com.github.ekenstein.sgf.parser.valueparsers.textParser
import com.github.ekenstein.sgf.toPropertySet
import com.github.ekenstein.sgf.utils.toNelUnsafe
import com.github.ekenstein.sgf.utils.toNonEmptySet
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
        msg: String,
        e: RecognitionException?
    ) {
        throw SgfException.ParseError.SyntaxError(msg, Marker(line, charPositionInLine, line, charPositionInLine))
    }
}

/**
 * Parses the given [string] to an [SgfCollection]. If there are any parse errors, [SgfException.ParseError]
 * will be thrown containing additional information of what went wrong.
 * @param string The string to parse to an [SgfCollection]
 * @param configure additional configuration for the parser
 * @throws SgfException.ParseError If the string couldn't be parsed to an [SgfCollection].
 */
fun SgfCollection.Companion.from(
    string: String,
    configure: SgfParserConfiguration.() -> Unit = { }
): SgfCollection = from(CharStreams.fromString(string), configure)

/**
 * Opens and parses the file located at the given [path] to an [SgfCollection]. If there are any parse errors,
 * [SgfException.ParseError] will be thrown containing additional information of what went wrong.
 * @param path The path to the file that should be parsed to an [SgfCollection]
 * @param configure additional configuration for the parser
 * @throws SgfException.ParseError If the file couldn't be parsed to an [SgfCollection].
 */
fun SgfCollection.Companion.from(
    path: Path,
    configure: SgfParserConfiguration.() -> Unit = { }
): SgfCollection = from(CharStreams.fromPath(path), configure)

/**
 * Reads and parses the given [inputStream] to an [SgfCollection]. If there are any parse errors,
 * [SgfException.ParseError] will be thrown containing additional information of what went wrong.
 * @param inputStream The input stream to read and parse to an [SgfCollection]
 * @param configure additional configuration for the parser
 * @throws SgfException.ParseError If the string couldn't be parsed to an [SgfCollection].
 */
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
        trees.toNelUnsafe()
    )
}

private fun SgfParser.GameTreeContext.extract(configuration: SgfParserConfiguration): SgfGameTree {
    val sequenceContext = sequence()
    val sequence = sequenceContext.extract(configuration).takeIf {
        it.isNotEmpty()
    } ?: throw SgfException.ParseError.Other("A game tree sequence must not be empty", sequenceContext.toMarker())

    val variations = gameTree().map { it.extract(configuration) }

    return SgfGameTree(sequence.toNelUnsafe(), variations)
}

private fun SgfParser.SequenceContext.extract(configuration: SgfParserConfiguration) =
    node().map { it.extract(configuration) }

private fun SgfParser.NodeContext.extract(configuration: SgfParserConfiguration): SgfNode {
    val props = prop().mapNotNull {
        try {
            it.extract(configuration)
        } catch (ex: SgfException.ParseError.MalformedPropertyValue) {
            if (configuration.ignoreMalformedProperties) {
                null
            } else if (configuration.preserveMalformedProperties) {
                SgfProperty.Private(
                    identifier = it.PROP_IDENTIFIER().text,
                    values = it.VALUE()?.map { value -> value.textStrippedFromBrackets } ?: emptyList()
                )
            } else {
                throw ex
            }
        }
    }
    return SgfNode(props.toPropertySet())
}

private fun SgfParser.PropContext.extract(configuration: SgfParserConfiguration) =
    when (val identifier = PROP_IDENTIFIER().text) {
        "B" -> SgfProperty.Move.B(extractSingleValueOrNull { it.asMove() } ?: Move.Pass)
        "W" -> SgfProperty.Move.W(extractSingleValueOrNull { it.asMove() } ?: Move.Pass)
        "KO" -> SgfProperty.Move.KO
        "MN" -> SgfProperty.Move.MN(extractSingleValue { it.asNumber() })
        "AB" -> SgfProperty.Setup.AB(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: toMarker().throwMalformedPropertyValueException("AB must not contain an empty set of points")
        )
        "AW" -> SgfProperty.Setup.AW(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: toMarker().throwMalformedPropertyValueException("AW must not contain an empty set of points")
        )
        "AE" -> SgfProperty.Setup.AE(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "AE must not contain an empty set of points",
                    toMarker()
                )
        )
        "PL" -> SgfProperty.Setup.PL(extractSingleValue { it.asColor(true) })
        "C" -> SgfProperty.NodeAnnotation.C(extractSingleValueOrNull { it.asText() } ?: "")
        "DM" -> SgfProperty.NodeAnnotation.DM(extractSingleValue { it.asDouble() })
        "GB" -> SgfProperty.NodeAnnotation.GB(extractSingleValue { it.asDouble() })
        "GW" -> SgfProperty.NodeAnnotation.GW(extractSingleValue { it.asDouble() })
        "HO" -> SgfProperty.NodeAnnotation.HO(extractSingleValue { it.asDouble() })
        "N" -> SgfProperty.NodeAnnotation.N(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "UC" -> SgfProperty.NodeAnnotation.UC(extractSingleValue { it.asDouble() })
        "V" -> SgfProperty.NodeAnnotation.V(extractSingleValue { it.asReal() })
        "BM" -> SgfProperty.MoveAnnotation.BM(extractSingleValue { it.asDouble() })
        "DO" -> SgfProperty.MoveAnnotation.DO
        "IT" -> SgfProperty.MoveAnnotation.IT
        "TE" -> SgfProperty.MoveAnnotation.TE(extractSingleValue { it.asDouble() })
        "CR" -> SgfProperty.Markup.CR(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "CR must not contain an empty set of points",
                    toMarker()
                )
        )
        "AR" -> SgfProperty.Markup.AR(
            extractValues { it.asComposed(pointParser, pointParser) }.toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "AR must not contain an empty list of composed points",
                    toMarker()
                )
        )
        "LB" -> SgfProperty.Markup.LB(extractValues { it.asComposed(pointParser, simpleTextParser) }.toMap())
        "LN" -> SgfProperty.Markup.LN(
            extractValues { it.asComposed(pointParser, pointParser) }.toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "LN must not contain an empty set of composed points",
                    toMarker()
                )
        )
        "MA" -> SgfProperty.Markup.MA(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "MA must not contain an empty set of points",
                    toMarker()
                )
        )
        "SL" -> SgfProperty.Markup.SL(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "SL must not contain an empty set of points",
                    toMarker()
                )
        )
        "SQ" -> SgfProperty.Markup.SQ(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "SQ must not contain an empty set of points",
                    toMarker()
                )
        )
        "TR" -> SgfProperty.Markup.TR(
            extractValues { it.asCompressedPoint() }.flatten().toNonEmptySet()
                ?: throw SgfException.ParseError.MalformedPropertyValue(
                    "TR must not contain an empty set of points",
                    toMarker()
                )
        )
        "DD" -> SgfProperty.Markup.DD(extractValues { it.asCompressedPoint() }.flatten().toSet())
        "AP" -> {
            val (name, version) = extractSingleValue { it.asComposed(simpleTextParser, simpleTextParser) }
            SgfProperty.Root.AP(name, version)
        }
        "FF" -> SgfProperty.Root.FF(extractSingleValue { it.asNumber(1..4) })
        "GM" -> SgfProperty.Root.GM(extractSingleValue { it.asGameType() })
        "SZ" -> {
            val (width, height) = extractSingleValue { it.asSize() }
            SgfProperty.Root.SZ(width, height)
        }
        "CA" -> SgfProperty.Root.CA(extractSingleValue { it.asCharset() })
        "ST" -> SgfProperty.Root.ST(extractSingleValue { it.asNumber(0..3) })
        "HA" -> SgfProperty.GameInfo.HA(extractSingleValue { it.asNumber(2..9) })
        "KM" -> SgfProperty.GameInfo.KM(extractSingleValue { it.asReal() })
        "EV" -> SgfProperty.GameInfo.EV(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "PB" -> SgfProperty.GameInfo.PB(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "PW" -> SgfProperty.GameInfo.PW(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "WR" -> SgfProperty.GameInfo.WR(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "BR" -> SgfProperty.GameInfo.BR(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "DT" -> SgfProperty.GameInfo.DT(extractSingleValue { it.asGameDates() })
        "RE" -> SgfProperty.GameInfo.RE(extractSingleValue { it.asGameResult() })
        "TM" -> SgfProperty.GameInfo.TM(extractSingleValue { it.asReal() })
        "SO" -> SgfProperty.GameInfo.SO(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "GN" -> SgfProperty.GameInfo.GN(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "GC" -> SgfProperty.GameInfo.GC(extractSingleValueOrNull { it.asText() } ?: "")
        "ON" -> SgfProperty.GameInfo.ON(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "OT" -> SgfProperty.GameInfo.OT(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "RO" -> SgfProperty.GameInfo.RO(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "RU" -> SgfProperty.GameInfo.RU(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "US" -> SgfProperty.GameInfo.US(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "WT" -> SgfProperty.GameInfo.WT(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "BT" -> SgfProperty.GameInfo.BT(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "AN" -> SgfProperty.GameInfo.AN(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "CP" -> SgfProperty.GameInfo.CP(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "PC" -> SgfProperty.GameInfo.PC(extractSingleValueOrNull { it.asSimpleText() } ?: "")
        "BL" -> SgfProperty.Timing.BL(extractSingleValue { it.asReal() })
        "WL" -> SgfProperty.Timing.WL(extractSingleValue { it.asReal() })
        "OB" -> SgfProperty.Timing.OB(extractSingleValue { it.asNumber() })
        "OW" -> SgfProperty.Timing.OW(extractSingleValue { it.asNumber() })
        "FG" -> SgfProperty.Misc.FG(extractSingleValueOrNull { it.asComposed(numberParser(), simpleTextParser) })
        "PM" -> SgfProperty.Misc.PM(extractSingleValue { it.asNumber() })
        "VW" -> SgfProperty.Misc.VW(extractValues { it.asCompressedPoint() }.flatten().toSet())
        else -> if (configuration.preserveUnknownProperties) {
            SgfProperty.Private(identifier, VALUE()?.map { it.textStrippedFromBrackets } ?: emptyList())
        } else {
            val marker = toMarker()
            throw SgfException.ParseError.Other(
                "Encountered an unknown property ${PROP_IDENTIFIER().text}",
                marker
            )
        }
    }

private fun <T> SgfParser.PropContext.extractSingleValueOrNull(block: (TerminalNode) -> T): T? {
    val values = VALUE() ?: emptyList()
    if (values.isEmpty()) {
        return null
    }

    if (values.size > 1) {
        toMarker().throwMalformedPropertyValueException("Expected NONE or a single value, but got $values")
    }

    return block(values[0])
}

private fun <T> SgfParser.PropContext.extractSingleValue(block: (TerminalNode) -> T): T {
    val values = VALUE() ?: emptyList()
    if (values.isEmpty() || values.size > 1) {
        toMarker().throwMalformedPropertyValueException("Expected a single value, but got $values")
    }

    return block(values[0])
}

private fun <T> SgfParser.PropContext.extractValues(block: (TerminalNode) -> T): List<T> {
    val values = VALUE() ?: emptyList()
    return values.map(block)
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
private fun TerminalNode.asGameType() = gameTypeParser.parse(symbol.toMarker(), textStrippedFromBrackets)
private fun TerminalNode.asSize() = sizeParser.parse(symbol.toMarker(), textStrippedFromBrackets)

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
