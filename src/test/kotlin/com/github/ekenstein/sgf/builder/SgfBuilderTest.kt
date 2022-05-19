package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.extensions.newGame
import com.github.ekenstein.sgf.utils.nelOf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SgfBuilderTest {
    @Test
    fun `given a game tree one can change root information of the tree`() {
        val gameTree = SgfGameTree.newGame(19, 6.5, 0)
            .addProperty(SgfProperty.Root.SZ(19))
            .addProperty(SgfProperty.Root.GM(GameType.Go))
            .addProperty(SgfProperty.Move.B(3, 3))
        val actual = sgf(gameTree) {
            root {
                size(13)
            }
        }

        val expected = sgf {
            root {
                fileFormat(4)
                size(13)
                gameType(GameType.Go)
                gameInfo {
                    komi(6.5)
                }
            }

            move {
                stone(SgfColor.Black, 3, 3)
            }
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `can only have one move annotation per node`() {
        val actual = sgf {
            move {
                annotate(MoveAnnotation.Bad)
                annotate(MoveAnnotation.Doubtful)
            }
        }

        val expected = SgfGameTree(nelOf(SgfNode(SgfProperty.MoveAnnotation.DO)))
        assertEquals(expected, actual)
    }

    @Test
    fun `can only have one node annotation per node`() {
        val actual = sgf {
            move {
                annotate(NodeAnnotation.EvenPosition)
                annotate(NodeAnnotation.GoodForBlack)
            }
        }

        val expected = SgfGameTree(nelOf(SgfNode(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))))
        assertEquals(expected, actual)
    }

    @Test
    fun `adding a private property will update the old private property with the same identifier`() {
        val actual = sgf {
            move {
                property("APA", emptyList())
                property("BEPA", emptyList())
                property("APA", listOf("Hello"))
            }
        }

        val expected = SgfGameTree(
            nelOf(
                SgfNode(
                    SgfProperty.Private("APA", listOf("Hello")),
                    SgfProperty.Private("BEPA", emptyList())
                )
            )
        )

        assertEquals(expected, actual)
    }
}
