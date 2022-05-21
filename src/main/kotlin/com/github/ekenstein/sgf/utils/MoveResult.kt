package com.github.ekenstein.sgf.utils

sealed class MoveResult<out T> {
    data class Failure<T>(val origin: T) : MoveResult<T>()
    data class Success<T>(val value: T, val origin: T) : MoveResult<T>()
}

fun <T> MoveResult<T>.flatMap(block: (T) -> MoveResult<T>): MoveResult<T> = when (this) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> block(value)
}

fun <T, U> MoveResult<T>.flatMap(origin: U, block: (T) -> MoveResult<U>): MoveResult<U> = when (this) {
    is MoveResult.Failure -> MoveResult.Failure(origin)
    is MoveResult.Success -> block(value)
}

fun <T, U> MoveResult<T>.map(origin: U, block: (T) -> U): MoveResult<U> = flatMap(origin) {
    MoveResult.Success(block(it), origin)
}

fun <T> MoveResult<T>.orStay() = when (this) {
    is MoveResult.Failure -> origin
    is MoveResult.Success -> value
}

fun <T> MoveResult<T>.orNull() = when (this) {
    is MoveResult.Failure -> null
    is MoveResult.Success -> value
}

fun <T> MoveResult<T>.orElse(block: (T) -> MoveResult<T>) = when (this) {
    is MoveResult.Failure -> block(origin)
    is MoveResult.Success -> this
}

fun <T> MoveResult<T>.get(): T = orNull() ?: error("The move result was a failure")

fun <T> MoveResult<T>.withOrigin(origin: T): MoveResult<T> = when (this) {
    is MoveResult.Failure -> copy(origin = origin)
    is MoveResult.Success -> copy(origin = origin)
}
