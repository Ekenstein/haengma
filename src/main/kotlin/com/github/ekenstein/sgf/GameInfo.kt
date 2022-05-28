package com.github.ekenstein.sgf

import kotlin.math.ceil

private const val DEFAULT_SIZE = 19
private const val DEFAULT_KOMI = 0.0
private const val DEFAULT_HANDICAP = 0

/**
 * Provides rules for the game.
 *
 * @param boardSize The size of the board. Must be greater or equal to 1. This presumes that the board is a square.
 * @param komi The komi of the game.
 * @param handicap The number of handicap stones that black has.
 *  The valid number of handicap stones is based on the size of the board.
 *  - If the size of the board is less than 7, the valid handicap value is 0.
 *  - If the size of the board is 7, the valid handicap value is 0, or 2..4
 *  - If the size of the board is even, the valid handicap value is 0, or 2..4
 *  - Otherwise the valid handicap value is 0, or 2..9.
 */
data class Rules(
    val boardSize: Int,
    val komi: Double,
    val handicap: Int
) {
    init {
        require(boardSize >= 1) {
            "The board size must be greater or equal to 1."
        }

        val maxHandicap = maxHandicapForBoardSize(boardSize)
        require(handicap == 0 || handicap in 2..maxHandicap) {
            "Invalid handicap $handicap. The handicap must be 0 or between 2 and $maxHandicap"
        }
    }

    companion object {
        val default = Rules(DEFAULT_SIZE, DEFAULT_KOMI, DEFAULT_HANDICAP)
    }
}

private fun edgeDistance(boardSize: Int) = when {
    boardSize < 7 -> null
    boardSize < 13 -> 3
    else -> 4
}

/**
 * Provides information about a player that is part of the game.
 * @param name The name of the player.
 * @param rank The rank of the player.
 *  For Go the following format is recommended:
 *  - "..k" or "..kyu" for kyu ranks and
 *  - "..d" or "..dan" for dan ranks.
 *
 *  Go servers may want to add '?' for an uncertain rating and
 *  '*' for an established rating.
 * @param team Provides the name of the player team, if game was part of a
 *  team-match (e.g. China-Japan Supermatch)
 */
data class Player(
    val name: String?,
    val rank: String?,
    val team: String?
) {
    companion object {
        val default: Player = Player(null, null, null)
    }
}

/**
 * Provides information about the game.
 */
data class GameInfo(
    val rules: Rules,
    val blackPlayer: Player,
    val whitePlayer: Player,
    val result: GameResult?,
    val gameComment: String?,
    val gameDate: List<GameDate>,
    val gameName: String?,
    val gamePlace: String?
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

private fun Player.toSgfProperties(color: SgfColor): PropertySet = when (color) {
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
