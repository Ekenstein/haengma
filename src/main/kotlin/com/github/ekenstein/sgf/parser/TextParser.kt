package com.github.ekenstein.sgf.parser

private val escapedCharRegex = Regex("""\\([\\:\]])""")
private val whitespaceExceptNewLineRegex = Regex("""[^\S\r\n]""")
private val whitespaceExceptSpaceRegex = Regex("""[^\S ]+""")

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
