package com.github.ekenstein.sgf.extensions

import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.Move
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
}
