package com.github.ekenstein.sgf.utils

/**
 * Represents either a successful movement or a failed movement.
 */
sealed class MoveResult<out T> {
    /**
     * The origin of the movement regardless of the result.
     */
    abstract val origin: T

    /**
     * Represents a failed movement. Contains the [origin] prior to the movement.
     */
    data class Failure<T>(override val origin: T) : MoveResult<T>()

    /**
     * Represents a successful movement. Contains the new [position] and the [origin] prior to the movement.
     */
    data class Success<T>(val position: T, override val origin: T) : MoveResult<T>()
}

/**
 * Executes and returns the result of the given [move] iff the result was [MoveResult.Success], otherwise
 * this failed result will be returned.
 */
fun <T> MoveResult<T>.flatMap(move: (T) -> MoveResult<T>): MoveResult<T> = when (this) {
    is MoveResult.Failure -> this
    is MoveResult.Success -> move(position)
}

/**
 * Executes and returns the result of the given [move] iff the result was [MoveResult.Success], otherwise
 * a failed result will be returned containing the given [origin].
 */
fun <T, U> MoveResult<T>.flatMap(origin: U, move: (T) -> MoveResult<U>): MoveResult<U> = when (this) {
    is MoveResult.Failure -> MoveResult.Failure(origin)
    is MoveResult.Success -> move(position)
}

/**
 * Executes the [move] and returns a [MoveResult.Success] with the given [origin] iff the given result was
 * [MoveResult.Success], otherwise a [MoveResult.Failure] with the given [origin] will be returned.
 */
fun <T, U> MoveResult<T>.map(origin: U, move: (T) -> U): MoveResult<U> = flatMap(origin) {
    MoveResult.Success(move(it), origin)
}

/**
 * Returns the origin of the movement iff the result was [MoveResult.Failure], otherwise the new position.
 */
fun <T> MoveResult<T>.orStay() = when (this) {
    is MoveResult.Failure -> origin
    is MoveResult.Success -> position
}

/**
 * Returns the position iff the result was [MoveResult.Success], otherwise null if the result was [MoveResult.Failure]
 */
fun <T> MoveResult<T>.orNull() = when (this) {
    is MoveResult.Failure -> null
    is MoveResult.Success -> position
}

/**
 * Executes and returns the result of the given [move] on the origin iff the result was [MoveResult.Failure],
 * otherwise the given successful result will be returned.
 */
fun <T> MoveResult<T>.orElse(move: (T) -> MoveResult<T>) = when (this) {
    is MoveResult.Failure -> move(origin)
    is MoveResult.Success -> this
}

/**
 * Returns the new position of the result iff the result was [MoveResult.Success], otherwise an
 * [IllegalStateException] will be thrown.
 */
fun <T> MoveResult<T>.get(): T = orError { "The movement was unsuccessful" }

/**
 * Updates the origin of the movement result regardless of what the result was.
 */
fun <T> MoveResult<T>.withOrigin(origin: T): MoveResult<T> = when (this) {
    is MoveResult.Failure -> copy(origin = origin)
    is MoveResult.Success -> copy(origin = origin)
}

/**
 * Returns the new position of the result iff the result was [MoveResult.Success], otherwise
 * an [IllegalStateException] will be thrown containing the given [message].
 */
fun <T> MoveResult<T>.orError(message: () -> String) = orNull() ?: error(message())
