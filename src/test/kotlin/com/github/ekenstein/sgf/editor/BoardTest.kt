package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class BoardTest {
    @Test
    fun `placing a stone outside of the board results an IllegalArgumentException`() {
        val illegalPoints = (1..19).flatMap {
            listOf(
                SgfPoint(-1, it),
                SgfPoint(0, it),
                SgfPoint(20, it),
                SgfPoint(it, -1),
                SgfPoint(it, 0),
                SgfPoint(it, 20)
            )
        }
        val board = Board.empty(19)

        assertAll(
            illegalPoints.map {
                {
                    assertThrows<IllegalArgumentException> {
                        board.placeStone(SgfColor.Black, it)
                    }
                    assertThrows<IllegalArgumentException> {
                        board.placeStone(SgfColor.Black, it)
                    }
                    assertThrows<IllegalArgumentException> {
                        board.placeStone(SgfColor.White, it)
                    }
                    assertThrows<IllegalArgumentException> {
                        board.placeStone(SgfColor.White, it)
                    }
                }
            }
        )
    }

    @Test
    fun `placing a stone on the board adds it to the board position`() {
        val actual = Board.empty(19).placeStone(SgfColor.Black, SgfPoint(3, 3))
        val expected = Board.empty(19).copy(stones = mapOf(SgfPoint(3, 3) to SgfColor.Black))
        assertEquals(expected, actual)
    }

    @Test
    fun `capturing a stone increases the capture count for that color that captured the stone`() {
        val tigersMouth = setOf(
            SgfPoint(1, 2),
            SgfPoint(2, 1),
            SgfPoint(3, 2)
        )

        val ponnuki = tigersMouth + SgfPoint(2, 3)

        assertAll(
            {
                val board = ponnuki.fold(Board.empty(19)) { board, point ->
                    board.placeStone(SgfColor.Black, point)
                }

                val actualBoard = board.placeStone(SgfColor.White, SgfPoint(2, 2))
                val expectedBoard = board.copy(blackCaptures = 1)
                assertEquals(expectedBoard, actualBoard)
            },
            {
                val board = ponnuki.fold(Board.empty(19)) { board, point ->
                    board.placeStone(SgfColor.White, point)
                }

                val actualBoard = board.placeStone(SgfColor.Black, SgfPoint(2, 2))
                val expectedBoard = board.copy(whiteCaptures = 1)
                assertEquals(expectedBoard, actualBoard)
            },
            {
                val board = tigersMouth.fold(Board.empty(19)) { board, point ->
                    board.placeStone(SgfColor.Black, point)
                }.placeStone(SgfColor.White, SgfPoint(2, 2))

                val actualBoard = board.placeStone(SgfColor.Black, SgfPoint(2, 3))
                val expectedBoard = ponnuki.fold(Board.empty(19)) { b, point ->
                    b.placeStone(SgfColor.Black, point)
                }.copy(blackCaptures = 1)

                assertEquals(expectedBoard, actualBoard)
            },
            {
                val board = tigersMouth.fold(Board.empty(19)) { board, point ->
                    board.placeStone(SgfColor.White, point)
                }.placeStone(SgfColor.Black, SgfPoint(2, 2))

                val actualBoard = board.placeStone(SgfColor.White, SgfPoint(2, 3))
                val expectedBoard = ponnuki.fold(Board.empty(19)) { b, point ->
                    b.placeStone(SgfColor.White, point)
                }.copy(whiteCaptures = 1)

                assertEquals(expectedBoard, actualBoard)
            }
        )
    }

    @Test
    fun `placing a stone on-top of another stone replaces that stone on the board`() {
        val actual = Board.empty(19)
            .placeStone(SgfColor.Black, SgfPoint(3, 3))
            .placeStone(SgfColor.White, SgfPoint(3, 3))

        val expected = Board.empty(19).placeStone(SgfColor.White, SgfPoint(3, 3))
        assertEquals(expected, actual)
    }

    @Test
    fun `isOccupied returns true if there is a stone on that point`() {
        val point = SgfPoint(3, 3)
        val board = Board.empty(19).placeStone(SgfColor.Black, SgfPoint(3, 3))
        assertTrue(board.isOccupied(point))
    }

    @Test
    fun `isOccupied returns false if there is no stone on that point`() {
        assertAll(
            {
                assertFalse(Board.empty(19).isOccupied(SgfPoint(3, 3)))
            },
            {
                val board = Board.empty(19).placeStone(SgfColor.Black, SgfPoint(3, 3))
                assertFalse(board.isOccupied(SgfPoint(4, 3)))
            }
        )
    }
}
