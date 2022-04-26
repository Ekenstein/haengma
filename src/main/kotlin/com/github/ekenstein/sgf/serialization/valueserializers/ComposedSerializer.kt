package com.github.ekenstein.sgf.serialization.valueserializers

internal fun composedSerializer(left: ValueSerializer, right: ValueSerializer) = ValueSerializer { appendable ->
    left.serialize(appendable)
    appendable.append(':')
    right.serialize(appendable)
}
