package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
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
}
