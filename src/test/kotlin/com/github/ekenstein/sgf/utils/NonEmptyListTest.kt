package com.github.ekenstein.sgf.utils

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NonEmptyListTest {
    @Test
    fun `nelOf returns a non-empty list`() {
        assertAll(
            {
                val list = nelOf(1, 2, 3)
                assertEquals(1, list.head)
                assertEquals(listOf(2, 3), list.tail)
                assertFalse(list.isEmpty())
            },
            {
                val tail = listOf(2, 3)
                val list = nelOf(1, tail)
                assertEquals(1, list.head)
                assertEquals(tail, list.tail)
                assertFalse(list.isEmpty())
            }
        )
    }

    @Test
    fun `toNel converts the list to a non-empty list`() {
        val list = listOf(1, 2, 3)
        val nel = list.toNel()
        assertNotNull(nel)
        assertEquals(1, nel?.head)
        assertEquals(listOf(2, 3), nel?.tail)
        assertEquals(list, nel)
    }

    @Test
    fun `toNel returns null if the list is empty`() {
        val list = emptyList<Int>()
        val nel = list.toNel()
        assertNull(nel)
    }

    @Test
    fun `toNel returns null if the linked list is empty`() {
        val list = emptyLinkedList<Int>()
        val nel = list.toNel()
        assertNull(nel)
    }

    @Test
    fun `toNel returns non-empty list if the linked list is not empty`() {
        val list = linkedListOf(1, 2, 3)
        val nel = list.toNel()
        assertIterableEquals(list, nel)
    }

    @Test
    fun `toNelUnsafe returns a non-empty list if the list is not empty`() {
        val list = listOf(1)
        val nel = list.toNelUnsafe()
        assertEquals(1, nel.head)
        assertEquals(emptyList<Int>(), nel.tail)
    }

    @Test
    fun `toNelUnsafe throws IllegalStateException if the list is empty`() {
        val list = emptyList<Int>()
        assertThrows<IllegalStateException> { list.toNelUnsafe() }
    }

    @Test
    fun `toNelUnsafe returns the non-empty list`() {
        val nel = nelOf(1, 2, 3)
        val actual = nel.toNelUnsafe()
        assertEquals(nel, actual)
    }
}
