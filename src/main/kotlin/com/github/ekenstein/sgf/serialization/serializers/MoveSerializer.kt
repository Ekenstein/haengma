package com.github.ekenstein.sgf.serialization.serializers

import com.github.ekenstein.sgf.Move

internal fun moveSerializer(move: Move) = ValueSerializer { appendable ->
    when (move) {
        Move.Pass -> Unit
        is Move.Stone -> pointSerializer(move.point).serialize(appendable)
    }
}
