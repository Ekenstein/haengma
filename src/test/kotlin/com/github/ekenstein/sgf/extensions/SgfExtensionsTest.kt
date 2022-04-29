package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.gameTree
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SgfExtensionsTest {
    @Test
    fun `adding a property of type root to the game tree when there is no root node will add a root node`() {
        assertAll(
            {
                val property = SgfProperty.Root.GM(GameType.Go)
                val tree = SgfGameTree.empty.addProperty(property)
                val expected = SgfGameTree(listOf(SgfNode(setOf(property))), emptyList())
                assertEquals(expected, tree)
            },
            {
                val tree = gameTree { node { property(SgfProperty.Move.B(1, 1)) } }
                val actual = tree
                    .addProperty(SgfProperty.Root.FF(4))
                    .addProperty(SgfProperty.Root.GM(GameType.Go))

                val expected = gameTree {
                    node {
                        property(SgfProperty.Root.FF(4))
                        property(SgfProperty.Root.GM(GameType.Go))
                    }

                    node {
                        property(SgfProperty.Move.B(1, 1))
                    }
                }

                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `adding a property of type root to the game tree will replace the existing property in the root node`() {
        val property = SgfProperty.Root.GM(GameType.Go)
        val tree = gameTree {
            node {
                property(SgfProperty.Root.GM(GameType.Chess))
                property(SgfProperty.Root.CA(Charsets.UTF_8))
            }
            node {
                property(SgfProperty.Move.B(Move.Stone(SgfPoint(1, 1))))
            }
        }

        val newTree = tree.addProperty(property)
        val expected = gameTree {
            node {
                property(property)
                property(SgfProperty.Root.CA(Charsets.UTF_8))
            }
            node {
                property(SgfProperty.Move.B(Move.Stone(SgfPoint(1, 1))))
            }
        }

        assertEquals(expected, newTree)
    }

    @Test
    fun `game info properties will be added to the node with game info properties or the root node`() {
        assertAll(
            {
                val tree = gameTree {
                    node {
                        property(SgfProperty.Root.FF(4))
                    }
                    node {
                        property(SgfProperty.GameInfo.PC("Test"))
                    }
                }

                val actual = tree.addProperty(SgfProperty.GameInfo.CP("Copyright"))
                val expected = gameTree {
                    node {
                        property(SgfProperty.Root.FF(4))
                    }
                    node {
                        property(SgfProperty.GameInfo.PC("Test"))
                        property(SgfProperty.GameInfo.CP("Copyright"))
                    }
                }

                assertEquals(expected, actual)
            },
            {
                val tree = gameTree {
                    node {
                        property(SgfProperty.Root.FF(4))
                    }
                }

                val actual = tree.addProperty(SgfProperty.GameInfo.CP("Copyright"))
                val expected = gameTree {
                    node {
                        property(SgfProperty.Root.FF(4))
                        property(SgfProperty.GameInfo.CP("Copyright"))
                    }
                }

                assertEquals(expected, actual)
            },
            {
                val tree = gameTree { }
                val actual = tree.addProperty(SgfProperty.GameInfo.CP("Copyright"))
                val expected = gameTree {
                    node {
                        property(SgfProperty.GameInfo.CP("Copyright"))
                    }
                }

                assertEquals(expected, actual)
            },
            {
                val tree = gameTree {
                    node { property(SgfProperty.Root.FF(4)) }
                    node { property(SgfProperty.GameInfo.CP("Copyright")) }
                }
                val actual = tree.addProperty(SgfProperty.GameInfo.PC("Place"))
                val expected = gameTree {
                    node { property(SgfProperty.Root.FF(4)) }
                    node {
                        property(SgfProperty.GameInfo.CP("Copyright"))
                        property(SgfProperty.GameInfo.PC("Place"))
                    }
                }
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `move properties must not be mixed with setup properties`() {
        assertAll(
            {
                val tree = gameTree { }
                val actual = tree.addProperty(SgfProperty.Move.B(1, 1))
                val expected = gameTree { node { property(SgfProperty.Move.B(1, 1)) } }
                assertEquals(expected, actual)
            },
            {
                val tree = gameTree { node { property(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1)))) } }
                val actual = tree.addProperty(SgfProperty.Move.B(2, 2))
                val expected = gameTree {
                    node { property(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1)))) }
                    node { property(SgfProperty.Move.B(2, 2)) }
                }
                assertEquals(expected, actual)
            },
            {
                val tree = gameTree { node { property(SgfProperty.Move.B(1, 1)) } }
                val actual = tree.addProperty(SgfProperty.Move.B(2, 2))
                val expected = gameTree {
                    node { property(SgfProperty.Move.B(1, 1)) }
                    node { property(SgfProperty.Move.B(2, 2)) }
                }

                assertEquals(expected, actual)
            },
            {
                val tree = gameTree { node { property(SgfProperty.Move.MN(1)) } }
                val actual = tree.addProperty(SgfProperty.Move.MN(2))
                val expected = gameTree { node { property(SgfProperty.Move.MN(2)) } }
                assertEquals(expected, actual)
            },
            {
                val tree = gameTree { node { property(SgfProperty.Move.KO) } }
                val actual = tree.addProperty(SgfProperty.Move.KO)
                val expected = gameTree { node { property(SgfProperty.Move.KO) } }
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `B and W properties must not be mixed`() {
        assertAll(
            {
                val tree = gameTree { node { property(SgfProperty.Move.W(1, 1)) } }
                val actual = tree.addProperty(SgfProperty.Move.B(2, 2))
                val expected = gameTree {
                    node { property(SgfProperty.Move.W(1, 1)) }
                    node { property(SgfProperty.Move.B(2, 2)) }
                }

                assertEquals(expected, actual)
            },
            {
                val tree = gameTree { node { property(SgfProperty.Move.B(1, 1)) } }
                val actual = tree.addProperty(SgfProperty.Move.W(2, 2))
                val expected = gameTree {
                    node { property(SgfProperty.Move.B(1, 1)) }
                    node { property(SgfProperty.Move.W(2, 2)) }
                }

                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `it's bad style to add move properties to root node`() {
        val tree = gameTree { node { property(SgfProperty.Root.FF(4)) } }
        val actual = tree.addProperty(SgfProperty.Move.MN(1))
        val expected = gameTree {
            node { property(SgfProperty.Root.FF(4)) }
            node { property(SgfProperty.Move.MN(1)) }
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `setup properties must not be mixed with move properties`() {
        val tree = gameTree { node { property(SgfProperty.Move.B(1, 1)) } }
        val actual = tree.addProperty(SgfProperty.Setup.AB(setOf(SgfPoint(2, 2))))
        val expected = gameTree {
            node { property(SgfProperty.Move.B(1, 1)) }
            node { property(SgfProperty.Setup.AB(setOf(SgfPoint(2, 2)))) }
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `adding the same setup property will replace the old setup property`() {
        val tree = gameTree { node { property(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1)))) } }
        val actual = tree.addProperty(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1), SgfPoint(2, 2))))
        val expected = gameTree {
            node { property(SgfProperty.Setup.AB(setOf(SgfPoint(1, 1), SgfPoint(2, 2)))) }
        }

        assertEquals(expected, actual)
    }

    @Test
    fun `UC must not be mixed with GB, GW, DM`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `DM must not be mixed with GB, GW, UC`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `GB must not be mixed with DM, GW, UC`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.GW(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `GW must not be mixed with DM, GB, UC`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.DM(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.GB(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.NodeAnnotation.UC(SgfDouble.Normal))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `BM must not be mixed with TE, DO, IT`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.DO)
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.DO)
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.IT)
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.IT)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `TE must not be mixed with BM, DO, IT`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.DO)
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.DO)
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.IT)
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.IT)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `DO must not be mixed with BM, TE, IT`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.DO)
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.IT)
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.IT)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `IT must not be mixed with BM, TE, DO`() {
        val initial = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.IT)
        assertAll(
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.BM(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.TE(SgfDouble.Normal))
                assertEquals(expected, actual)
            },
            {
                val actual = initial.addProperty(SgfProperty.MoveAnnotation.DO)
                val expected = SgfGameTree.empty.addProperty(SgfProperty.MoveAnnotation.DO)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `Can have multiple private properties on a node`() {
        val actual = SgfGameTree.empty
            .addProperty(SgfProperty.Private("FOO", emptyList()))
            .addProperty(SgfProperty.Private("BAR", emptyList()))

        val expected = SgfGameTree(
            listOf(
                SgfNode(
                    SgfProperty.Private("FOO", emptyList()),
                    SgfProperty.Private("BAR", emptyList())
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `Adding a private property will remove the old private property with the same identifier`() {
        val tree = SgfGameTree.empty.addProperty(SgfProperty.Private("FOO", emptyList()))
        val actual = tree.addProperty(SgfProperty.Private("FOO", listOf("Hello")))
        val expected = SgfGameTree.empty.addProperty(SgfProperty.Private("FOO", listOf("Hello")))
        assertEquals(expected, actual)
    }
}
