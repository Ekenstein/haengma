package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.flip

/**
 * Represents a board position.
 * @param stones The stones the position contains
 * @param boardSize The size of the board where the first item is the width and the second item is the height
 * @param blackCaptures the number of stones black has captured
 * @param whiteCaptures the number of stones white has captured.
 */
data class Board(
    val stones: Map<SgfPoint, SgfColor>,
    val boardSize: Pair<Int, Int>,
    val blackCaptures: Int,
    val whiteCaptures: Int
) {
    /**
     * The width of the board.
     */
    val width = boardSize.first

    /**
     * The height of the board.
     */
    val height = boardSize.second

    private val allIntersections by lazy {
        (0 until (width * height)).map {
            val x = (it % width)
            val y = (it / height)
            SgfPoint(x + 1, y + 1)
        }.toSet()
    }

    /**
     * Returns all the empty intersections of the board as a set of points.
     */
    val emptyIntersections by lazy {
        allIntersections.filter { it !in stones }.toSet()
    }

    companion object {
        /**
         * Returns an empty board position where the width and height of the board is the given [boardSize]
         * @param boardSize The width and height of the board.
         */
        fun empty(boardSize: Int) = empty(boardSize to boardSize)

        /**
         * Returns an empty board position with the given [boardSize].
         *
         * @param boardSize The size of the board where the first item is the width of the board and the second item
         *                  is the height of the board.
         */
        fun empty(boardSize: Pair<Int, Int>) = Board(
            stones = emptyMap(),
            boardSize = boardSize,
            blackCaptures = 0,
            whiteCaptures = 0
        )
    }
}

/**
 * Returns a text representation of the given board.
 */
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

/**
 * Removes the group of stones connected to the given [point]
 * from the board and increases the captures for the opponent.
 * If there is no stone at the given [point], this is a no-op.
 */
fun Board.removeGroup(point: SgfPoint): Board {
    val color = stones[point]
        ?: return this

    val group = getGroupContainingStone(color, point)
    val captureCount = group.size

    return increaseCaptureCount(color.flip(), captureCount).copy(
        stones = stones - group
    )
}

/**
 * Counts the territory and returns the difference between the territories.
 * If the result is negative, white is leading by the returned delta, otherwise black is leading.
 */
fun Board.count(komi: Double): Double {
    val territoryByColor = getTerritories()
    val blackTerritory = territoryByColor[SgfColor.Black].orEmpty().count()
    val whiteTerritory = territoryByColor[SgfColor.White].orEmpty().count()

    return blackTerritory - (whiteTerritory + komi)
}

/**
 * Returns the mapped out territories. Dame, e.g. a territory that is shared
 * by both white and black counts as a no-man's territory and thus will not be returned.
 */
fun Board.getTerritories(): Map<SgfColor, Set<SgfPoint>> {
    val emptyIntersections = emptyIntersections.toMutableSet()
    val territories = mutableListOf<Set<SgfPoint>>()

    while (emptyIntersections.isNotEmpty()) {
        val point = emptyIntersections.first()
        val territory = buildTerritoryFromPoint(point)
        territories += territory
        emptyIntersections -= territory
    }

    return territories
        .associateBy { territory -> getOwnerOfTerritory(territory) }
        .filterKeys { it != null }
        .mapKeys { (color, _) -> color!! }
}

private fun Board.getOwnerOfTerritory(territory: Set<SgfPoint>): SgfColor? {
    val colors = territory
        .flatMap { point -> point.adjacentPoints(boardSize).map { stones[it] } }
        .filterNotNull()
        .toSet()

    return colors.singleOrNull()
}

private fun Board.buildTerritoryFromPoint(point: SgfPoint): Set<SgfPoint> {
    val territory = mutableSetOf<SgfPoint>()
    fun buildTerritory(point: SgfPoint) {
        territory.add(point)
        point.adjacentPoints(boardSize).filter {
            it !in territory && it in emptyIntersections
        }.forEach {
            buildTerritory(it)
        }
    }

    buildTerritory(point)
    return territory
}

/**
 * Places a stone of [color] at the given [point] and returns a new board with the updated position.
 *
 * If the stone captures some opponent stones, the number of captured stones are updated accordingly for the player,
 * otherwise if the placed stone results in a suicide, the number of captured stones for the opponent is updated
 * accordingly.
 *
 * If the stone is placed outside the board, an [IllegalArgumentException] will be thrown.
 *
 * Note that this function does not validate whether the given move is valid or not, just executes it.
 * If you wish to validate a move, look at [SgfEditor.placeStone]
 */
fun Board.placeStone(color: SgfColor, point: SgfPoint): Board {
    require(point.x in 1..width && point.y in 1..height) {
        "The stone must not be placed outside of the board."
    }

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
        stone == color.flip()
    }

    val deadLiberties = adjacentPoints.count { it in group }
    totalPossibleLiberties - enemies - deadLiberties
}

/**
 * Returns true if the given [point] on the board is occupied by another stone, otherwise false.
 */
fun Board.isOccupied(point: SgfPoint) = stones.containsKey(point)

/**
 * Returns true if the given point on the board is occupied by another stone, otherwise false.
 */
fun Board.isOccupied(x: Int, y: Int) = isOccupied(SgfPoint(x, y))

/**
 * Rotates the board 90 degrees to the left.
 */
fun Board.rotateLeft(): Board = copy(
    stones = stones.mapKeys { (point, _) ->
        SgfPoint((height - point.y) + 1, point.x)
    }
)

/**
 * Mirrors the position
 */
fun Board.mirror(): Board = copy(
    stones = stones.mapKeys { (point, _) ->
        SgfPoint((width - point.x) + 1, point.y)
    }
)

/**
 * Returns a canonical hash for the current position.
 */
fun Board.canonicalHash(): Int {
    tailrec fun Board.transpose(step: Int, result: List<Int>): List<Int> {
        if (step <= 0) {
            return result
        }

        val rotated = rotateLeft()
        val mirrored = rotated.mirror()
        val hashes = listOf(
            rotated.stones.hashCode(),
            mirrored.stones.hashCode()
        )
        return rotated.transpose(step - 1, result + hashes)
    }

    val transposes = transpose(4, emptyList())
    return transposes.minOf { it }
}
