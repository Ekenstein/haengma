package com.github.ekenstein.sgf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PropertySetTest {
    @Test
    fun `the last property of duplicate properties will be added to the property set`() {
        val actual = propertySetOf(
            SgfProperty.Move.B(3, 3),
            SgfProperty.Move.B(4, 4),
            SgfProperty.Move.B(5, 5),
            SgfProperty.Move.W(6, 6)
        )

        val expected = propertySetOf(SgfProperty.Move.B(5, 5), SgfProperty.Move.W(6, 6))
        assertEquals(expected, actual)
    }

    @Test
    fun `empty property set has size 0`() {
        val set = emptyPropertySet()
        assertEquals(0, set.size)
    }

    @Test
    fun `contains returns true if the type of the given property exists in the set`() {
        val set = propertySetOf(SgfProperty.Setup.PL(SgfColor.White))
        assertTrue(set.contains(SgfProperty.Setup.PL(SgfColor.Black)))
    }

    @Test
    fun `the last non null property of duplicate properties will be added to the property set`() {
        val actual = propertySetOfNotNull(
            null,
            SgfProperty.Move.B(3, 3),
            null,
            SgfProperty.Move.B(4, 4)
        )

        val expected = propertySetOf(SgfProperty.Move.B(4, 4))
        assertEquals(expected, actual)
    }

    @Test
    fun `adding a property to a set of properties will replace the current property in the set`() {
        val actual = propertySetOf(SgfProperty.Move.B.pass()) + SgfProperty.Move.B(3, 3)
        val expected = propertySetOf(SgfProperty.Move.B(3, 3))
        assertEquals(expected, actual)
    }

    @Test
    fun `adding a property to a set of properties will add the property to the current set`() {
        val actual = propertySetOf(SgfProperty.Move.W(3, 3)) + SgfProperty.Move.MN(3)
        val expected = propertySetOf(SgfProperty.Move.W(3, 3), SgfProperty.Move.MN(3))
        assertEquals(expected, actual)
    }

    @Test
    fun `removing a property from the set will return a new set where the property of the same type is removed`() {
        val set = propertySetOf(SgfProperty.Move.MN(1), SgfProperty.Move.B(3, 3))
        val actual = set - SgfProperty.Move.MN(2)
        val expected = propertySetOf(SgfProperty.Move.B(3, 3))
        assertEquals(expected, actual)
    }

    @Test
    fun `adding a set of properties to a set of properties will replace the current properties`() {
        val set = propertySetOf(SgfProperty.Move.MN(1), SgfProperty.Move.B.pass())
        val set2 = propertySetOf(SgfProperty.Move.MN(2), SgfProperty.Move.B(3, 3))
        val actual = set + set2
        assertEquals(set2, actual)
    }

    @Test
    fun `adding a set of properties to a set of properties will add the given properties to the current properties`() {
        val set = propertySetOf(SgfProperty.Move.MN(1), SgfProperty.Move.B(3, 3))
        val set2 = propertySetOf(SgfProperty.Move.KO, SgfProperty.Move.B(4, 4))
        val actual = set + set2
        val expected = propertySetOf(SgfProperty.Move.MN(1), SgfProperty.Move.KO, SgfProperty.Move.B(4, 4))
        assertEquals(expected, actual)
    }
}
