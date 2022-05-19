package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.utils.nelOf
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SgfExtensionsTest {
    @Test
    fun `adding a property of type root to the game tree when there is no root node will add a root node`() {
        assertAll(
            {
                val property = SgfProperty.Root.GM(GameType.Go)
                val initialNode = SgfNode(setOf(SgfProperty.Move.B(3, 3)))
                val tree = SgfGameTree(nelOf(initialNode)).addProperty(property)
                val expected = SgfGameTree(nelOf(SgfNode(setOf(property)), initialNode), emptyList())
                assertEquals(expected, tree)
            },
            {
                val initialNode = SgfNode(setOf(SgfProperty.Move.B(1, 1)))
                val tree = SgfGameTree(nelOf(initialNode))
                val actual = tree
                    .addProperty(SgfProperty.Root.FF(4))
                    .addProperty(SgfProperty.Root.GM(GameType.Go))

                val expected = SgfGameTree(
                    nelOf(
                        SgfNode(setOf(SgfProperty.Root.FF(4), SgfProperty.Root.GM(GameType.Go))),
                        initialNode
                    )
                )

                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `adding a property of type root to the game tree will replace the existing property in the root node`() {
        val property = SgfProperty.Root.GM(GameType.Go)
        val tree = SgfGameTree(
            nelOf(
                SgfNode(
                    SgfProperty.Root.GM(GameType.Chess),
                    SgfProperty.Root.CA(Charsets.UTF_8)
                ),
                SgfNode(SgfProperty.Move.B(Move.Stone(SgfPoint(1, 1))))
            )
        )

        val newTree = tree.addProperty(property)
        val expected = SgfGameTree(
            nelOf(
                SgfNode(
                    SgfProperty.Root.GM(GameType.Go),
                    SgfProperty.Root.CA(Charsets.UTF_8)
                ),
                SgfNode(SgfProperty.Move.B(Move.Stone(SgfPoint(1, 1))))
            )
        )

        assertEquals(expected, newTree)
    }

    @Test
    fun `game info properties will be added to the node with game info properties or the root node`() {
        assertAll(
            {
                val tree = SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Root.FF(4)),
                        SgfNode(SgfProperty.GameInfo.PC("Test"))
                    )
                )

                val actual = tree.addProperty(SgfProperty.GameInfo.CP("Copyright"))
                val expected = SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Root.FF(4)),
                        SgfNode(
                            SgfProperty.GameInfo.PC("Test"),
                            SgfProperty.GameInfo.CP("Copyright")
                        )
                    )
                )

                assertEquals(expected, actual)
            },
            {
                val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Root.FF(4))))

                val actual = tree.addProperty(SgfProperty.GameInfo.CP("Copyright"))
                val expected = SgfGameTree(
                    nelOf(
                        SgfNode(
                            SgfProperty.Root.FF(4),
                            SgfProperty.GameInfo.CP("Copyright")
                        )
                    )
                )

                assertEquals(expected, actual)
            },
            {
                val tree = SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Root.FF(4)),
                        SgfNode(SgfProperty.GameInfo.CP("Copyright"))
                    )
                )

                val actual = tree.addProperty(SgfProperty.GameInfo.PC("Place"))
                val expected = SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Root.FF(4)),
                        SgfNode(
                            SgfProperty.GameInfo.CP("Copyright"),
                            SgfProperty.GameInfo.PC("Place")
                        )
                    )
                )
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `move properties must not be mixed with setup properties`() {
        assertAll(
            {
                val tree = SgfGameTree(
                    nelOf(
                        SgfNode(
                            SgfProperty.Setup.AB(setOf(SgfPoint(1, 1)))
                        )
                    )
                )
                val actual = tree.addProperty(SgfProperty.Move.B(2, 2))
                val expected = SgfGameTree(
                    nelOf(
                        SgfNode(
                            SgfProperty.Setup.AB(setOf(SgfPoint(1, 1)))
                        ),
                        SgfNode(
                            SgfProperty.Move.B(2, 2)
                        )
                    )
                )

                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `B and W properties must not be mixed`() {
        assertAll(
            {
                val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Move.W(1, 1))))
                val actual = tree.addProperty(SgfProperty.Move.B(2, 2))
                val expected = SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Move.W(1, 1)),
                        SgfNode(SgfProperty.Move.B(2, 2))
                    )
                )

                assertEquals(expected, actual)
            },
            {
                val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(1, 1))))
                val actual = tree.addProperty(SgfProperty.Move.W(2, 2))
                val expected = SgfGameTree(
                    nelOf(
                        SgfNode(SgfProperty.Move.B(1, 1)),
                        SgfNode(SgfProperty.Move.W(2, 2))
                    )
                )

                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `it's bad style to add move properties to root node`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Root.FF(4))))
        val actual = tree.addProperty(SgfProperty.Move.MN(1))
        val expected = SgfGameTree(
            nelOf(
                SgfNode(SgfProperty.Root.FF(4)),
                SgfNode(SgfProperty.Move.MN(1))
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `setup properties must not be mixed with move properties`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(1, 1))))
        val actual = tree.addProperty(SgfProperty.Setup.AB(setOf(SgfPoint(2, 2))))
        val expected = SgfGameTree(
            nelOf(
                SgfNode(SgfProperty.Move.B(1, 1)),
                SgfNode(SgfProperty.Setup.AB(setOf(SgfPoint(2, 2))))
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `adding the same setup property will replace the old setup property`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1))))))
        val actual = tree.addProperty(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1), SgfPoint(2, 2))))
        val expected = SgfGameTree(
            nelOf(
                SgfNode(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1), SgfPoint(2, 2))))
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `UC must not be mixed with GB, GW, DM`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal)))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `DM must not be mixed with GB, GW, UC`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `GB must not be mixed with DM, GW, UC`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `GW must not be mixed with DM, GB, UC`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `BM must not be mixed with TE, DO, IT`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal)))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.DO)
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.DO))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.IT)
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.IT))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `TE must not be mixed with BM, DO, IT`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal)))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.DO)
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.DO))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.IT)
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.IT))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `DO must not be mixed with BM, TE, IT`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.DO))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.IT)
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.IT))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `IT must not be mixed with BM, TE, DO`() {
        val initial = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.IT))))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal)))))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.DO)
                val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.MoveAnnotation.DO))))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `Can have multiple private properties on a node`() {
        val actual = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.Root.FF(4)))))
            .addProperty(SgfProperty.Private("FOO", emptyList()))
            .addProperty(SgfProperty.Private("BAR", emptyList()))

        val expected = SgfGameTree(
            nelOf(
                SgfNode(
                    SgfProperty.Root.FF(4),
                    SgfProperty.Private("FOO", emptyList()),
                    SgfProperty.Private("BAR", emptyList())
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `Adding a private property will remove the old private property with the same identifier`() {
        val tree = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.Private("FOO", emptyList())))))
        val actual = tree.addProperty(SgfProperty.Private("FOO", listOf("Hello")))
        val expected = SgfGameTree(nelOf(SgfNode(setOf(SgfProperty.Private("FOO", listOf("Hello"))))))
        assertEquals(expected, actual)
    }
}
