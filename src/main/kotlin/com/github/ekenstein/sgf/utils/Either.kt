package com.github.ekenstein.sgf.utils

sealed class Either<out TLeft, out TRight> {
    data class Left<T>(val value: T) : Either<T, Nothing>()
    data class Right<T>(val value: T) : Either<Nothing, T>()
}

fun <L, R, U> Either<L, R>.mapLeft(block: (L) -> U): Either<U, R> = when (this) {
    is Either.Left -> Either.Left(block(value))
    is Either.Right -> this
}

fun <L, R, U> Either<L, R>.mapRight(block: (R) -> U): Either<L, U> = when (this) {
    is Either.Left -> this
    is Either.Right -> Either.Right(block(value))
}

fun <T> Either<T, T>.match() = when (this) {
    is Either.Left -> value
    is Either.Right -> value
}
