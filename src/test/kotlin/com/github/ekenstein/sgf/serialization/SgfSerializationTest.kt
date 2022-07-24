package com.github.ekenstein.sgf.serialization

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.parser.from
import com.github.ekenstein.sgf.utils.nelOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.util.Locale

class SgfSerializationTest {
    @ParameterizedTest
    @MethodSource("locales")
    fun `regardless of locale, real values will always have a dot as decimal separator`(locale: Locale) {
        val tree = SgfGameTree(
            nelOf(
                SgfNode(
                    SgfProperty.GameInfo.KM(6.5)
                )
            )
        )
        val actual = withLocale(locale) {
            tree.encodeToString()
        }

        val expected = "(;KM[6.5])"
        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @MethodSource("locales")
    fun `regardless of locale, larger numbers will never have a group separator`(locale: Locale) {
        val overNineThousand = 5000000
        val sOverNineThousand = "5000000"
        val tree = SgfGameTree(
            SgfNode(SgfProperty.Move.MN(overNineThousand)),
            SgfNode(SgfProperty.GameInfo.KM(overNineThousand.toDouble()))
        )

        val actual = withLocale(locale) {
            tree.encodeToString()
        }

        val expected = "(;MN[$sOverNineThousand];KM[$sOverNineThousand])"
        assertEquals(expected, actual)
    }

    @Test
    fun `serializing game dates will add partial dates whenever possible`() {
        assertAll(
            {
                val tree = SgfGameTree(
                    nelOf(
                        SgfNode(
                            SgfProperty.GameInfo.DT(
                                GameDate.of(2022, 4, 6), // yyyy-MM-dd
                                GameDate.of(2022, 4, 7), // dd
                                GameDate.of(2022, 6, 1), // MM-dd
                                GameDate.of(2022, 6, 6) // dd
                            )
                        )
                    )
                )

                val expected = "(;DT[2022-04-06,07,06-01,06])"
                val actual = tree.encodeToString()
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `can encode to output stream`() {
        val outputStream = ByteArrayOutputStream()
        SgfGameTree(nelOf(SgfNode(SgfProperty.GameInfo.KM(6.5)))).encode(outputStream)
        val expected = "(;KM[6.5])"
        val actual = String(outputStream.toByteArray())
        assertEquals(expected, actual)
    }

    @Test
    fun `private properties with a list value should be serialized to sgf list values`() {
        val tree = SgfGameTree(
            nelOf(
                SgfNode(SgfProperty.Private("TW", listOf("aa", "bb")))
            )
        )

        val sgf = tree.encodeToString()
        val expected = "(;TW[aa][bb])"
        assertEquals(expected, sgf)
    }

    @Test
    fun `serializing a non-composed text will escape necessary chars`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.NodeAnnotation.C("gibodibo [2d]: hi \\o/"))))
        val expected = "(;C[gibodibo [2d\\]: hi \\\\o/])"
        val actual = tree.encodeToString()
        assertEquals(expected, actual)
    }

    @Test
    fun `serializing a non-composed simple text will escape necessary chars`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.NodeAnnotation.N("gibodibo [2d]: hi \\o/"))))
        val expected = "(;N[gibodibo [2d\\]: hi \\\\o/])"
        val actual = tree.encodeToString()
        assertEquals(expected, actual)
    }

    @Test
    fun `serializing a number will keep all the digits regardless of length`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Timing.WL(0.7003728838801924))))
        val expected = "(;WL[0.7003728838801924])"
        val actual = tree.encodeToString()
        assertEquals(expected, actual)
    }

    @Test
    fun `serializing a composed value with backslashes`() {
        val tree = SgfGameTree(nelOf(SgfNode(SgfProperty.Root.AP("apa\\", "bepa"))))
        val expected = "(;AP[apa\\\\:bepa])"
        val actual = tree.encodeToString()
        assertEquals(expected, actual)

        val t = SgfCollection.from(actual).trees.head
        assertEquals(tree, t)
    }

    private fun <T> withLocale(locale: Locale, block: () -> T): T {
        val default = Locale.getDefault()
        try {
            Locale.setDefault(locale)
            return block()
        } finally {
            Locale.setDefault(default)
        }
    }

    companion object {
        @JvmStatic
        fun locales() = Locale.getAvailableLocales().filterNotNull()
    }
}
