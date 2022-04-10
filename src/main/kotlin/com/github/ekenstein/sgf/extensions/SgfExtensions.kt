package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty

fun SgfGameTree.rootNode() = sequence.firstOrNull()

fun <T : SgfProperty> SgfGameTree.addNode(vararg properties: T): SgfGameTree = copy(
    sequence = sequence + SgfNode(properties.toSet())
)

inline fun <reified T : SgfProperty> SgfNode.property() = properties.filterIsInstance<T>().singleOrNull()

inline fun <reified T : SgfProperty> SgfNode.addProperty(property: T) = copy(
    properties = properties.filter { it !is T }.toSet() + property
)

inline fun <reified T : SgfProperty> SgfGameTree.addProperty(property: T): SgfGameTree {
    // workaround to keep the T::class intact
    val isRootProperty = property is SgfProperty.Root

    return when {
        isRootProperty -> {
            when (val rootNode = rootNode()) {
                null -> addNode(property)
                else -> copy(
                    sequence = listOf(rootNode.addProperty(property)) + sequence.drop(1)
                )
            }
        }
        else -> error("Unrecognized property ${T::class.simpleName}")
    }
}

val SgfProperty.isRootProperty
    get() = this is SgfProperty.Root
