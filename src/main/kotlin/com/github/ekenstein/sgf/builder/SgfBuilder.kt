package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.extensions.addProperty

@SgfMarker
interface SgfBuilder {
    fun root(block: RootNodeBuilder.() -> Unit)
    fun move(block: MoveBuilder.() -> Unit)
    fun variation(block: GameTreeBuilder.() -> Unit)
}

private class DefaultSgfBuilder : SgfBuilder {
    var gameTree = SgfGameTree.empty
        private set

    override fun root(block: RootNodeBuilder.() -> Unit) {
        val builder = DefaultRootNodeBuilder()
        builder.block()
        builder.node.properties.fold(gameTree) { gameTree, property -> gameTree.addProperty(property) }
    }

    override fun move(block: MoveBuilder.() -> Unit) {
        val builder = DefaultMoveBuilder()
        builder.block()
        builder.node.properties.fold(gameTree) { gameTree, property -> gameTree.addProperty(property) }
    }

    override fun variation(block: GameTreeBuilder.() -> Unit) {
        val builder = DefaultGameTreeBuilder()
        builder.block()
        gameTree = gameTree.copy(
            trees = gameTree.trees + builder.gameTree
        )
    }
}

fun sgf(block: SgfBuilder.() -> Unit): SgfGameTree {
    val builder = DefaultSgfBuilder()
    builder.block()
    return builder.gameTree
}
