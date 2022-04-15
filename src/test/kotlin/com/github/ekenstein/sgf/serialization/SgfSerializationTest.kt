package com.github.ekenstein.sgf.serialization

import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty
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
