package com.github.ekenstein.sgf.utils

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNull

class LinkedListTest {
    @Test
    fun `empty linked list is nil`() {
        assertEquals(LinkedList.Nil, emptyLinkedList<Int>())
    }

    @Test
    fun `empty linked list has size 0`() {
        assertEquals(0, emptyLinkedList<Int>().size)
    }

    @Test
    fun `empty linked list is empty`() {
        assertTrue(emptyLinkedList<Int>().isEmpty())
    }

    @Test
    fun `IndexOutOfBoundsException when accessing an index`() {
        assertThrows<IndexOutOfBoundsException> { emptyLinkedList<Int>()[0] }
    }

    @Test
    fun `a non-empty linked list is not empty`() {
        assertFalse(linkedListOf(1).isEmpty())
    }

    @Test
    fun `can retrieve the last item of a non-empty linked list`() {
        val list = linkedListOf(1, 2, 3)
        val item = list[list.size - 1]
        assertEquals(3, item)
    }

    @Test
    fun `can retrieve the first item of a non-empty linked list`() {
        val list = linkedListOf(1, 2, 3)
        val item = list[0]
        assertEquals(1, item)
    }

    @Test
    fun `non-empty linked list is cons`() {
        val list = linkedListOf(1)
        val expected = LinkedList.Cons(1, LinkedList.Nil)
        assertEquals(expected, list)
    }

    @Test
    fun `can iterate a non-empty linked list`() {
        val list = linkedListOf(1)
        val actual = mutableListOf<Int>()
        list.forEach { actual.add(it) }

        assertEquals(listOf(1), actual)
    }

    @Test
    fun `can iterate an empty linked list`() {
        val list = emptyLinkedList<Int>()
        val actual = mutableListOf<Int>()
        list.forEach { actual.add(it) }
        assertEquals(emptyList<Int>(), actual)
    }

    @Test
    fun `can reverse a linked list`() {
        val list = linkedListOf(1, 2, 3)
        val reversed = list.reverse()
        val expected = linkedListOf(3, 2, 1)
        assertEquals(expected, reversed)
    }

    @Test
    fun `can reverse a linked list with a tail`() {
        val tail = linkedListOf(4, 5)
        val list = linkedListOf(1, 2, 3)
        val reversed = list.reverse(tail)
        val expected = linkedListOf(3, 2, 1, 4, 5)
        assertEquals(expected, reversed)
    }

    @Test
    fun `adding an item to a linked list returns a linked list where the added item is at the end`() {
        val list = linkedListOf(1, 2, 3)
        val actual = list + 4
        val expected = linkedListOf(1, 2, 3, 4)
        assertEquals(expected, actual)
    }

    @Test
    fun `adding a list to a linked list returns a linked list where the added items are at the end`() {
        val list = linkedListOf(1, 2, 3)
        val actual = list + linkedListOf(4, 5, 6)
        val expected = linkedListOf(1, 2, 3, 4, 5, 6)
        assertEquals(expected, actual)
    }

    @Test
    fun `removing the first item will return a pair of the head of the list and the tail`() {
        val list = linkedListOf(1, 2, 3)
        val (head, tail) = list.removeFirst()
        assertEquals(1, head)
        assertEquals(linkedListOf(2, 3), tail)
    }

    @Test
    fun `removing the first item of an empty linked list returns a pair of null to nil`() {
        val list = emptyLinkedList<Int>()
        val (head, tail) = list.removeFirst()
        assertNull(head)
        assertEquals(emptyLinkedList<Int>(), tail)
    }

    @Test
    fun `linkedListOfNotNull filters out all null values and returns a linked list`() {
        assertAll(
            {
                val list = linkedListOfNotNull(1, null, 3)
                val expectedList = linkedListOf(1, 3)
                assertEquals(expectedList, list)
            },
            {
                val list = linkedListOfNotNull<Int>()
                val expectedList = emptyLinkedList<Int>()
                assertEquals(expectedList, list)
            },
            {
                val list = linkedListOfNotNull<Int>(null, null)
                val expectedList = emptyLinkedList<Int>()
                assertEquals(expectedList, list)
            }
        )
    }

    @Test
    fun `linked list can be compared with an ordinary array list`() {
        val linkedList = linkedListOf(1, 2)
        val arrayList = listOf(1, 2)
        assertIterableEquals(linkedList, arrayList)
    }
}
