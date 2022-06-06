package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class NavigationTest {
    @Test
    fun `going to next move skips nodes that has no moves`() {
        val colors = listOf(SgfColor.Black, SgfColor.White)

        assertAll(
            colors.map {
                {
                    val editor = SgfEditor()
                        .addStones(SgfColor.Black, SgfPoint(2, 2))
                        .setNextToPlay(it)
                        .placeStone(it, SgfPoint(3, 3))
                        .goToRootNode()

                    val next = editor.goToNextMove()
                    assertEquals(it to Move.Stone(SgfPoint(3, 3)), next.get().getCurrentMove())
                }
            }
        )
    }

    @Test
    fun `passes are included as moves when iterating moves`() {
        val colors = listOf(SgfColor.Black, SgfColor.White)
        assertAll(
            colors.map {
                {
                    val editor = SgfEditor().setNextToPlay(it).pass(it).goToRootNode()
                    val next = editor.goToNextMove().get()
                    assertEquals(it to Move.Pass, next.getCurrentMove())
                }
            }
        )
    }

    @Test
    fun `having no more moves to go to, a failed move result will be returned`() {
        val editor = SgfEditor()
        val actual = editor.goToNextMove()
        val expected = MoveResult.Failure(editor)
        assertEquals(expected, actual)
        assertNull(actual.origin.getCurrentMove())
    }

    @Test
    fun `having no moves to go back to returns a failed result`() {
        val editor = SgfEditor()
        val actual = editor.goToPreviousMove()
        val expected = MoveResult.Failure(editor)
        assertEquals(expected, actual)
        assertNull(actual.origin.getCurrentMove())
    }

    @Test
    fun `can navigate back to the previous move`() {
        val editor = SgfEditor().pass(SgfColor.Black).placeStone(SgfColor.White, 3, 3)
        val actual = editor.goToPreviousMove().get()
        assertEquals(SgfColor.Black to Move.Pass, actual.getCurrentMove())
    }
}
