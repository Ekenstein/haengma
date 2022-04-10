package com.github.ekenstein.sgf

import com.github.ekenstein.sgf.parser.Marker
import com.github.ekenstein.sgf.parser.SgfLexer
import com.github.ekenstein.sgf.parser.SgfParseException
import com.github.ekenstein.sgf.parser.SgfParser
import com.github.ekenstein.sgf.parser.parse
import com.github.ekenstein.sgf.serialization.serialize
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path

class SgfConfiguration {
    /**
     * Whether decoding should be lenient or not. If true, properties that are malformed will be ignored.
     * Default is false.
     */
    var isLenient: Boolean = false
}

class Sgf(private val configure: SgfConfiguration.() -> Unit = { }) {
    private val sgfErrorListener = object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String?,
            e: RecognitionException?
        ) {
            throw SgfParseException(msg!!, Marker(line, charPositionInLine, line, charPositionInLine))
        }
    }

    fun encode(outputStream: OutputStream, collection: SgfCollection) {
        val printStream = PrintStream(outputStream)
        collection.serialize(printStream)
    }

    fun decode(string: String) = decode(CharStreams.fromString(string))
    fun decode(path: Path) = decode(CharStreams.fromPath(path))
    fun decode(inputStream: InputStream) = decode(CharStreams.fromStream(inputStream))

    private fun decode(charStream: CharStream): SgfCollection {
        val lexer = SgfLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(sgfErrorListener)

        val tokenStream = CommonTokenStream(lexer)
        val parser = SgfParser(tokenStream)
        parser.removeErrorListeners()
        parser.addErrorListener(sgfErrorListener)

        return parser.parse(config.isLenient)
    }

    private val config by lazy {
        val config = SgfConfiguration()
        config.configure()
        config
    }
}

fun Sgf.encodeToString(collection: SgfCollection): String = ByteArrayOutputStream().use {
    encode(it, collection)
    String(it.toByteArray())
}
