package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.gameTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SgfExtensionsTest {
    @Test
    fun `adding a property to a node will replace the old node`() {
        val node = SgfNode(setOf(SgfProperty.Root.GM(GameType.Go)))
        val newNode = node.addProperty(SgfProperty.Root.GM(GameType.Chess))
        val expected = SgfNode(setOf(SgfProperty.Root.GM(GameType.Chess)))
        assertEquals(expected, newNode)
    }

    @Test
    fun `adding a property of type root to the game tree when there is no root node will add a root node`() {
        val property = SgfProperty.Root.GM(GameType.Go)
        val tree = SgfGameTree.empty.addProperty(property)
        val expected = SgfGameTree(listOf(SgfNode(setOf(property))), emptyList())
        assertEquals(expected, tree)
    }

    @Test
    fun `adding a property of type root to the game tree will replace the existing property in the root node`() {
        val property = SgfProperty.Root.GM(GameType.Go)
        val tree = gameTree {
            node {
                property(SgfProperty.Root.GM(GameType.Chess))
                property(SgfProperty.Root.CA("UTF-8"))
            }
            node {
                property(SgfProperty.Move.B(Move.Stone(SgfPoint(1, 1))))
            }
        }

        val newTree = tree.addProperty(property)
        val expected = gameTree {
            node {
                property(property)
                property(SgfProperty.Root.CA("UTF-8"))
            }
            node {
                property(SgfProperty.Move.B(Move.Stone(SgfPoint(1, 1))))
            }
        }

        assertEquals(expected, newTree)
    }
}
