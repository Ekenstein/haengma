package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.flip

data class Board(
    val stones: List<Stone>,
    val boardSize: Pair<Int, Int>,
    val blackCaptures: Int,
    val whiteCaptures: Int
) {
    companion object {
        fun empty(boardSize: Int) = empty(boardSize to boardSize)

        fun empty(boardSize: Pair<Int, Int>) = Board(
            stones = emptyList(),
            boardSize = boardSize,
            blackCaptures = 0,
            whiteCaptures = 0
        )
    }
}
data class Stone(val color: SgfColor, val point: SgfPoint)

fun Board.print(): String {
    val sb = StringBuilder()
    val (width, height) = boardSize
    (1..height).forEach { y ->
        (1..width).forEach { x ->
            val point = SgfPoint(x, y)
            when (stones.filter { it.point == point }.map { it.color }.singleOrNull()) {
                null -> sb.append(" . ")
                SgfColor.Black -> sb.append(" # ")
                SgfColor.White -> sb.append(" O ")
            }
        }
        sb.appendLine()
    }

    return sb.toString()
}

private fun Stone.adjacentPoints(boardSize: Pair<Int, Int>): Set<SgfPoint> = setOf(
    SgfPoint(point.x, point.y - 1),
    SgfPoint(point.x, point.y + 1),
    SgfPoint(point.x - 1, point.y),
    SgfPoint(point.x + 1, point.y),
).filter { (x, y) -> x in 1..boardSize.first && y in 1..boardSize.second }.toSet()

fun Board.placeStone(stone: Stone): Board {
    val board = copy(stones = stones + stone)
    val adjacentStones = board.stones.filter {
        it.point in stone.adjacentPoints(boardSize) && it.color != stone.color
    }

    return adjacentStones.fold(board) { b, s -> b.removeConnectedStonesIfTheyAreDead(s) }
        .removeConnectedStonesIfTheyAreDead(stone)
}

private fun Board.removeConnectedStonesIfTheyAreDead(stone: Stone): Board {
    val group = getGroupContainingStone(stone)
    val liberties = countLibertiesForGroup(group)

    return if (liberties <= 0) {
        copy(stones = stones - group).increaseCaptureCount(stone.color.flip(), group.size)
    } else {
        this
    }
}

private fun Board.increaseCaptureCount(color: SgfColor, numberOfCaptures: Int) = when (color) {
    SgfColor.Black -> copy(blackCaptures = blackCaptures + numberOfCaptures)
    SgfColor.White -> copy(whiteCaptures = whiteCaptures + numberOfCaptures)
}

private fun Board.getGroupContainingStone(stone: Stone): Set<Stone> {
    fun buildGroup(group: Set<Stone>, stone: Stone): Set<Stone> {
        val adjacentStones = stones.filter {
            it.point in stone.adjacentPoints(boardSize) && it !in group && it.color == stone.color
        }

        return adjacentStones.fold(group + adjacentStones, ::buildGroup)
    }

    return buildGroup(setOf(stone), stone)
}

private fun Board.countLibertiesForGroup(group: Set<Stone>): Int = group.sumOf { stone ->
    val adjacentPoints = stone.adjacentPoints(boardSize)
    val totalPossibleLiberties = adjacentPoints.size
    val enemies = stones.count {
        it.point in adjacentPoints && it.color != stone.color
    }
    val deadLiberties = group.count { it.point in adjacentPoints }

    totalPossibleLiberties - enemies - deadLiberties
}
