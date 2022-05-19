package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.utils.nelOf

@SgfDslMarker
interface SgfBuilder {
    fun root(block: RootNodeBuilder.() -> Unit)
    fun move(block: MoveBuilder.() -> Unit)
    fun setup(block: SetupBuilder.() -> Unit)
    fun variation(block: GameTreeBuilder.() -> Unit)
}

private class DefaultSgfBuilder(var gameTree: SgfGameTree) : SgfBuilder {
    override fun root(block: RootNodeBuilder.() -> Unit) {
        val rootNode = gameTree.sequence.firstOrNull() ?: SgfNode()
        val builder = DefaultRootNodeBuilder(rootNode)
        builder.block()
        gameTree = builder.node.properties.fold(gameTree) { gameTree, property -> gameTree.addProperty(property) }
    }

    override fun move(block: MoveBuilder.() -> Unit) {
        val builder = DefaultMoveBuilder(SgfNode())
        builder.block()
        gameTree = builder.node.properties.fold(gameTree) { gameTree, property -> gameTree.addProperty(property) }
    }

    override fun setup(block: SetupBuilder.() -> Unit) {
        val builder = DefaultSetupBuilder(SgfNode())
        builder.block()
        gameTree = builder.node.properties.fold(gameTree) { gameTree, property -> gameTree.addProperty(property) }
    }

    override fun variation(block: GameTreeBuilder.() -> Unit) {
        val builder = DefaultGameTreeBuilder()
        builder.block()
        gameTree = gameTree.copy(
            trees = gameTree.trees + builder.gameTree
        )
    }
}

fun sgf(block: SgfBuilder.() -> Unit) = sgf(SgfGameTree(nelOf(SgfNode())), block)

fun sgf(sgfGameTree: SgfGameTree, block: SgfBuilder.() -> Unit): SgfGameTree {
    val builder = DefaultSgfBuilder(sgfGameTree)
    builder.block()
    return builder.gameTree
}
