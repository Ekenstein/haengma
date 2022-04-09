package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.Sgf
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.encodeToString
import com.github.ekenstein.sgf.sgf
import kotlin.test.Test
import kotlin.test.assertEquals

class SgfParserTest {
    @Test
    fun `can parse number values`() {
        val sgf = "(;HA[9])"
        val collection = Sgf().decode(sgf)
        val expected = sgf {
            tree {
                node {
                    property(SgfProperty.GameInfo.HA(9))
                }
            }
        }

        assertEquals(expected, collection)
    }

    @Test
    fun `can parse real values`() {
        val sgf = "(;KM[6.5])"
        val collection = Sgf().decode(sgf)
        val expected = sgf {
            tree {
                node {
                    property(SgfProperty.GameInfo.KM(6.5))
                }
            }
        }

        assertEquals(expected, collection)
    }

    @Test
    fun `can parse black move`() {
        val sgf = "(;B[aa])"
        val collection = Sgf().decode(sgf)
        val expected = sgf {
            tree {
                node {
                    property(SgfProperty.Move.B(Move.Stone(1, 1)))
                }
            }
        }

        assertEquals(expected, collection)
    }

    @Test
    fun `can parse white move`() {
        val sgf = "(;W[ab])"
        val collection = Sgf().decode(sgf)
        val expected = sgf {
            tree {
                node {
                    property(SgfProperty.Move.W(Move.Stone(1, 2)))
                }
            }
        }

        assertEquals(expected, collection)
    }

    @Test
    fun `can parse composed`() {
        val sgf = "(;AP[johnny\\: Hello!:0.1.0])"
        val collection = Sgf().decode(sgf)
        val expected = sgf {
            tree {
                node {
                    property(SgfProperty.Root.AP("johnny: Hello!", "0.1.0"))
                }
            }
        }

        assertEquals(expected, collection)
    }

    @Test
    fun `real game`() {
        val sgf = Sgf()
        val (raw, collection) = withResource("3bn6-gokifu-20220324-Byun_Sangil-Gu_Zihao.sgf") {
            it to sgf.decode(it)
        }

        val serialized = sgf.encodeToString(collection)
        assertEquals(raw, serialized)
    }

    private fun <T> withResource(resourceName: String, block: (String) -> T): T {
        val resource = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)) {
            "Couldn't find the resource $resourceName"
        }

        return resource.use { block(String(it.readAllBytes())) }
    }
}
