package com.github.ekenstein.sgf

@DslMarker
annotation class SgfMarker

@SgfMarker
interface SgfNodeBuilder {
    fun property(property: SgfProperty)
}

@SgfMarker
interface SgfGameTreeBuilder {
    fun node(block: SgfNodeBuilder.() -> Unit)
    fun tree(block: SgfGameTreeBuilder.() -> Unit)
}

@SgfMarker
interface SgfCollectionBuilder {
    fun tree(block: SgfGameTreeBuilder.() -> Unit)
}

private class CollectionBuilderImpl : SgfCollectionBuilder {
    val trees = mutableListOf<SgfGameTree>()
    override fun tree(block: SgfGameTreeBuilder.() -> Unit) {
        val builder = GameTreeBuilderImpl()
        builder.block()
        trees.add(SgfGameTree(builder.sequence, builder.trees))
    }
}

private class GameTreeBuilderImpl : SgfGameTreeBuilder {
    val sequence = mutableListOf<SgfNode>()
    val trees = mutableListOf<SgfGameTree>()

    override fun node(block: SgfNodeBuilder.() -> Unit) {
        val builder = NodeBuilderImpl()
        builder.block()
        sequence.add(SgfNode(builder.properties.toSet()))
    }

    override fun tree(block: SgfGameTreeBuilder.() -> Unit) {
        val builder = GameTreeBuilderImpl()
        builder.block()
        trees.add(SgfGameTree(builder.sequence, builder.trees))
    }
}

class NodeBuilderImpl : SgfNodeBuilder {
    val properties = mutableListOf<SgfProperty>()

    override fun property(property: SgfProperty) {
        properties.add(property)
    }
}

fun sgf(block: SgfCollectionBuilder.() -> Unit): SgfCollection {
    val builder = CollectionBuilderImpl()
    builder.block()

    return SgfCollection(builder.trees)
}

fun gameTree(block: SgfGameTreeBuilder.() -> Unit): SgfGameTree {
    val builder = GameTreeBuilderImpl()
    builder.block()
    return SgfGameTree(builder.sequence, builder.trees)
}
