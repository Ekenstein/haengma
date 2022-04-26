package com.github.ekenstein.sgf.serialization.valueserializers

internal fun interface ValueSerializer {
    fun serialize(appendable: Appendable)
}
