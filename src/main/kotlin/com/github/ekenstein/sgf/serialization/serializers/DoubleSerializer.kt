package com.github.ekenstein.sgf.serialization.serializers

import com.github.ekenstein.sgf.SgfDouble

internal fun doubleSerializer(double: SgfDouble) = ValueSerializer { appendable ->
    when (double) {
        SgfDouble.Normal -> appendable.append('1')
        SgfDouble.Emphasized -> appendable.append('2')
    }
}
