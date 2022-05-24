package com.github.ekenstein.sgf.utils

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import utils.nextItem
import utils.nextList
import utils.rng

class ZipperTest {
    @Test
    fun `toZipper takes a non-empty list and converts it to a zipper where the head of the list is the focus`() {
        val nel = nelOf(1, 2)
        val zipper = nel.toZipper()
        assertEquals(nel.head, zipper.focus)
        assertEquals(emptyLinkedList<Int>(), zipper.left)
        assertEquals(linkedListOf(2), zipper.right)
    }

    @Test
    fun `goRight moves the current focus to the left and the head of the right as focus`() {
        val zipper = nelOf(1, 2).toZipper()
        val result = zipper.goRight()
        assertEquals(zipper, result.origin)
        val next = result.get()
        assertEquals(linkedListOf(1), next.left)
        assertEquals(2, next.focus)
        assertEquals(emptyLinkedList<Int>(), next.right)
    }

    @Test
    fun `goRight returns a failed movement result if there's nothing to the right of the focus`() {
        val zipper = nelOf(1, 2).toZipper()
        val next = zipper.goRight().get()
        val result = next.goRight()
        val expected = MoveResult.Failure(next)
        assertEquals(expected, result)
    }

    @Test
    fun `goLeft returns a failed movement result if there is nothing to the left of the focus`() {
        val zipper = nelOf(1, 2).toZipper()
        val result = zipper.goLeft()
        val expected = MoveResult.Failure(zipper)
        assertEquals(expected, result)
    }

    @Test
    fun `goLeft returns a zipper where the focus is the head of the left and the current focus is the head of right`() {
        val zipper = nelOf(1, 2, 3).toZipper()
        val next = zipper.goRight().flatMap { it.goRight() }.get()
        val result = next.goLeft().get()
        val expected = Zipper(linkedListOf(1), 2, linkedListOf(3))
        assertEquals(expected, result)

        assertEquals(MoveResult.Success(zipper, result), result.goLeft())
    }

    @Test
    fun `goRightUnsafe throws IllegalStateException if there are no items to the right of the focus`() {
        val zipper = nelOf(1).toZipper()
        assertThrows<IllegalStateException> { zipper.goRightUnsafe() }
    }

    @Test
    fun `goRightUnsafe returns the zipper if the move result was successful`() {
        val zipper = nelOf(1, 2).toZipper()
        val next = zipper.goRightUnsafe()
        assertEquals(linkedListOf(1), next.left)
        assertEquals(2, next.focus)
        assertEquals(emptyLinkedList<Int>(), next.right)
    }

    @Test
    fun `goToLast goes to the right-most item in the zipper`() {
        assertAll(
            {
                val zipper = nelOf(1).toZipper()
                assertEquals(zipper, zipper.goToLast())
            },
            {
                val zipper = nelOf(1, 2, 3).toZipper().goToLast()
                val expected = Zipper(linkedListOf(2, 1), 3, emptyLinkedList())
                assertEquals(expected, zipper)
            }
        )
    }

    @Test
    fun `goToFirst goes to the left-most item in the zipper`() {
        assertAll(
            {
                val zipper = nelOf(1, 2, 3).toZipper()
                assertEquals(zipper, zipper.goToFirst())
            },
            {
                val zipper = nelOf(1, 2, 3).toZipper().insertLeft(0)
                val actual = zipper.goToFirst()
                val expected = Zipper(emptyLinkedList(), 0, linkedListOf(1, 2, 3))
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `insertLeft inserts the item directly to the left of the current focus`() {
        assertAll(
            {
                val zipper = nelOf(1).toZipper().insertLeft(0)
                val expected = Zipper(linkedListOf(0), 1, emptyLinkedList())
                assertEquals(expected, zipper)

                val committed = zipper.commit()
                assertEquals(nelOf(0, 1), committed)
            },
            {
                val zipper = nelOf(1).toZipper().insertLeft(0).insertLeft(-1)
                val expected = Zipper(linkedListOf(-1, 0), 1, emptyLinkedList())
                assertEquals(expected, zipper)
                val committed = zipper.commit()
                assertEquals(nelOf(0, -1, 1), committed)
            }
        )
    }

    @Test
    fun `insertRight inserts the item directly to the right of the current focus`() {
        assertAll(
            {
                val zipper = nelOf(1).toZipper().insertRight(2)
                val expected = Zipper(emptyLinkedList(), 1, linkedListOf(2))
                assertEquals(expected, zipper)

                val committed = zipper.commit()
                assertEquals(nelOf(1, 2), committed)
            },
            {
                val zipper = nelOf(1, 2).toZipper().insertRight(3)
                val expected = Zipper(emptyLinkedList(), 1, linkedListOf(3, 2))
                assertEquals(expected, zipper)
                val committed = zipper.commit()
                assertEquals(nelOf(1, 3, 2), committed)
            }
        )
    }

    @Test
    fun `navigation does not change the result of the commit`() {
        val operations: List<(Zipper<Int>) -> Zipper<Int>> = listOf(
            { it.goRight().orStay() },
            { it.goLeft().orStay() },
            { it.goToLast() },
            { it.goToFirst() }
        )

        repeat(20) {
            val perform = rng.nextList {
                it.nextItem(operations)
            }

            val expected = rng.nextList { it.nextInt() }.toNelUnsafe()
            val on = expected.toZipper()
            val next = perform.fold(on) { z, p -> p(z) }
            val actual = next.commit()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `commitAtCurrentPosition returns a nel that only includes the left items and the focus`() {
        assertAll(
            {
                val zipper = nelOf(1, 2, 3, 4).toZipper()
                val actual = zipper.commitAtCurrentPosition()
                val expected = nelOf(1)
                assertEquals(expected, actual)
            },
            {
                val zipper = nelOf(1, 2, 3, 4).toZipper().goRightUnsafe().goRightUnsafe()
                val actual = zipper.commitAtCurrentPosition()
                val expected = nelOf(1, 2, 3)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `update updates the current focus`() {
        val zipper = nelOf(1, 2, 3).toZipper()
        val next = zipper.goRightUnsafe()
        val updated = next.update { it + 2 }
        val expected = Zipper(linkedListOf(1), 4, linkedListOf(3))
        assertEquals(expected, updated)
    }
}
