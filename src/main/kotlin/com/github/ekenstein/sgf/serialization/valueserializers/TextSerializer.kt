package com.github.ekenstein.sgf.serialization.valueserializers

private val whitespaceExceptNewLineRegex = Regex("""[^\S\r\n]""")
private val whitespaceExceptSpaceRegex = Regex("""[^\S ]+""")
private fun escapeRegex(isComposed: Boolean): Regex {
    val needsEscapingIfComposed = listOf(':').filter { isComposed }
    val needsEscaping = listOf("\\]", "\\\\") + needsEscapingIfComposed

    val escapeChars = needsEscaping.joinToString("")

    return Regex("""[$escapeChars]""")
}

internal fun simpleTextSerializer(string: String, isComposed: Boolean) = ValueSerializer { appendable ->
    val serialized = string.replace(whitespaceExceptSpaceRegex, " ")
        .replace(escapeRegex(isComposed), "\\$0")

    appendable.append(serialized)
}

internal fun textSerializer(string: String, isComposed: Boolean) = ValueSerializer { appendable ->
    val serialized = string.replace(whitespaceExceptNewLineRegex, " ")
        .replace(escapeRegex(isComposed), "\\$0")

    appendable.append(serialized)
}
