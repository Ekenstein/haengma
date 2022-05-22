package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.GameResult
import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.PropertySet
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.emptyPropertySet
import com.github.ekenstein.sgf.propertySetOf
import com.github.ekenstein.sgf.propertySetOfNotNull
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

private fun Rules.toSgfProperties(): PropertySet {
    val handicapProperties = handicap.takeIf { it >= 2 }?.let {
        propertySetOf(
            SgfProperty.GameInfo.HA(it),
            SgfProperty.Setup.AB(handicapPoints(it, boardSize))
        )
    } ?: emptyPropertySet()

    return propertySetOf(
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

data class Player(
    var name: String?,
    var rank: String?,
    var team: String?
) {
    companion object {
        val default: Player
            get() = Player(null, null, null)
    }
}

private fun Player.toSgfProperties(color: SgfColor) = when (color) {
    SgfColor.Black -> propertySetOfNotNull(
        name?.let { SgfProperty.GameInfo.PB(it) },
        rank?.let { SgfProperty.GameInfo.BR(it) },
        team?.let { SgfProperty.GameInfo.BT(it) }
    )
    SgfColor.White -> propertySetOfNotNull(
        name?.let { SgfProperty.GameInfo.PW(it) },
        rank?.let { SgfProperty.GameInfo.WR(it) },
        team?.let { SgfProperty.GameInfo.WT(it) }
    )
}

data class GameInfo(
    val rules: Rules,
    val blackPlayer: Player,
    val whitePlayer: Player,
    var result: GameResult?,
    var gameComment: String?,
    var gameDate: List<GameDate>,
    var gameName: String?,
    var gamePlace: String?
) {
    val gameType = GameType.Go
    val fileFormat = 4

    companion object {
        val default: GameInfo
            get() = GameInfo(
                rules = Rules.default,
                blackPlayer = Player.default,
                whitePlayer = Player.default,
                result = null,
                gameComment = null,
                gameDate = emptyList(),
                gameName = null,
                gamePlace = null
            )
    }
}

internal fun GameInfo.toSgfProperties(): PropertySet {
    val ruleProperties = rules.toSgfProperties()
    val blackPlayerProperties = blackPlayer.toSgfProperties(SgfColor.Black)
    val whitePlayerProperties = whitePlayer.toSgfProperties(SgfColor.White)

    return propertySetOfNotNull(
        gameComment?.let { SgfProperty.GameInfo.GC(it) },
        gameDate.takeIf { it.isNotEmpty() }?.let { SgfProperty.GameInfo.DT(it) },
        SgfProperty.Root.GM(gameType),
        SgfProperty.Root.FF(fileFormat),
        gamePlace?.let { SgfProperty.GameInfo.PC(it) },
        gameName?.let { SgfProperty.GameInfo.GN(it) },
        result?.let { SgfProperty.GameInfo.RE(it) }
    ) + ruleProperties + blackPlayerProperties + whitePlayerProperties
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
    gameComment = property<SgfProperty.GameInfo.GC>()?.comment,
    gameDate = property<SgfProperty.GameInfo.DT>()?.dates
        ?: emptyList(),
    blackPlayer = Player(
        name = property<SgfProperty.GameInfo.PB>()?.name,
        rank = property<SgfProperty.GameInfo.BR>()?.rank,
        team = property<SgfProperty.GameInfo.BT>()?.team
    ),
    whitePlayer = Player(
        name = property<SgfProperty.GameInfo.PW>()?.name,
        rank = property<SgfProperty.GameInfo.WR>()?.rank,
        team = property<SgfProperty.GameInfo.WT>()?.team
    ),
    gameName = property<SgfProperty.GameInfo.GN>()?.name,
    gamePlace = property<SgfProperty.GameInfo.PC>()?.place,
    result = property<SgfProperty.GameInfo.RE>()?.result
)

internal fun GameInfo.toGameTree(): SgfGameTree = SgfGameTree(nelOf(SgfNode(toSgfProperties())))
