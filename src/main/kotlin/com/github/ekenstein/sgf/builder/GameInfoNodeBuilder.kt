package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty

@SgfMarker
interface GameInfoNodeBuilder : NodeBuilder {
    fun handicap(value: Int)
    fun komi(value: Double)
    fun result(value: String)
}

internal class DefaultGameInfoNodeBuilder : GameInfoNodeBuilder {
    var node: SgfNode = SgfNode.empty
        private set

    override fun handicap(value: Int) {
        node = node.addProperty(SgfProperty.GameInfo.HA(value))
    }

    override fun komi(value: Double) {
        node = node.addProperty(SgfProperty.GameInfo.KM(value))
    }

    override fun result(value: String) {
        node = node.addProperty(SgfProperty.GameInfo.RE(value))
    }

    override fun property(value: SgfProperty) {
        node = node.addProperty(value)
    }

    override fun property(identifier: String, values: List<String>) {
        node = node.addProperty(SgfProperty.Private(identifier, values))
    }
}
