package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.extensions.plus
import java.nio.charset.Charset

@SgfDslMarker
interface RootNodeBuilder : NodeBuilder {
    fun size(size: Int)
    fun size(width: Int, height: Int)
    fun fileFormat(value: Int)
    fun gameType(value: GameType)
    fun charset(value: Charset)
    fun application(name: String, version: String)

    fun gameInfo(block: GameInfoNodeBuilder.() -> Unit)
}

internal class DefaultRootNodeBuilder(var node: SgfNode) : RootNodeBuilder {

    override fun size(size: Int) {
        node = node.addProperty(SgfProperty.Root.SZ(size))
    }

    override fun size(width: Int, height: Int) {
        node = node.addProperty(SgfProperty.Root.SZ(width, height))
    }

    override fun fileFormat(value: Int) {
        node = node.addProperty(SgfProperty.Root.FF(value))
    }

    override fun gameType(value: GameType) {
        node = node.addProperty(SgfProperty.Root.GM(value))
    }

    override fun charset(value: Charset) {
        node = node.addProperty(SgfProperty.Root.CA(value))
    }

    override fun application(name: String, version: String) {
        node = node.addProperty(SgfProperty.Root.AP(name, version))
    }

    override fun gameInfo(block: GameInfoNodeBuilder.() -> Unit) {
        val builder = DefaultGameInfoNodeBuilder(node)
        builder.block()
        node += builder.node
    }

    override fun property(value: SgfProperty) {
        node = node.addProperty(value)
    }

    override fun property(identifier: String, values: List<String>) {
        node = node.addProperty(SgfProperty.Private(identifier, values))
    }
}
