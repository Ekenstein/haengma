package com.github.ekenstein.sgf.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class NonEmptySetTest {
    @Test
    fun `non empty set is never empty`() {
        val set = nonEmptySetOf(1)
        assertFalse(set.isEmpty())
    }

    @Test
    fun `non empty set is not an ordered set`() {
        val set1 = nonEmptySetOf(1, 2)
        val set2 = nonEmptySetOf(2, 1)
        assertEquals(set1, set2)
    }

    @Test
    fun `non empty set can be compared to an ordinary set`() {
        val set1 = nonEmptySetOf(1, 2, 3)
        val set2 = setOf(3, 1, 2)
        assertEquals(set1, set2)
    }

    @Test
    fun `non empty set contains only unique items`() {
        val set = nonEmptySetOf(1, 1, 2)
        val expected = setOf(1, 2)
        assertEquals(expected, set)
    }

    @Test
    fun `can concatenate a non empty set with an item of the same type`() {
        val set = nonEmptySetOf(1)
        val actual = set + 2
        assertEquals(setOf(1, 2), actual)
    }

    @Test
    fun `can concatenate a non empty set with another set`() {
        val set = nonEmptySetOf(1, 2, 3)
        val anotherSet = setOf(3, 4, 5)
        val actual = set + anotherSet
        val expected = setOf(1, 2, 3, 4, 5)
        assertEquals(expected, actual)
    }

    @Test
    fun `the size of the non empty set is the count of all unique items`() {
        val set = nonEmptySetOf(1, 1, 1)
        assertEquals(1, set.size)
    }

    @Test
    fun `toNonEmptySet returns null if the collection is empty`() {
        val actual = emptyList<Int>().toNonEmptySet()
        assertNull(actual)
    }

    @Test
    fun `toNonEmptySet returns a non empty set if the collection is not empty`() {
        val actual = listOf(1).toNonEmptySet()
        assertEquals(nonEmptySetOf(1), actual)
    }

    @Test
    fun `toNonEmptySetUnsafe throws IllegalArgumentException if the collection is empty`() {
        assertThrows<IllegalArgumentException> { emptyList<Int>().toNonEmptySetUnsafe() }
    }

    @Test
    fun `toNonEmptySetUnsafe returns a non empty set if the collection is not empty`() {
        val actual = assertDoesNotThrow { listOf(1).toNonEmptySetUnsafe() }
        assertEquals(nonEmptySetOf(1), actual)
    }
}
