package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.utils.nelOf
import com.github.ekenstein.sgf.utils.orStay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QueryTest {
    @Test
    fun `current move number when positioned at root returns 1 if the root node has a move`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(3, 3))))
        val editor = SgfEditor(tree)
        val actual = editor.getMoveNumber()
        assertEquals(1, actual)
    }

    @Test
    fun `current move number at root without move properties is 0`() {
        val actual = SgfEditor().getMoveNumber()
        assertEquals(0, actual)
    }

    @Test
    fun `pass counts as a move and thus has a move number`() {
        val editor = SgfEditor().pass(SgfColor.Black)
        val actual = editor.getMoveNumber()
        assertEquals(1, actual)
    }

    @Test
    fun `a placed stone counts as a move and thus has a move number`() {
        val editor = SgfEditor().placeStone(SgfColor.Black, 3, 3)
        val actual = editor.getMoveNumber()
        assertEquals(1, actual)
    }

    @Test
    fun `setup properties does not count as a move and thus has no move number`() {
        val editor = SgfEditor().addStones(SgfColor.Black, SgfPoint(3, 3))
        val actual = editor.getMoveNumber()
        assertEquals(0, actual)
    }

    @Test
    fun `a move in a child tree still counts as a move and thus has a move number`() {
        val editor = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNode()
            .orStay()
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 5, 5)

        val moveNumber = editor.getMoveNumber()
        assertEquals(2, moveNumber)
    }
}
