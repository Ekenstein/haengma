package com.github.ekenstein.sgf.serialization.serializers

internal fun interface ValueSerializer {
    fun serialize(appendable: Appendable)
}
