package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty

@SgfDslMarker
interface MoveBuilder : NodeBuilder {
    fun moveNumber(value: Int)
    fun stone(color: SgfColor, x: Int, y: Int)
    fun pass(color: SgfColor)
}

internal class DefaultMoveBuilder : MoveBuilder {
    var node = SgfNode.empty
        private set

    override fun moveNumber(value: Int) {
        node = node.addProperty(SgfProperty.Move.MN(value))
    }

    override fun stone(color: SgfColor, x: Int, y: Int) {
        node = when (color) {
            SgfColor.Black -> node.addProperty(SgfProperty.Move.B(x, y))
            SgfColor.White -> node.addProperty(SgfProperty.Move.W(x, y))
        }
    }

    override fun pass(color: SgfColor) {
        node = when (color) {
            SgfColor.Black -> node.addProperty(SgfProperty.Move.B.pass())
            SgfColor.White -> node.addProperty(SgfProperty.Move.W.pass())
        }
    }

    override fun property(value: SgfProperty) {
        node = node.addProperty(value)
    }

    override fun property(identifier: String, values: List<String>) {
        node = node.addProperty(SgfProperty.Private(identifier, values))
    }
}
