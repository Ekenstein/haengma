package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.extensions.property
import com.github.ekenstein.sgf.utils.nelOf
import kotlin.math.ceil

private const val DEFAULT_SIZE = 19
private const val DEFAULT_KOMI = 0.0
private const val DEFAULT_HANDICAP = 0
private const val DEFAULT_GAME_COMMENT = ""

data class Rules(
    var boardSize: Int,
    var komi: Double,
    var handicap: Int
) {
    init {
        val maxHandicap = maxHandicapForBoardSize(boardSize)
        require(handicap == 0 || handicap in 2..maxHandicap) {
            "Invalid handicap $handicap. The handicap must be 0 or between 2 and $maxHandicap"
        }
    }

    companion object {
        val default
            get() = Rules(DEFAULT_SIZE, DEFAULT_KOMI, DEFAULT_HANDICAP)
    }
}

private fun Rules.toSgfProperties(): Set<SgfProperty> {
    val handicapProperties = handicap.takeIf { it >= 2 }?.let {
        setOf(
            SgfProperty.GameInfo.HA(it),
            SgfProperty.Setup.AB(handicapPoints(it, boardSize))
        )
    } ?: emptySet()

    return setOf(
        SgfProperty.Root.SZ(boardSize),
        SgfProperty.GameInfo.KM(komi)
    ) + handicapProperties
}

private fun maxHandicapForBoardSize(boardSize: Int) = when {
    boardSize < 7 -> 0
    boardSize == 7 -> 4
    boardSize % 2 == 0 -> 4
    else -> 9
}

private fun handicapPoints(handicap: Int, boardSize: Int): Set<SgfPoint> {
    val edgeDistance = edgeDistance(boardSize) ?: return emptySet()
    val middle = ceil(boardSize / 2.0).toInt()
    val tengen = SgfPoint(middle, middle)

    fun points(handicap: Int): Set<SgfPoint> = when (handicap) {
        2 -> setOf(
            SgfPoint(x = edgeDistance, y = boardSize - edgeDistance + 1),
            SgfPoint(x = boardSize - edgeDistance + 1, y = edgeDistance)
        )
        3 -> setOf(SgfPoint(x = boardSize - edgeDistance + 1, y = boardSize - edgeDistance + 1)) + points(2)
        4 -> setOf(SgfPoint(x = edgeDistance, y = edgeDistance)) + points(3)
        5 -> setOf(tengen) + points(4)
        6 -> setOf(
            SgfPoint(x = edgeDistance, y = middle),
            SgfPoint(x = boardSize - edgeDistance + 1, y = middle)
        ) + points(4)
        7 -> setOf(tengen) + points(6)
        8 -> setOf(
            SgfPoint(middle, edgeDistance),
            SgfPoint(middle, boardSize - edgeDistance + 1)
        ) + points(6)
        9 -> setOf(tengen) + points(8)
        else -> emptySet()
    }

    return points(handicap)
}

private fun edgeDistance(boardSize: Int) = when {
    boardSize < 7 -> null
    boardSize < 13 -> 3
    else -> 4
}

data class GameInfo(
    val rules: Rules,
    var gameComment: String,
    var gameDate: List<GameDate>,
) {
    val gameType = GameType.Go
    val fileFormat = 4

    companion object {
        val default: GameInfo
            get() = GameInfo(
                rules = Rules.default,
                gameComment = DEFAULT_GAME_COMMENT,
                gameDate = emptyList()
            )
    }
}

internal fun GameInfo.toSgfProperties(): Set<SgfProperty> {
    val ruleProperties = rules.toSgfProperties()
    return setOfNotNull(
        gameComment.takeIf { it.isNotBlank() }?.let { SgfProperty.GameInfo.GC(it) },
        gameDate.takeIf { it.isNotEmpty() }?.let { SgfProperty.GameInfo.DT(it) },
        SgfProperty.Root.GM(gameType),
        SgfProperty.Root.FF(fileFormat)
    ) + ruleProperties
}

internal fun SgfNode.updateGameInfo(gameInfo: GameInfo): SgfNode {
    val properties = gameInfo.toSgfProperties()
    val cleanNode = copy(
        properties = properties.filter {
            when (it) {
                is SgfProperty.GameInfo.HA,
                is SgfProperty.GameInfo.DT,
                is SgfProperty.GameInfo.KM,
                is SgfProperty.GameInfo.GC,
                is SgfProperty.Root.SZ -> false
                else -> true
            }
        }.toSet()
    )

    return properties.fold(cleanNode) { n, p -> n.addProperty(p) }
}

internal fun SgfNode.getGameInfo() = GameInfo(
    rules = Rules(
        boardSize = property<SgfProperty.Root.SZ>()?.width
            ?: DEFAULT_SIZE,
        komi = property<SgfProperty.GameInfo.KM>()?.komi
            ?: DEFAULT_KOMI,
        handicap = property<SgfProperty.GameInfo.HA>()?.numberOfStones
            ?: DEFAULT_HANDICAP
    ),
    gameComment = property<SgfProperty.GameInfo.GC>()?.comment
        ?: DEFAULT_GAME_COMMENT,
    gameDate = property<SgfProperty.GameInfo.DT>()?.dates
        ?: emptyList()
)

internal fun GameInfo.toGameTree(): SgfGameTree = SgfGameTree(nelOf(SgfNode(toSgfProperties())))
