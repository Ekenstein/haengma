package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.parser.from
import com.github.ekenstein.sgf.propertySetOf
import com.github.ekenstein.sgf.serialization.encodeToString
import com.github.ekenstein.sgf.utils.nelOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import utils.nextGameInfo
import utils.nextList
import utils.rng
import utils.run

class SgfEditorTest {
    @Test
    fun `playing a move will add it to the tree`() {
        val actualTree = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .commit()

        val expectedTree = SgfGameTree(
            nelOf(
                SgfNode(GameInfo.default.toSgfProperties()),
                SgfNode(SgfProperty.Move.B(3, 3))
            )
        )

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `doing a commit on an unaltered viewer will just return the initial tree`() {
        val actualTree = SgfEditor().commit()
        val expectedTree = SgfGameTree(nelOf(SgfNode(GameInfo.default.toSgfProperties())))
        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `viewer will always follow the latest path`() {
        val actualTree = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 5, 5)
            .commit()

        val expectedTree = SgfGameTree(
            nelOf(SgfNode(GameInfo.default.toSgfProperties())),
            listOf(
                SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Move.B(4, 4)),
                        SgfNode(SgfProperty.Move.W(5, 5))
                    )
                ),
                SgfGameTree(
                    nelOf(SgfNode(SgfProperty.Move.B(3, 3)))
                )
            )
        )

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `viewer will always follow the latest path, even in deep nested variations`() {
        val actualTree = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .placeStone(SgfColor.White, 5, 5)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 6, 6)
            .placeStone(SgfColor.Black, 7, 7)
            .commit()

        val expectedTree = SgfGameTree(
            nelOf(SgfNode(GameInfo.default.toSgfProperties())),
            listOf(
                SgfGameTree(
                    nelOf(SgfNode(SgfProperty.Move.B(4, 4))),
                    listOf(
                        SgfGameTree(
                            nelOf(
                                SgfNode(SgfProperty.Move.W(6, 6)),
                                SgfNode(SgfProperty.Move.B(7, 7))
                            )
                        ),
                        SgfGameTree(
                            nelOf(SgfNode(SgfProperty.Move.W(5, 5)))
                        )
                    )
                ),
                SgfGameTree(
                    nelOf(SgfNode(SgfProperty.Move.B(3, 3)))
                )
            )
        )

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `backing to previous node when there is no previous node in the variation will navigate to parent`() {
        val actualTree = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .placeStone(SgfColor.White, 4, 4)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 5, 5)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.White, 4, 4)
            .placeStone(SgfColor.Black, 5, 5)
            .commit()

        val expectedTree = SgfGameTree(
            nelOf(
                SgfNode(GameInfo.default.toSgfProperties()),
                SgfNode(SgfProperty.Move.B(3, 3))
            ),
            listOf(
                SgfGameTree(
                    nelOf(SgfNode(SgfProperty.Move.W(5, 5)))
                ),
                SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Move.W(4, 4)),
                        SgfNode(SgfProperty.Move.B(5, 5))
                    )
                )
            )
        )
        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `if there is no next node it will go to the first child tree`() {
        val actualTree = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .goToPreviousNodeOrStay()
            .goToNextNodeOrStay()
            .placeStone(SgfColor.White, 5, 5)
            .commit()

        val expectedTree = SgfGameTree(
            nelOf(SgfNode(GameInfo.default.toSgfProperties())),
            listOf(
                SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Move.B(4, 4)),
                        SgfNode(SgfProperty.Move.W(5, 5))
                    )
                ),
                SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Move.B(3, 3)),
                    )
                ),
            )
        )
        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `if there is no previous node and no parent, the operation is a no-op`() {
        val actualTree = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .goToPreviousNodeOrStay()
            .goToPreviousNodeOrStay()
            .placeStone(SgfColor.Black, 4, 4)
            .commit()

        val expectedTree = SgfGameTree(
            nelOf(
                SgfNode(GameInfo.default.toSgfProperties()),
            ),
            listOf(
                SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(4, 4)))),
                SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(3, 3))))
            )
        )

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `if there is no next node and no child tree, the operation is a no-op`() {
        val actualTree = SgfEditor()
            .placeStone(SgfColor.Black, 3, 3)
            .goToNextNodeOrStay()
            .placeStone(SgfColor.White, 4, 4)
            .commit()

        val expectedTree = SgfGameTree(
            nelOf(
                SgfNode(GameInfo.default.toSgfProperties()),
                SgfNode(SgfProperty.Move.B(3, 3)),
                SgfNode(SgfProperty.Move.W(4, 4))
            )
        )

        assertEquals(expectedTree, actualTree)
    }

    @Test
    fun `navigating without changing anything is a no-op`() {
        val expectedTree = SgfEditor()
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
            { it.goToNextNodeOrStay() },
            { it.goToLeftMostChildTreeOrStay() },
            { it.goToNextTreeOrStay() },
            { it.goToPreviousTreeOrStay() },
            { it.goToParentTreeOrStay() }
        )

        val apply = rng.nextList(operations)

        val actualTree = apply.fold(SgfEditor(expectedTree)) { editor, operation ->
            operation(editor)
        }.commit()

        assertEquals(expectedTree, actualTree)
    }

    @Nested
    inner class `Executing moves` {
        @Test
        fun `It's illegal to play at an occupied point`() {
            assertAll(
                {
                    val editor = SgfEditor().placeStone(SgfColor.Black, 3, 3)
                    assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, 3, 3) }
                },
                {
                    val editor = SgfEditor {
                        rules.handicap = 9
                    }
                    val occupiedPoints = listOf(
                        4 to 4, 10 to 4, 16 to 4,
                        4 to 10, 10 to 10, 16 to 10,
                        4 to 16, 10 to 16, 16 to 16
                    )

                    assertAll(
                        occupiedPoints.map { (x, y) ->
                            { assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, x, y) } }
                        }
                    )
                },
                {
                    val editor = SgfEditor {
                        rules.handicap = 9
                        rules.boardSize = 13
                    }
                    val occupiedPoints = listOf(
                        4 to 4, 7 to 4, 10 to 4,
                        4 to 7, 7 to 7, 10 to 7,
                        4 to 10, 7 to 10, 10 to 10
                    )

                    assertAll(
                        occupiedPoints.map { (x, y) ->
                            { assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, x, y) } }
                        }
                    )
                },
                {
                    val editor = SgfEditor {
                        rules.handicap = 9
                        rules.boardSize = 9
                    }
                    val occupiedPoints = listOf(
                        3 to 3, 5 to 3, 7 to 3,
                        3 to 5, 5 to 5, 7 to 5,
                        3 to 7, 5 to 7, 7 to 7
                    )

                    assertAll(
                        occupiedPoints.map { (x, y) ->
                            { assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, x, y) } }
                        }
                    )
                }
            )
        }

        @Test
        fun `It's illegal to play at a point that is suicide`() {
            assertThrows<SgfException.IllegalMove> {
                SgfEditor()
                    .addStones(SgfColor.Black, SgfPoint(1, 1), SgfPoint(2, 2), SgfPoint(3, 1))
                    .setNextToPlay(SgfColor.White)
                    .placeStone(SgfColor.White, 2, 1)
            }
        }

        @Test
        fun `It's illegal to play outside the board`() {
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

            assertAll(
                illegalPoints.map {
                    {
                        assertThrows<SgfException.IllegalMove> {
                            SgfEditor().placeStone(SgfColor.Black, it.x, it.y)
                        }
                        assertThrows<SgfException.IllegalMove> {
                            SgfEditor().placeStone(SgfColor.Black, it.x, it.y, true)
                        }
                        assertThrows<SgfException.IllegalMove> {
                            SgfEditor().setNextToPlay(SgfColor.White).placeStone(SgfColor.White, it.x, it.y)
                        }
                        assertThrows<SgfException.IllegalMove> {
                            SgfEditor().setNextToPlay(SgfColor.White).placeStone(SgfColor.White, it.x, it.y, true)
                        }
                    }
                }
            )
        }

        @Test
        fun `It's illegal to play on the opponent's turn`() {
            assertAll(
                {
                    assertThrows<SgfException.IllegalMove> {
                        SgfEditor().setNextToPlay(SgfColor.White).placeStone(SgfColor.Black, 3, 3)
                    }
                },
                {
                    assertThrows<SgfException.IllegalMove> {
                        SgfEditor().placeStone(SgfColor.White, 3, 3)
                    }
                },
                {
                    assertThrows<SgfException.IllegalMove> {
                        SgfEditor().placeStone(SgfColor.Black, 3, 3).placeStone(SgfColor.Black, 4, 4)
                    }
                },
                {
                    assertThrows<SgfException.IllegalMove> {
                        SgfEditor {
                            rules.handicap = 2
                        }.placeStone(SgfColor.Black, 3, 3)
                    }
                },
                {
                    assertThrows<SgfException.IllegalMove> {
                        SgfEditor {
                            rules.handicap = 9
                        }.setNextToPlay(SgfColor.Black).placeStone(SgfColor.White, 3, 3)
                    }
                },
                {
                    assertThrows<SgfException.IllegalMove> {
                        SgfEditor {
                            rules.handicap = 9
                        }.pass(SgfColor.Black)
                    }
                }
            )
        }

        @Test
        fun `it's illegal to re-take a ko after it has been initiated`() {
            val editor = SgfEditor()
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
                    val koContinues = editor
                        .placeStone(SgfColor.Black, 16, 4)
                        .placeStone(SgfColor.White, 17, 3)
                        .placeStone(SgfColor.Black, 6, 4)

                    val board = koContinues.extractBoard()
                    assertEquals(2, board.blackCaptures)
                    assertEquals(1, board.whiteCaptures)

                    assertThrows<SgfException.IllegalMove> {
                        koContinues.placeStone(SgfColor.White, 5, 4)
                    }
                }
            )
        }

        @Test
        fun `adding a move property to a node that has root properties will add the property to the next node`() {
            val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Root.SZ(19))))
            assertAll(
                {
                    val editor = SgfEditor(tree)
                        .goToLastNode()
                        .placeStone(SgfColor.Black, 3, 3)
                    val actualTree = editor.commit()
                    val expectedTree = tree.copy(
                        sequence = tree.sequence + SgfNode(SgfProperty.Move.B(3, 3))
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val editor = SgfEditor(tree)
                        .goToLastNode()
                        .placeStone(SgfColor.Black, 3, 3)
                        .goToPreviousNodeOrStay()
                        .placeStone(SgfColor.Black, 3, 3)

                    val actualTree = editor.commit()
                    val expectedTree = tree.copy(
                        sequence = tree.sequence + SgfNode(SgfProperty.Move.B(3, 3))
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val editor = SgfEditor(tree)
                        .goToLastNode()
                        .placeStone(SgfColor.Black, 3, 3)
                        .goToPreviousNodeOrStay()
                        .placeStone(SgfColor.Black, 10, 10)

                    val actualTree = editor.commit()
                    val expectedTree = tree.copy(
                        trees = listOf(
                            SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(10, 10)))),
                            SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(3, 3))))
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                }
            )
        }

        @Test
        fun `adding a move property to a node that has setup properties will add the property to the next node`() {
            val treeWithSetupProperty = SgfEditor()
                .setNextToPlay(SgfColor.White)
                .commit()

            assertAll(
                {
                    val editor = SgfEditor(treeWithSetupProperty)
                        .goToLastNode()
                        .placeStone(SgfColor.White, 3, 3)
                    val actualTree = editor.commit()
                    val expectedTree = treeWithSetupProperty.copy(
                        sequence = treeWithSetupProperty.sequence + SgfNode(SgfProperty.Move.W(3, 3))
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val editor = SgfEditor(treeWithSetupProperty)
                        .goToLastNode()
                        .placeStone(SgfColor.White, 3, 3)
                        .goToPreviousNodeOrStay()
                        .placeStone(SgfColor.White, 3, 3)

                    val actualTree = editor.commit()
                    val expectedTree = treeWithSetupProperty.copy(
                        sequence = treeWithSetupProperty.sequence + SgfNode(SgfProperty.Move.W(3, 3))
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val editor = SgfEditor(treeWithSetupProperty)
                        .goToLastNode()
                        .placeStone(SgfColor.White, 3, 3)
                        .goToPreviousNodeOrStay()
                        .placeStone(SgfColor.White, 10, 10)

                    val actualTree = editor.commit()
                    val expectedTree = treeWithSetupProperty.copy(
                        trees = listOf(
                            SgfGameTree(nelOf(SgfNode(SgfProperty.Move.W(10, 10)))),
                            SgfGameTree(nelOf(SgfNode(SgfProperty.Move.W(3, 3))))
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                }
            )
        }

        @Test
        fun `capturing a group increases the capture count`() {
            val sgf = "(;AW[cn][bo][ap][bp][aq][cq][dq][ar][dr][er][as][ds]" +
                "AB[cp][dp][ep][bq][eq][fq][gq][br][cr][fr][bs][es][fs])"

            val collection = SgfCollection.from(sgf)
            val tree = collection.trees.head
            val editor = SgfEditor(tree).goToLastNode()
            assertAll(
                {
                    val board = editor
                        .setNextToPlay(SgfColor.Black)
                        .placeStone(SgfColor.Black, 3, 19)
                        .extractBoard()

                    assertEquals(5, board.blackCaptures)
                },
                {
                    val board = editor
                        .setNextToPlay(SgfColor.White)
                        .placeStone(SgfColor.White, 3, 19)
                        .extractBoard()

                    assertEquals(4, board.whiteCaptures)
                }
            )
        }

        @Test
        fun `if placing a stone has force set to true, the move will be executed even if it's suicide`() {
            val editor = SgfEditor()
                .addStones(SgfColor.Black, SgfPoint(1, 1), SgfPoint(2, 2), SgfPoint(3, 1))
                .setNextToPlay(SgfColor.White)

            assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, 2, 1) }

            val nextPosition = editor.placeStone(SgfColor.White, 2, 1, true)

            val board = nextPosition.extractBoard()
            val expectedStones = mapOf(
                SgfPoint(1, 1) to SgfColor.Black,
                SgfPoint(2, 2) to SgfColor.Black,
                SgfPoint(3, 1) to SgfColor.Black,
            )

            assertEquals(expectedStones, board.stones)

            val rootProperties = GameInfo.default.toSgfProperties() + propertySetOf(
                SgfProperty.Setup.AB(setOf(SgfPoint(1, 1), SgfPoint(2, 2), SgfPoint(3, 1))),
                SgfProperty.Setup.PL(SgfColor.White)
            )

            val actualTree = nextPosition.commit()
            val expectedTree = SgfGameTree(
                nelOf(
                    SgfNode(rootProperties),
                    SgfNode(
                        SgfProperty.Move.W(2, 1),
                        SgfProperty.Move.KO
                    )
                )
            )
            assertEquals(expectedTree, actualTree)
        }

        @Test
        fun `if placing a stone has force set to true, the move will be executed even if the position is repeating`() {
            val blackStones = setOf(
                2 to 1,
                1 to 2,
                3 to 2,
                2 to 3
            ).map { (x, y) -> SgfPoint(x, y) }.toSet()

            val whiteStones = setOf(
                3 to 1,
                4 to 2,
                3 to 3
            ).map { (x, y) -> SgfPoint(x, y) }.toSet()

            val koPosition = SgfEditor()
                .addStones(SgfColor.Black, blackStones)
                .addStones(SgfColor.White, whiteStones)
                .placeStone(SgfColor.Black, 16, 4)
                .placeStone(SgfColor.White, 2, 2)

            assertThrows<SgfException.IllegalMove> {
                koPosition.placeStone(SgfColor.Black, 3, 2)
            }

            val nextPosition = koPosition.placeStone(SgfColor.Black, 3, 2, true)
            val rootProperties = GameInfo.default.toSgfProperties() + propertySetOf(
                SgfProperty.Setup.AB(blackStones),
                SgfProperty.Setup.AW(whiteStones)
            )

            val expectedGameTree = SgfGameTree(
                nelOf(
                    SgfNode(rootProperties),
                    SgfNode(SgfProperty.Move.B(16, 4)),
                    SgfNode(SgfProperty.Move.W(2, 2)),
                    SgfNode(SgfProperty.Move.B(3, 2), SgfProperty.Move.KO)
                )
            )

            val actualGameTree = nextPosition.commit()
            assertEquals(expectedGameTree, actualGameTree)

            val expectedStones = mapOf(
                SgfPoint(16, 4) to SgfColor.Black,
                SgfPoint(3, 2) to SgfColor.Black
            ) + blackStones.map { it to SgfColor.Black } + whiteStones.map { it to SgfColor.White }

            val actualStones = nextPosition.extractBoard().stones
            assertEquals(expectedStones, actualStones)
            assertEquals(SgfColor.White, nextPosition.nextToPlay())
        }

        @Test
        fun `if placing a stone has force set to true, the move will be executed even if the point is occupied`() {
            val editor = SgfEditor()
                .placeStone(SgfColor.Black, 3, 3)

            assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, 3, 3) }
            val nextPosition = editor.placeStone(SgfColor.White, 3, 3, true)
            val expectedGameTree = SgfGameTree(
                nelOf(
                    SgfNode(GameInfo.default.toSgfProperties()),
                    SgfNode(SgfProperty.Move.B(3, 3)),
                    SgfNode(SgfProperty.Move.W(3, 3), SgfProperty.Move.KO)
                )
            )
            val actualGameTree = nextPosition.commit()
            assertEquals(expectedGameTree, actualGameTree)

            val expectedStones = mapOf(SgfPoint(3, 3) to SgfColor.White)
            val actualStones = nextPosition.extractBoard().stones

            assertEquals(expectedStones, actualStones)
            assertEquals(SgfColor.Black, nextPosition.nextToPlay())
        }

        @Test
        fun `if placing a stone has force set to true, the move will be executed even if it's not the player's turn`() {
            assertAll(
                {
                    val editor = SgfEditor()
                    assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, 3, 3) }
                    val nextPosition = editor.placeStone(SgfColor.White, 3, 3, true)
                    val expectedGameTree = SgfGameTree(
                        nelOf(
                            SgfNode(GameInfo.default.toSgfProperties()),
                            SgfNode(SgfProperty.Move.W(3, 3), SgfProperty.Move.KO)
                        )
                    )

                    val actualGameTree = nextPosition.commit()
                    assertEquals(expectedGameTree, actualGameTree)
                    assertEquals(SgfColor.Black, nextPosition.nextToPlay())
                },
                {
                    val editor = SgfEditor().placeStone(SgfColor.Black, 3, 3).setNextToPlay(SgfColor.Black)
                    assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, 4, 4) }
                    val nextPosition = editor.placeStone(SgfColor.White, 4, 4, true)
                    val expectedGameTree = SgfGameTree(
                        nelOf(
                            SgfNode(GameInfo.default.toSgfProperties()),
                            SgfNode(SgfProperty.Move.B(3, 3)),
                            SgfNode(SgfProperty.Setup.PL(SgfColor.Black)),
                            SgfNode(SgfProperty.Move.W(4, 4), SgfProperty.Move.KO)
                        )
                    )
                    val actualGameTree = nextPosition.commit()
                    assertEquals(expectedGameTree, actualGameTree)
                    assertEquals(SgfColor.Black, nextPosition.nextToPlay())
                }
            )
        }

        @Test
        fun `if passing with force flag set to true, the pass will be executed even if it's not the player's turn`() {
            assertAll(
                {
                    val editor = SgfEditor()
                    assertThrows<SgfException.IllegalMove> { editor.pass(SgfColor.White) }
                    val nextPosition = editor.pass(SgfColor.White, true)
                    val expectedGameTree = SgfGameTree(
                        nelOf(
                            SgfNode(GameInfo.default.toSgfProperties()),
                            SgfNode(SgfProperty.Move.W(Move.Pass), SgfProperty.Move.KO)
                        )
                    )
                    val actualGameTree = nextPosition.commit()
                    assertEquals(expectedGameTree, actualGameTree)
                },
                {
                    val editor = SgfEditor().setNextToPlay(SgfColor.White)
                    assertThrows<SgfException.IllegalMove> { editor.pass(SgfColor.Black) }
                    val nextPosition = editor.pass(SgfColor.Black, true)
                    val expectedGameTree = SgfGameTree(
                        nelOf(
                            SgfNode(GameInfo.default.toSgfProperties() + SgfProperty.Setup.PL(SgfColor.White)),
                            SgfNode(SgfProperty.Move.B(Move.Pass), SgfProperty.Move.KO)
                        )
                    )

                    val actualGameTree = nextPosition.commit()
                    assertEquals(expectedGameTree, actualGameTree)

                    assertEquals(SgfColor.White, nextPosition.nextToPlay())
                }
            )
        }

        @Test
        fun `passed moves does not get included onto the extracted board`() {
            assertAll(
                {
                    val board = SgfEditor().pass(SgfColor.Black).extractBoard()
                    assertTrue(board.stones.isEmpty())
                },
                {
                    val board = SgfEditor().pass(SgfColor.Black).pass(SgfColor.White).extractBoard()
                    assertTrue(board.stones.isEmpty())
                }
            )
        }

        @Test
        fun `placing a stone at a node that has no root, setup, white or black props will update the current node`() {
            val tree = SgfGameTree(
                nelOf(
                    SgfNode(SgfProperty.Root.SZ(19)),
                    SgfNode(SgfProperty.Move.MN(1))
                )
            )

            val editor = SgfEditor(tree).goToLastNode().placeStone(SgfColor.Black, 3, 3)
            val actual = editor.commit()
            val expected = SgfGameTree(
                nelOf(
                    SgfNode(SgfProperty.Root.SZ(19)),
                    SgfNode(
                        SgfProperty.Move.MN(1),
                        SgfProperty.Move.B(3, 3)
                    )
                )
            )

            assertEquals(expected, actual)
        }

        @Test
        fun `the focus of the editor will remain the same if placing a stone that the current node already has`() {
            val tree = SgfGameTree(
                nelOf(
                    SgfNode(SgfProperty.Move.B(3, 3))
                )
            )
            val editor = SgfEditor(tree).placeStone(SgfColor.Black, 3, 3, true)
            val expectedCurrentNode = SgfNode(SgfProperty.Move.B(3, 3), SgfProperty.Move.KO)
            assertEquals(expectedCurrentNode, editor.currentNode)

            val expected = SgfGameTree(
                nelOf(
                    expectedCurrentNode
                )
            )

            val actual = editor.commit()

            assertEquals(expected, actual)
        }
    }

    @Nested
    inner class `Setting up positions` {
        @Test
        fun `stones added to the position must not be on the same node as move properties`() {
            assertAll(
                {
                    val tree = GameInfo.default.toGameTree()
                    val actualTree = SgfEditor(tree)
                        .placeStone(SgfColor.Black, 4, 4)
                        .addStones(SgfColor.Black, SgfPoint(4, 4), SgfPoint(5, 5))
                        .commit()

                    val expectedTree = tree.copy(
                        sequence = tree.sequence + nelOf(
                            SgfNode(SgfProperty.Move.B(4, 4)),
                            SgfNode(SgfProperty.Setup.AB(setOf(SgfPoint(4, 4), SgfPoint(5, 5))))
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val gameInfo = GameInfo.default
                    val actualTree = SgfEditor(gameInfo)
                        .placeStone(SgfColor.Black, 4, 4)
                        .placeStone(SgfColor.White, 5, 5)
                        .goToPreviousNodeOrStay()
                        .addStones(SgfColor.Black, SgfPoint(5, 5))
                        .goToPreviousNodeOrStay()
                        .addStones(SgfColor.White, SgfPoint(6, 6))
                        .commit()

                    val expectedTree = SgfGameTree(
                        nelOf(
                            SgfNode(gameInfo.toSgfProperties()),
                            SgfNode(SgfProperty.Move.B(4, 4))
                        ),
                        listOf(
                            SgfGameTree(
                                nelOf(SgfNode(SgfProperty.Setup.AW(setOf(SgfPoint(6, 6)))))
                            ),
                            SgfGameTree(
                                nelOf(SgfNode(SgfProperty.Setup.AB(setOf(SgfPoint(5, 5)))))
                            ),
                            SgfGameTree(
                                nelOf(SgfNode(SgfProperty.Move.W(5, 5)))
                            )
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val gameInfo = GameInfo.default
                    val actualTree = SgfEditor(gameInfo)
                        .placeStone(SgfColor.Black, 4, 4)
                        .addStones(SgfColor.White, SgfPoint(7, 3))
                        .placeStone(SgfColor.White, 5, 5)
                        .commit()

                    val expectedTree = SgfGameTree(
                        nelOf(
                            SgfNode(gameInfo.toSgfProperties()),
                            SgfNode(SgfProperty.Move.B(4, 4)),
                            SgfNode(SgfProperty.Setup.AW(setOf(SgfPoint(7, 3)))),
                            SgfNode(SgfProperty.Move.W(5, 5))
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val gameInfo = GameInfo.default
                    val actualTree = SgfEditor(gameInfo)
                        .placeStone(SgfColor.Black, 4, 4)
                        .addStones(SgfColor.Black, SgfPoint(7, 3))
                        .placeStone(SgfColor.White, 5, 5)
                        .commit()

                    val expectedTree = SgfGameTree(
                        nelOf(
                            SgfNode(gameInfo.toSgfProperties()),
                            SgfNode(SgfProperty.Move.B(4, 4)),
                            SgfNode(SgfProperty.Setup.AB(setOf(SgfPoint(7, 3)))),
                            SgfNode(SgfProperty.Move.W(5, 5))
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                }
            )
        }

        @Test
        fun `setup properties will merge their points`() {
            assertAll(
                {
                    val gameInfo = GameInfo.default
                    val actualTree = SgfEditor(gameInfo)
                        .addStones(SgfColor.Black, SgfPoint(4, 4))
                        .addStones(SgfColor.Black, SgfPoint(5, 5))
                        .commit()

                    val properties = gameInfo.toSgfProperties() + SgfProperty.Setup.AB(
                        setOf(
                            SgfPoint(4, 4),
                            SgfPoint(5, 5)
                        )
                    )
                    val expectedTree = SgfGameTree(nelOf(SgfNode(properties)))

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val actualTree = SgfEditor()
                        .placeStone(SgfColor.Black, 4, 4)
                        .addStones(SgfColor.White, SgfPoint(4, 4))
                        .addStones(SgfColor.Black, SgfPoint(5, 5))
                        .addStones(SgfColor.White, SgfPoint(6, 6))
                        .commit()

                    val expectedTree = SgfGameTree(
                        nelOf(
                            SgfNode(GameInfo.default.toSgfProperties()),
                            SgfNode(SgfProperty.Move.B(4, 4)),
                            SgfNode(
                                SgfProperty.Setup.AB(setOf(SgfPoint(5, 5))),
                                SgfProperty.Setup.AW(setOf(SgfPoint(4, 4), SgfPoint(6, 6)))
                            )
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                },
                {
                    val actualTree = SgfEditor()
                        .placeStone(SgfColor.Black, 4, 4)
                        .removeStones(SgfPoint(4, 4))
                        .addStones(SgfColor.Black, SgfPoint(5, 5))
                        .removeStones(SgfPoint(6, 6))
                        .commit()

                    val expectedTree = SgfGameTree(
                        nelOf(
                            SgfNode(GameInfo.default.toSgfProperties()),
                            SgfNode(SgfProperty.Move.B(4, 4)),
                            SgfNode(
                                SgfProperty.Setup.AB(setOf(SgfPoint(5, 5))),
                                SgfProperty.Setup.AE(setOf(SgfPoint(4, 4), SgfPoint(6, 6)))
                            )
                        )
                    )

                    assertEquals(expectedTree, actualTree)
                }
            )
        }

        @Test
        fun `can place a stone on a point that has been cleared`() {
            val actualTree = SgfEditor(GameInfo.default)
                .placeStone(SgfColor.Black, 4, 4)
                .removeStones(SgfPoint(4, 4))
                .placeStone(SgfColor.White, 4, 4)
                .commit()

            val expectedTree = SgfGameTree(
                nelOf(
                    SgfNode(GameInfo.default.toSgfProperties()),
                    SgfNode(SgfProperty.Move.B(4, 4)),
                    SgfNode(SgfProperty.Setup.AE(setOf(SgfPoint(4, 4)))),
                    SgfNode(SgfProperty.Move.W(4, 4))
                )
            )

            assertEquals(expectedTree, actualTree)
        }

        @Test
        fun `can't place a stone on an added stone`() {
            val editor = SgfEditor()
                .placeStone(SgfColor.Black, 4, 4)
                .addStones(SgfColor.Black, SgfPoint(5, 5), SgfPoint(6, 6))
                .removeStones(SgfPoint(5, 5))

            assertThrows<SgfException.IllegalMove> { editor.placeStone(SgfColor.White, 6, 6) }
        }

        @Test
        fun `setup properties can be mixed on the same node`() {
            val actualTree = SgfEditor()
                .placeStone(SgfColor.Black, 4, 4)
                .addStones(SgfColor.Black, SgfPoint(5, 5))
                .addStones(SgfColor.White, SgfPoint(6, 6))
                .removeStones(SgfPoint(7, 7))
                .commit()

            val expectedTree = SgfGameTree(
                nelOf(
                    SgfNode(GameInfo.default.toSgfProperties()),
                    SgfNode(SgfProperty.Move.B(4, 4)),
                    SgfNode(
                        SgfProperty.Setup.AB(setOf(SgfPoint(5, 5))),
                        SgfProperty.Setup.AW(setOf(SgfPoint(6, 6))),
                        SgfProperty.Setup.AE(setOf(SgfPoint(7, 7)))
                    )
                )
            )

            assertEquals(expectedTree, actualTree)
        }

        @Test
        fun `can set whose turn it is to play`() {
            val editor = SgfEditor()
                .placeStone(SgfColor.Black, 4, 4)
                .setNextToPlay(SgfColor.Black)
                .placeStone(SgfColor.Black, 5, 5)
                .placeStone(SgfColor.White, 6, 6)
                .goToPreviousNodeOrStay()
                .setNextToPlay(SgfColor.Black)
                .placeStone(SgfColor.Black, 6, 6)

            val expectedTree = SgfGameTree(
                nelOf(
                    SgfNode(GameInfo.default.toSgfProperties()),
                    SgfNode(SgfProperty.Move.B(4, 4)),
                    SgfNode(SgfProperty.Setup.PL(SgfColor.Black)),
                    SgfNode(SgfProperty.Move.B(5, 5)),
                ),
                listOf(
                    SgfGameTree(
                        nelOf(
                            SgfNode(SgfProperty.Setup.PL(SgfColor.Black)),
                            SgfNode(SgfProperty.Move.B(6, 6))
                        )
                    ),
                    SgfGameTree(
                        nelOf(
                            SgfNode(SgfProperty.Move.W(6, 6))
                        )
                    )
                )
            )

            assertEquals(expectedTree, editor.commit())
            assertThrows<SgfException.IllegalMove> {
                editor.goToPreviousNodeOrStay().placeStone(SgfColor.White, 9, 9)
            }
        }
    }

    @Nested
    inner class `Branching the tree` {
        @Test
        fun `undo played move and then play a move will create two branches`() {
            val actualTree = SgfEditor()
                .placeStone(SgfColor.Black, 3, 3)
                .goToPreviousNodeOrStay()
                .placeStone(SgfColor.Black, 4, 4)
                .commit()

            val expectedTree = SgfGameTree(
                nelOf(SgfNode(GameInfo.default.toSgfProperties())),
                listOf(
                    SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(4, 4)))),
                    SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(3, 3))))
                )
            )

            assertEquals(expectedTree, actualTree)
        }

        @Test
        fun `branching a sequence with children will preserve the children for the branched out sequence`() {
            val editor = SgfEditor()
                .placeStone(SgfColor.Black, 4, 4)
                .placeStone(SgfColor.White, 16, 4)
                .goToPreviousNodeOrStay()
                .placeStone(SgfColor.White, 16, 16)
                .placeStone(SgfColor.Black, 16, 4)
                .goToRootNode()
                .placeStone(SgfColor.Black, 3, 3)
                .placeStone(SgfColor.White, 16, 4)

            val actualTree = editor.commit()

            val expectedTree = SgfGameTree(
                sequence = nelOf(SgfNode(GameInfo.default.toSgfProperties())),
                trees = listOf(
                    SgfGameTree(
                        nelOf(
                            SgfNode(SgfProperty.Move.B(3, 3)),
                            SgfNode(SgfProperty.Move.W(16, 4))
                        )
                    ),
                    SgfGameTree(
                        nelOf(SgfNode(SgfProperty.Move.B(4, 4))),
                        listOf(
                            SgfGameTree(
                                nelOf(
                                    SgfNode(SgfProperty.Move.W(16, 16)),
                                    SgfNode(SgfProperty.Move.B(16, 4))
                                )
                            ),
                            SgfGameTree(
                                nelOf(SgfNode(SgfProperty.Move.W(16, 4)))
                            )
                        )
                    )
                )
            )

            assertEquals(expectedTree, actualTree)
        }

        @Test
        fun `always branch out a move if there are no moves to the right and the children is not empty`() {
            // you can create your own tree
            val editor = SgfEditor { rules.handicap = 2 }
            val actualTree = editor
                .placeStone(SgfColor.White, 17, 3)
                .placeStone(SgfColor.Black, 16, 3)
                .placeStone(SgfColor.White, 17, 4)
                .placeStone(SgfColor.Black, 17, 5)
                .goToPreviousNodeOrStay()
                .placeStone(SgfColor.Black, 16, 5)
                .goToPreviousNodeOrStay()
                .placeStone(SgfColor.Black, 4, 4)
                .goToRootNode()
                .placeStone(SgfColor.White, 16, 16)
                .commit()

            val expectedTree = SgfGameTree(
                nelOf(SgfNode(editor.getGameInfo().toSgfProperties())),
                listOf(
                    SgfGameTree(nelOf(SgfNode(SgfProperty.Move.W(16, 16)))),
                    SgfGameTree(
                        nelOf(
                            SgfNode(SgfProperty.Move.W(17, 3)),
                            SgfNode(SgfProperty.Move.B(16, 3)),
                            SgfNode(SgfProperty.Move.W(17, 4))
                        ),
                        listOf(
                            SgfGameTree(
                                nelOf(
                                    SgfNode(SgfProperty.Move.B(4, 4))
                                )
                            ),
                            SgfGameTree(
                                nelOf(
                                    SgfNode(SgfProperty.Move.B(16, 5))
                                )
                            ),
                            SgfGameTree(
                                nelOf(
                                    SgfNode(SgfProperty.Move.B(17, 5))
                                )
                            )
                        )
                    )
                )
            )

            assertEquals(expectedTree, actualTree, actualTree.encodeToString())
        }
    }

    @Test
    fun `can always retrieve game info from the editor`() {
        rng.run(100) {
            val gameInfo = nextGameInfo()
            val editor = SgfEditor(gameInfo)
            val actualGameInfo = editor.getGameInfo()
            assertEquals(gameInfo, actualGameInfo)

            val expectedTree = SgfGameTree(nelOf(SgfNode(gameInfo.toSgfProperties())))
            val actualTree = editor.commit()
            assertEquals(expectedTree, actualTree)
        }
        assertAll(
            {
                val editor = SgfEditor(GameInfo.default)
                val actualGameInfo = editor.getGameInfo()
                assertEquals(GameInfo.default, actualGameInfo)
            },
            {
                val gameTree = SgfGameTree(nelOf(SgfNode(SgfProperty.Setup.PL(SgfColor.Black))))
                val editor = SgfEditor(gameTree)
                val actualGameInfo = editor.getGameInfo()
                assertEquals(GameInfo.default, actualGameInfo)
            },
            {
                val gameInfo = GameInfo.default.apply {
                    rules.komi = 6.5
                    rules.boardSize = 9
                }

                val editor = SgfEditor(gameInfo)
                val actualGameInfo = editor.getGameInfo()
                assertEquals(gameInfo, actualGameInfo)
            },
            {
                val editor = SgfEditor(GameInfo.default).placeStone(SgfColor.Black, 1, 1)
                val actualGameInfo = editor.getGameInfo()
                assertEquals(GameInfo.default, actualGameInfo)
            }
        )
    }
}
