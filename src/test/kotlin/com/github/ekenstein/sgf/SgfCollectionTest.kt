package com.github.ekenstein.sgf

import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.commit
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SgfCollectionTest {
    @Test
    fun `can filter games by its game information`() {
        val game1 = SgfEditor {
            gameName = "Foo"
        }.commit()

        val game2 = SgfEditor {
            gameName = "Bar"
        }.commit()

        val collection = SgfCollection(game1, game2)
        val tree = collection.filter {
            it.gameName == "Foo"
        }

        assertEquals(listOf(game1), tree)
    }
}
