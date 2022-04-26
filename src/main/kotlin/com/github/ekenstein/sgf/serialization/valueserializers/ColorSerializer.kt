package com.github.ekenstein.sgf.serialization.valueserializers

import com.github.ekenstein.sgf.SgfColor

internal fun colorSerializer(color: SgfColor) = ValueSerializer { appendable ->
    when (color) {
        SgfColor.Black -> appendable.append('B')
        SgfColor.White -> appendable.append('W')
    }
}
