package com.github.ekenstein.sgf.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class EitherTest {
    @Test
    fun `match returns the result of either side`() {
        assertAll(
            {
                val either: Either<Int, Int> = Either.Left(1)
                val actual = either.match()
                val expected = 1
                assertEquals(expected, actual)
            },
            {
                val either: Either<Int, Int> = Either.Right(2)
                val actual = either.match()
                val expected = 2
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `mapLeft maps the left value if the either represents a left value`() {
        assertAll(
            {
                val either: Either<Int, String> = Either.Left(1)
                val actual = either.mapLeft { "Foo$it" }
                val expected: Either<String, String> = Either.Left("Foo1")
                assertEquals(expected, actual)
            },
            {
                val either: Either<Int, String> = Either.Right("Foo")
                val actual = either.mapLeft { "Foo$it" }
                val expected: Either<String, String> = Either.Right("Foo")
                assertEquals(expected, actual)
            }
        )
    }

    @Test
    fun `mapRight maps the right value if the either represents a right value`() {
        assertAll(
            {
                val either: Either<String, Int> = Either.Right(1)
                val actual = either.mapRight { "Foo$it" }
                val expected: Either<String, String> = Either.Right("Foo1")
                assertEquals(expected, actual)
            },
            {
                val either: Either<String, Int> = Either.Left("Foo")
                val actual = either.mapRight { "Foo$it" }
                val expected: Either<String, String> = Either.Left("Foo")
                assertEquals(expected, actual)
            }
        )
    }
}
