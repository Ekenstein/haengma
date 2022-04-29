package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.extensions.addProperty

@SgfDslMarker
interface GameTreeBuilder {
    fun move(block: MoveBuilder.() -> Unit)
    fun variation(block: GameTreeBuilder.() -> Unit)
}

internal class DefaultGameTreeBuilder : GameTreeBuilder {
    var gameTree = SgfGameTree.empty
        private set

    override fun move(block: MoveBuilder.() -> Unit) {
        val builder = DefaultMoveBuilder(SgfNode())
        builder.block()
        gameTree = builder.node.properties.fold(gameTree) { tree, property -> tree.addProperty(property) }
    }

    override fun variation(block: GameTreeBuilder.() -> Unit) {
        val builder = DefaultGameTreeBuilder()
        builder.block()
        gameTree = gameTree.copy(
            trees = gameTree.trees + builder.gameTree
        )
    }
}
