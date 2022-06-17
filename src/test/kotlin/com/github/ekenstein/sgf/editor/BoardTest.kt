package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.parser.from
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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

    @Test
    fun `if the board only contains black stones, the territory all belongs to black`() {
        val board = Board.empty(19).placeStone(SgfColor.Black, SgfPoint(10, 10))
        val expectedTerritory = board.emptyIntersections - SgfPoint(10, 10)
        val expected = mapOf(SgfColor.Black to expectedTerritory)
        val actual = board.getTerritories()
        assertEquals(expected, actual)

        val komi = 6.5
        val expectedScore = expectedTerritory.count() - komi
        assertEquals(expectedScore, board.count(komi))
    }

    @Test
    fun `splitting the board with black and white stones maps out the corresponding territories`() {
        val blackBorder = (1..19).associate { SgfPoint(10, it) to SgfColor.Black }
        val whiteBorder = (1..19).associate { SgfPoint(9, it) to SgfColor.White }
        val board = Board.empty(19).copy(stones = blackBorder + whiteBorder)

        val expectedWhiteTerritory = board.emptyIntersections.filter { (x, _) -> x < 9 }.toSet()
        val expectedBlackTerritory = board.emptyIntersections.filter { (x, _) -> x > 10 }.toSet()

        val expected = mapOf(
            SgfColor.White to expectedWhiteTerritory,
            SgfColor.Black to expectedBlackTerritory
        )

        val actual = board.getTerritories()
        assertEquals(expected, actual)
    }

    @Test
    fun `dame doesn't count as territory`() {
        val sgf = "(;SZ[5]AW[cd][dd][ed][ce]AB[ca][cb][db][eb])"

        val tree = SgfCollection.from(sgf).trees.head
        val board = SgfEditor(tree).extractBoard()

        val expectedWhiteTerritory = setOf(
            SgfPoint(4, 5),
            SgfPoint(5, 5)
        )

        val expectedBlackTerritory = setOf(
            SgfPoint(4, 1),
            SgfPoint(5, 1)
        )

        val expected = mapOf(
            SgfColor.White to expectedWhiteTerritory,
            SgfColor.Black to expectedBlackTerritory
        )

        val actual = board.getTerritories()
        assertEquals(expected, actual)
    }

    @Test
    fun `rotating a board left four times will give back the initial position`() {
        val sgf = "(;SZ[5]AW[cd][dd][ed][ce]AB[ca][cb][db][eb])"

        val tree = SgfCollection.from(sgf).trees.head
        val board = SgfEditor(tree).extractBoard()
        val actual = (1..4).fold(board) { b, _ ->
            b.rotateLeft()
        }

        assertEquals(board, actual)
    }

    @Test
    fun `rotating an empty board to the left returns the initial position`() {
        val board = Board.empty(19)
        val actual = board.rotateLeft()
        assertEquals(board, actual)
    }

    @Test
    fun `rotating tengen returns tengen`() {
        val board = Board.empty(19).placeStone(SgfColor.Black, SgfPoint(10, 10))
        val actual = board.rotateLeft()
        assertEquals(board, actual)
    }

    @Test
    fun `the canonical hash is the same for each rotation of a board`() {
        val sgf = "(;SZ[5]AW[cd][dd][ed][ce]AB[ca][cb][db][eb])"

        val tree = SgfCollection.from(sgf).trees.head
        val board = SgfEditor(tree).extractBoard()

        val transposes = (1..4).map {
            board.rotateLeft().canonicalHash()
        }.toSet()

        assertEquals(1, transposes.size)
    }

    @Test
    fun `two equal boards has the same canonical hash`() {
        val sgf = "(;SZ[5]AW[cd][dd][ed][ce]AB[ca][cb][db][eb])"

        val tree = SgfCollection.from(sgf).trees.head
        val board1 = SgfEditor(tree).extractBoard()
        val board2 = SgfEditor(tree).extractBoard()

        assertEquals(board1.canonicalHash(), board2.canonicalHash())
    }

    @Test
    fun `two different board positions does not have the same canonical hash`() {
        val board1 = SgfEditor().placeStone(SgfColor.Black, 3, 3).extractBoard()
        val board2 = SgfEditor().placeStone(SgfColor.Black, 4, 4).extractBoard()
        assertNotEquals(board1.canonicalHash(), board2.canonicalHash())
    }

    @Test
    fun `two boards that are mirrored has the same canonical hash`() {
        val board1 = SgfEditor()
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 3, 6)
            .placeStone(SgfColor.Black, 6, 3)
            .extractBoard()

        val board2 = SgfEditor()
            .placeStone(SgfColor.Black, 16, 4)
            .placeStone(SgfColor.White, 17, 6)
            .placeStone(SgfColor.Black, 14, 3)
            .extractBoard()

        assertEquals(board1.canonicalHash(), board2.canonicalHash())
    }

    @Test
    fun `removing a group from the board increases the capture count of the opponent`() {
        val board = Board.empty(19)
            .placeStone(SgfColor.Black, SgfPoint(3, 3))
            .placeStone(SgfColor.Black, SgfPoint(3, 4))
            .placeStone(SgfColor.White, SgfPoint(3, 5))

        val boardWithoutBlack = board.removeGroup(SgfPoint(3, 3))
        val boardWithoutWhite = board.removeGroup(SgfPoint(3, 5))

        val expectedBoardWithoutBlack = board.copy(
            whiteCaptures = 2,
            stones = board.stones - setOf(SgfPoint(3, 3), SgfPoint(3, 4))
        )

        val expectedBoardWithoutWhite = board.copy(
            blackCaptures = 1,
            stones = board.stones - setOf(SgfPoint(3, 5))
        )

        assertEquals(expectedBoardWithoutBlack, boardWithoutBlack)
        assertEquals(expectedBoardWithoutWhite, boardWithoutWhite)
    }
}
