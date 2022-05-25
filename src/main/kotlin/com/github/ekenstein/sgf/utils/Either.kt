package com.github.ekenstein.sgf.utils

/**
 * Represents either a left value or a right value, not both.
 */
sealed class Either<out TLeft, out TRight> {
    /**
     * Represents a left [value]
     */
    data class Left<T>(val value: T) : Either<T, Nothing>()

    /**
     * Represents a right [value]
     */
    data class Right<T>(val value: T) : Either<Nothing, T>()
}

/**
 * Maps the left value, if the given either represents a right value, this will be returned,
 * otherwise an either representing a left value containing the mapped value.
 */
fun <L, R, U> Either<L, R>.mapLeft(block: (L) -> U): Either<U, R> = when (this) {
    is Either.Left -> Either.Left(block(value))
    is Either.Right -> this
}

/**
 * Maps the right value, if the given either represents a left value, this will be returned,
 * otherwise an either representing a right value containing the mapped value.
 */
fun <L, R, U> Either<L, R>.mapRight(block: (R) -> U): Either<L, U> = when (this) {
    is Either.Left -> this
    is Either.Right -> Either.Right(block(value))
}

/**
 * Matches the values on either sides of the given [Either] and returns the value regardless
 * of which side the value was on.
 */
fun <T> Either<T, T>.match() = when (this) {
    is Either.Left -> value
    is Either.Right -> value
}
