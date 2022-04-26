package com.github.ekenstein.sgf.serialization

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Locale
import kotlin.test.assertEquals

class SgfSerializationTest {
    @ParameterizedTest
    @MethodSource("locales")
    fun `regardless of locale, real values will always have a dot as decimal separator`(locale: Locale) {
        val tree = SgfGameTree.empty.addProperty(SgfProperty.GameInfo.KM(6.5))
        val actual = withLocale(locale) {
            tree.encodeToString()
        }

        val expected = "(;KM[6.5])"
        assertEquals(expected, actual)
    }

    @Test
    fun `serializing game dates will add partial dates whenever possible`() {
        assertAll(
            {
                val tree = SgfGameTree.empty.addProperty(
                    SgfProperty.GameInfo.DT(
                        GameDate.of(2022, 4, 6), // yyyy-MM-dd
                        GameDate.of(2022, 4, 7), // dd
                        GameDate.of(2022, 6, 1), // MM-dd
                        GameDate.of(2022, 6, 6) // dd
                    )
                )

                val expected = "(;DT[2022-04-06,07,06-01,06])"
                val actual = tree.encodeToString()
                assertEquals(expected, actual)
            }
        )
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
