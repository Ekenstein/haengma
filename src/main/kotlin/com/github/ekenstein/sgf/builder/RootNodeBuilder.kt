package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
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
    fun setup(block: SetupBuilder.() -> Unit)
}

internal class DefaultRootNodeBuilder(override var node: SgfNode) : RootNodeBuilder, DefaultNodeBuilder() {

    override fun size(size: Int) {
        node += SgfProperty.Root.SZ(size)
    }

    override fun size(width: Int, height: Int) {
        node += SgfProperty.Root.SZ(width, height)
    }

    override fun fileFormat(value: Int) {
        node += SgfProperty.Root.FF(value)
    }

    override fun gameType(value: GameType) {
        node += SgfProperty.Root.GM(value)
    }

    override fun charset(value: Charset) {
        node += SgfProperty.Root.CA(value)
    }

    override fun application(name: String, version: String) {
        node += SgfProperty.Root.AP(name, version)
    }

    override fun gameInfo(block: GameInfoNodeBuilder.() -> Unit) {
        val builder = DefaultGameInfoNodeBuilder(node)
        builder.block()
        node += builder.node
    }

    override fun setup(block: SetupBuilder.() -> Unit) {
        val builder = DefaultSetupBuilder(node)
        builder.block()
        node += builder.node
    }
}
