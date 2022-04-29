package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SgfBuilderTest {
    @Test
    fun `given a game tree one can change root information of the tree`() {
        val gameTree = SgfGameTree.empty
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
                size(13)
                gameType(GameType.Go)
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

        val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.DO)
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

        val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
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
            listOf(
                SgfNode(
                    SgfProperty.Private("APA", listOf("Hello")),
                    SgfProperty.Private("BEPA", emptyList())
                )
            )
        )

        assertEquals(expected, actual)
    }
}
