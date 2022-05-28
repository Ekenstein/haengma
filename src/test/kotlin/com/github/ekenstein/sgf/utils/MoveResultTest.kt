package com.github.ekenstein.sgf.utils

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MoveResultTest {
    @Test
    fun `orError throws IllegalStateException containing the given message if the movement failed`() {
        val result = MoveResult.Failure(1)
        val message = "Foo"
        try {
            result.orError { message }
            assertFalse(true, "Expected an exception to be thrown")
        } catch (ex: Exception) {
            assertTrue(ex is IllegalStateException, "The $ex isn't an IllegalStateException")
            assertEquals(message, ex.message)
        }
    }

    @Test
    fun `onError returns the new position if the result represents a successful movement`() {
        val result = MoveResult.Success(1, 2)
        val actual = result.orError { "Foo" }
        val expected = 1
        assertEquals(expected, actual)
    }

    @Test
    fun `orNull returns null if the result represents a failure`() {
        val result = MoveResult.Failure(1)
        val actual = result.orNull()
        assertNull(actual)
    }

    @Test
    fun `orNull returns the new position if the result represents a successful movement`() {
        val result = MoveResult.Success(1, 2)
        val actual = result.orNull()
        val expected = 1

        assertEquals(expected, actual)
    }

    @Test
    fun `withOrigin updates the origin of a failed movement`() {
        val result = MoveResult.Failure(1)
        val actual = result.withOrigin(2)
        val expected = MoveResult.Failure(2)
        assertEquals(expected, actual)
    }

    @Test
    fun `withOrigin updates the origin of a successful movement`() {
        val result = MoveResult.Success(1, 2)
        val actual = result.withOrigin(3)
        val expected = MoveResult.Success(1, 3)
        assertEquals(expected, actual)
    }

    @Test
    fun `get returns the new position on a successful movement`() {
        val result = MoveResult.Success(1, 2)
        val actual = result.get()
        val expected = 1
        assertEquals(expected, actual)
    }

    @Test
    fun `get throws IllegalStateException on a failed movement`() {
        val result = MoveResult.Failure(1)
        assertThrows<IllegalStateException> { result.get() }
    }

    @Test
    fun `orElse executes the move if the movement result was a failure`() {
        assertAll(
            {
                val result = MoveResult.Failure(1)
                val actual = result.orElse { origin ->
                    MoveResult.Success(1 + origin, origin)
                }
                val expected = MoveResult.Success(2, result.origin)
                assertEquals(expected, actual)
            },
            {
                val result = MoveResult.Failure(1)
                val actual = result.orElse { origin ->
                    MoveResult.Failure(origin + 1)
                }
                val expected = MoveResult.Failure(2)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `orElse does not execute the move if movement result was a success`() {
        val result = MoveResult.Success(1, 2)
        val actual = result.orElse { origin ->
            MoveResult.Failure(origin)
        }

        val expected = MoveResult.Success(1, 2)
        assertEquals(expected, actual)
    }

    @Test
    fun `orStay returns new position if the movement result was a success`() {
        val result = MoveResult.Success(1, 2)
        val actual = result.orStay()
        assertEquals(result.position, actual)
    }

    @Test
    fun `orStay returns the origin if the movement result was a failure`() {
        val result = MoveResult.Failure(2)
        val actual = result.orStay()
        assertEquals(result.origin, actual)
    }

    @Test
    fun `map returns a successful result if the movement was a success`() {
        val result = MoveResult.Success(1, 2)
        val actual = result.map(3.0) { position -> position + 4.5 }
        val expected = MoveResult.Success(5.5, 3.0)
        assertEquals(expected, actual)
    }

    @Test
    fun `map returns a failed result if the movement was a failure`() {
        val result = MoveResult.Failure(2)
        val actual = result.map(5.0) { origin -> origin + 2.5 }
        val expected = MoveResult.Failure(5.0)
        assertEquals(expected, actual)
    }

    @Test
    fun `flatMap executes and returns the result of the move iff the movement result was a success`() {
        val result = MoveResult.Success(1, 2)

        assertAll(
            {
                val actual = result.flatMap { position ->
                    MoveResult.Success(position + 1, position + 2)
                }
                val expected = MoveResult.Success(2, 3)
                assertEquals(expected, actual)
            },
            {
                val actual = result.flatMap { position ->
                    MoveResult.Failure(position + 1)
                }
                val expected = MoveResult.Failure(2)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `flatMap returns the given result if the movement was a failure`() {
        val result = MoveResult.Failure(2)
        val actual = result.flatMap { position -> MoveResult.Failure(position + 1) }
        assertEquals(result, actual)
    }

    @Test
    fun `flatMap executes the given move if movement was successful, otherwise returns a failed result with origin`() {
        assertAll(
            {
                val result = MoveResult.Success(1, 2)
                val actual = result.flatMap(5.0) { position ->
                    MoveResult.Success(position + 1.5, 3.0)
                }
                val expected = MoveResult.Success(2.5, 3.0)
                assertEquals(expected, actual)
            },
            {
                val result = MoveResult.Failure(2)
                val actual = result.flatMap(5.0) { position ->
                    MoveResult.Success(position + 1.5, 3.0)
                }
                val expected = MoveResult.Failure(5.0)
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `can retrieve the origin regardless of the result`() {
        assertAll(
            {
                val result: MoveResult<Int> = MoveResult.Success(1, 2)
                assertEquals(2, result.origin)
            },
            {
                val result: MoveResult<Int> = MoveResult.Failure(1)
                assertEquals(1, result.origin)
            }
        )
    }

    @Test
    fun `onSuccess will run the block iff the result was successful`() {
        val result = MoveResult.Success(1, 2)
        var wasExecuted = false
        val actual = result.onSuccess {
            wasExecuted = true
            assertEquals(1, it)
        }

        assertEquals(result, actual)
        assertTrue(wasExecuted)
    }

    @Test
    fun `onSuccess will not run the block if the result was a failure`() {
        val result = MoveResult.Failure(2)
        val actual = result.onSuccess {
            assertTrue(false, "Shouldn't run the block if the result was a failure")
        }

        assertEquals(result, actual)
    }
}
