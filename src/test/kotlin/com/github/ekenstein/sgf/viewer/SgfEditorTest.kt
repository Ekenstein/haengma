package com.github.ekenstein.sgf.viewer

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.builder.sgf
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.extensions.newGame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import utils.nextList
import utils.rng
import kotlin.test.assertEquals

class SgfEditorTest {
    @Test
    fun `playing a move will add it to the tree`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .commit()

        val expectedTree = sgf(tree) {
            move {
                stone(SgfColor.Black, 3, 3)
            }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `doing a commit on an unaltered viewer will just return the initial tree`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree).commit()
        assertEquals(tree, actualTree)
    }

    @Test
    fun `viewer will always follow the latest path`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 5, 5)
            .commit()

        val expectedTree = sgf(tree) {
            variation {
                move { stone(SgfColor.Black, 4, 4) }
                move { stone(SgfColor.White, 5, 5) }
            }

            variation {
                move { stone(SgfColor.Black, 3, 3) }
            }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `viewer will always follow the latest path, even in deep nested variations`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 5, 5)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 6, 6)
            .placeStone(SgfColor.Black, 7, 7)
            .commit()

        val expectedTree = sgf(tree) {
            variation {
                move { stone(SgfColor.Black, 4, 4) }
                variation {
                    move { stone(SgfColor.White, 6, 6) }
                    move { stone(SgfColor.Black, 7, 7) }
                }
                variation {
                    move { stone(SgfColor.White, 5, 5) }
                }
            }
            variation {
                move { stone(SgfColor.Black, 3, 3) }
            }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `undo played move and then play a move will create two branches`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .commit()

        val expectedTree = sgf(tree) {
            variation {
                move {
                    stone(SgfColor.Black, 4, 4)
                }
            }
            variation {
                move {
                    stone(SgfColor.Black, 3, 3)
                }
            }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `backing to previous node when there is no previous node in the variation will navigate to parent`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .placeStone(SgfColor.White, 4, 4)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 5, 5)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 4, 4)
            .placeStone(SgfColor.Black, 5, 5)
            .commit()

        val expectedTree = sgf(tree) {
            move { stone(SgfColor.Black, 3, 3) }

            variation {
                move { stone(SgfColor.White, 5, 5) }
            }
            variation {
                move { stone(SgfColor.White, 4, 4) }
                move { stone(SgfColor.Black, 5, 5) }
            }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `if there is no next node it will go to the first child tree`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .goToPreviousNodeOrStay()
            .goToNextNodeOrStay()
            .placeStone(SgfColor.White, 5, 5)
            .commit()

        val expectedTree = sgf(tree) {
            variation {
                move { stone(SgfColor.Black, 4, 4) }
                move { stone(SgfColor.White, 5, 5) }
            }

            variation {
                move { stone(SgfColor.Black, 3, 3) }
            }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `if there is no previous node and no parent, the operation is a no-op`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .commit()

        val expectedTree = sgf(tree) {
            variation { move { stone(SgfColor.Black, 4, 4) } }
            variation { move { stone(SgfColor.Black, 3, 3) } }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `if there is no next node and no child tree, the operation is a no-op`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 3, 3)
            .goToNextNodeOrStay()
            .placeStone(SgfColor.White, 4, 4)
            .commit()

        val expectedTree = sgf(tree) {
            move { stone(SgfColor.Black, 3, 3) }
            move { stone(SgfColor.White, 4, 4) }
        }
        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `PL can decide which color to play next`() {
        val tree = SgfEditor(SgfGameTree.newGame(19, 6.5, 0))
            .placeStone(SgfColor.Black, 3, 3)
            .commit()

        val actualTree = SgfEditor(tree.addProperty(SgfProperty.Setup.PL(SgfColor.Black)))
            .goToLastNode()
            .placeStone(SgfColor.Black, 4, 4)
            .commit()

        val expectedTree = sgf(tree) {
            setup {
                colorToPlay(SgfColor.Black)
            }

            move { stone(SgfColor.Black, 4, 4) }
        }

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `navigating without changing anything is a no-op`() {
        val expectedTree = SgfEditor(SgfGameTree.newGame(19, 6.5, 0))
            .placeStone(SgfColor.Black, 3, 3)
            .placeStone(SgfColor.White, 4, 4)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 5, 5)
            .placeStone(SgfColor.Black, 6, 6)
            .goToRootNode()
            .placeStone(SgfColor.Black, 8, 8)
            .commit()

        val operations: List<(SgfEditor) -> SgfEditor> = listOf(
            { it.goToRootNode() },
            { it.goToLastNode() },
            { it.goToPreviousNodeOrStay() },
            { it.goToNextNodeOrStay() }
        )

        val apply = rng.nextList(operations)

        val actualTree = apply.fold(SgfEditor(expectedTree)) { editor, operation ->
            operation(editor)
        }.commit()

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `It's illegal to play at an occupied point`() {
        assertAll(
            {
                val tree = SgfGameTree.newGame(19, 6.5, 0)
                val editor = SgfEditor(tree).placeStone(SgfColor.Black, 3, 3)
                assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, 3, 3) }
            },
            {
                val tree = SgfGameTree.newGame(19, 6.5, 9)
                val occupiedPoints = listOf(
                    4 to 4, 10 to 4, 16 to 4,
                    4 to 10, 10 to 10, 16 to 10,
                    4 to 16, 10 to 16, 16 to 16
                )

                assertAll(
                    occupiedPoints.map { (x, y) ->
                        { assertThrows<SgfException.IllegalMove> { SgfEditor(tree).placeStone(SgfColor.White, x, y) } }
                    }
                )
            },
            {
                val tree = SgfGameTree.newGame(13, 6.5, 9)
                val occupiedPoints = listOf(
                    4 to 4, 7 to 4, 10 to 4,
                    4 to 7, 7 to 7, 10 to 7,
                    4 to 10, 7 to 10, 10 to 10
                )

                assertAll(
                    occupiedPoints.map { (x, y) ->
                        { assertThrows<SgfException.IllegalMove> { SgfEditor(tree).placeStone(SgfColor.White, x, y) } }
                    }
                )
            },
            {
                val tree = SgfGameTree.newGame(13, 6.5, 9)
                val occupiedPoints = listOf(
                    4 to 4, 7 to 4, 10 to 4,
                    4 to 7, 7 to 7, 10 to 7,
                    4 to 10, 7 to 10, 10 to 10
                )

                assertAll(
                    occupiedPoints.map { (x, y) ->
                        { assertThrows<SgfException.IllegalMove> { SgfEditor(tree).placeStone(SgfColor.White, x, y) } }
                    }
                )
            },
            {
                val tree = SgfGameTree.newGame(9, 6.5, 9)
                val occupiedPoints = listOf(
                    3 to 3, 5 to 3, 7 to 3,
                    3 to 5, 5 to 5, 7 to 5,
                    3 to 7, 5 to 7, 7 to 7
                )

                assertAll(
                    occupiedPoints.map { (x, y) ->
                        { assertThrows<SgfException.IllegalMove> { SgfEditor(tree).placeStone(SgfColor.White, x, y) } }
                    }
                )
            }
        )
    }

    @Test
    fun `It's illegal to play at a point that is suicide`() {
        val tree = SgfGameTree.newGame(19, 6.5, 0)
            .addProperty(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1), SgfPoint(2, 2), SgfPoint(3, 1))))
            .addProperty(SgfProperty.Setup.PL(SgfColor.White))

        assertThrows<SgfException.IllegalMove> { SgfEditor(tree).placeStone(SgfColor.White, 2, 1) }
    }

    @Test
    fun `It's illegal to play on the opponent's turn`() {
        assertAll(
            {
                val tree = SgfGameTree.newGame(19, 6.5, 0)
                    .addProperty(SgfProperty.Setup.PL(SgfColor.White))

                assertThrows<SgfException.IllegalMove> {
                    SgfEditor(tree).placeStone(SgfColor.Black, 3, 3)
                }
            },
            {
                val tree = SgfGameTree.newGame(19, 6.5, 0)
                assertThrows<SgfException.IllegalMove> {
                    SgfEditor(tree).placeStone(SgfColor.White, 3, 3)
                }
            },
            {
                val editor = SgfEditor(SgfGameTree.newGame(19, 6.5, 0))
                    .placeStone(SgfColor.Black, 3, 3)

                assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.Black, 3, 3) }
            },
            {
                val editor = SgfEditor(SgfGameTree.newGame(19, 6.5, 2))
                assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.Black, 3, 3) }
            },
            {
                val tree = SgfGameTree.newGame(19, 6.5, 9)
                    .addProperty(SgfProperty.Setup.PL(SgfColor.Black))

                assertThrows<SgfException.IllegalMove> { SgfEditor(tree).placeStone(SgfColor.White, 3, 3) }
            },
            {
                val tree = SgfGameTree.newGame(19, 6.5, 9)
                assertThrows<SgfException.IllegalMove> { SgfEditor(tree).pass(SgfColor.Black) }
            }
        )
    }

    @Test
    fun `branching a sequence with children will preserve the children for the branched out sequence`() {
        val tree = SgfGameTree.newGame(boardSize = 19, komi = 6.5, handicap = 0)
        val actualTree = SgfEditor(tree)
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 16, 4)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 16, 16)
            .placeStone(SgfColor.Black, 16, 4)
            .goToRootNode()
            .placeStone(SgfColor.Black, 3, 3)
            .placeStone(SgfColor.White, 16, 4)

        val expectedTree = sgf(tree) {
            variation {
                move { stone(SgfColor.Black, 3, 3) }
                move { stone(SgfColor.White, 16, 4) }
            }
            variation {
                move { stone(SgfColor.Black, 4, 4) }
                variation {
                    move { stone(SgfColor.White, 16, 16) }
                    move { stone(SgfColor.Black, 16, 4) }
                }
                variation {
                    move { stone(SgfColor.White, 16, 4) }
                }
            }
        }

        assertEquals(expectedTree, actualTree.commit())
    }

    @Test
    fun `it's illegal to re-take a ko after it has been initiated`() {
        val editor = SgfEditor(SgfGameTree.newGame(19, 6.5, 0))
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 5, 4)
            .placeStone(SgfColor.Black, 5, 3)
            .placeStone(SgfColor.White, 6, 3)
            .placeStone(SgfColor.Black, 5, 5)
            .placeStone(SgfColor.White, 6, 5)
            .placeStone(SgfColor.Black, 6, 4)
            .placeStone(SgfColor.White, 7, 4)
            .placeStone(SgfColor.Black, 7, 3)
            .placeStone(SgfColor.White, 5, 4)

        assertAll(
            {
                assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.Black, 6, 4) }
            },
            {
                val board = assertDoesNotThrow {
                    editor
                        .placeStone(SgfColor.Black, 16, 4)
                        .placeStone(SgfColor.White, 17, 3)
                        .placeStone(SgfColor.Black, 6, 4)
                        .board
                }

                assertEquals(2, board.blackCaptures)
                assertEquals(1, board.whiteCaptures)
                println(board.print())
            }
        )
    }
}
