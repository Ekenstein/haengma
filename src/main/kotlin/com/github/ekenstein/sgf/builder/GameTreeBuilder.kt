package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.utils.nelOf

@SgfDslMarker
interface GameTreeBuilder {
    fun move(block: MoveBuilder.() -> Unit)
    fun setup(block: SetupBuilder.() -> Unit)
    fun variation(block: GameTreeBuilder.() -> Unit)
}

internal class DefaultGameTreeBuilder : GameTreeBuilder {
    var gameTree = SgfGameTree(nelOf(SgfNode()))
        private set

    override fun move(block: MoveBuilder.() -> Unit) {
        val builder = DefaultMoveBuilder(SgfNode())
        builder.block()
        gameTree = builder.node.properties.fold(gameTree) { tree, property -> tree.addProperty(property) }
    }

    override fun setup(block: SetupBuilder.() -> Unit) {
        val builder = DefaultSetupBuilder(SgfNode())
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
