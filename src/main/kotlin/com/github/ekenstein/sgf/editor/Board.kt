package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.flip

data class Board(
    val stones: Map<SgfPoint, SgfColor>,
    val boardSize: Pair<Int, Int>,
    val blackCaptures: Int,
    val whiteCaptures: Int
) {
    companion object {
        fun empty(boardSize: Int) = empty(boardSize to boardSize)

        fun empty(boardSize: Pair<Int, Int>) = Board(
            stones = emptyMap(),
            boardSize = boardSize,
            blackCaptures = 0,
            whiteCaptures = 0
        )
    }
}

fun Board.print(): String {
    val sb = StringBuilder()
    val (width, height) = boardSize
    (1..height).forEach { y ->
        (1..width).forEach { x ->
            val point = SgfPoint(x, y)
            when (stones[point]) {
                SgfColor.Black -> sb.append(" # ")
                SgfColor.White -> sb.append(" O ")
                null -> sb.append(" . ")
            }
        }
        sb.appendLine()
    }

    return sb.toString()
}

fun Board.placeStone(color: SgfColor, point: SgfPoint): Board {
    val updatedBoard = copy(stones = stones + (point to color))
    val enemyColor = color.flip()
    val enemyAdjacentPoints = point.adjacentPoints(boardSize).filter {
        updatedBoard.stones[it] == enemyColor
    }

    return enemyAdjacentPoints.fold(updatedBoard) { board, enemyPoint ->
        board.removeConnectedStonesIfTheyAreDead(enemyColor, enemyPoint)
    }.removeConnectedStonesIfTheyAreDead(color, point)
}

private fun Board.removeConnectedStonesIfTheyAreDead(color: SgfColor, point: SgfPoint): Board {
    val group = getGroupContainingStone(color, point)
    val liberties = countLibertiesForGroup(color, group)

    return if (liberties <= 0) {
        copy(stones = stones - group).increaseCaptureCount(color.flip(), group.size)
    } else {
        this
    }
}

private fun Board.increaseCaptureCount(color: SgfColor, numberOfCaptures: Int) = when (color) {
    SgfColor.Black -> copy(blackCaptures = blackCaptures + numberOfCaptures)
    SgfColor.White -> copy(whiteCaptures = whiteCaptures + numberOfCaptures)
}

private fun Board.getGroupContainingStone(color: SgfColor, point: SgfPoint): Set<SgfPoint> {
    val currentBoard = stones.toMutableMap()
    val currentGroup = mutableSetOf(point)

    fun buildGroup(point: SgfPoint) {
        val adjacentPoints = point.adjacentPoints(boardSize)
        adjacentPoints.forEach { adjacentPoint ->
            if (currentBoard[adjacentPoint] == color) {
                currentBoard.remove(adjacentPoint)
                currentGroup.add(adjacentPoint)
                buildGroup(adjacentPoint)
            }
        }
    }

    buildGroup(point)
    return currentGroup
}

private fun SgfPoint.adjacentPoints(boardSize: Pair<Int, Int>): Set<SgfPoint> {
    val adjacentPoints = setOf(
        SgfPoint(x, y - 1),
        SgfPoint(x, y + 1),
        SgfPoint(x - 1, y),
        SgfPoint(x + 1, y),
    )
    val (width, height) = boardSize

    return adjacentPoints.filter { (x, y) ->
        x in 1..width && y in 1..height
    }.toSet()
}

private fun Board.countLibertiesForGroup(color: SgfColor, group: Set<SgfPoint>): Int = group.sumOf { point ->
    val adjacentPoints = point.adjacentPoints(boardSize)
    val totalPossibleLiberties = adjacentPoints.size
    val enemies = adjacentPoints.count {
        val stone = stones[it]
        stone != null && stone != color
    }

    val deadLiberties = adjacentPoints.count { it in group }
    totalPossibleLiberties - enemies - deadLiberties
}

fun Board.isOccupied(point: SgfPoint) = stones.containsKey(point)
fun Board.isOccupied(x: Int, y: Int) = isOccupied(SgfPoint(x, y))
