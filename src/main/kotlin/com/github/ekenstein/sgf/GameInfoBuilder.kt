package com.github.ekenstein.sgf

interface PlayerBuilder {
    /**
     * The name of the player or null if the player name is unknown.
     * Default value is null.
     */
    var name: String?

    /**
     * The rank of the player or null if the player rank is unknown.
     * Default value is null.
     */
    var rank: String?

    /**
     * The team the player is part of, or null if the player is not part of a team
     * or the team is unknown.
     * Default value is null.
     */
    var team: String?
}

interface GameRuleBuilder {
    /**
     * The size of the board. This is assumed that the board is a square.
     * The board size must be larger than or equal to 1.
     * Default value is 19.
     * @throws IllegalArgumentException If the provided board size is invalid.
     */
    var boardSize: Int

    /**
     * The komi of the game. Default value is 0.0
     */
    var komi: Double

    /**
     * The handicap of the game. The allowed handicap is based on the size of the board.
     *
     * - If the size of the board is less than 7, the valid handicap value is 0.
     * - If the size of the board is 7, the valid handicap value is 0, or 2..4
     * - If the size of the board is even, the valid handicap value is 0, or 2..4
     * - Otherwise the valid handicap value is 0, or 2..9.
     *
     * Default handicap value is 0.
     * @throws IllegalArgumentException If the provided handicap value is invalid.
     */
    var handicap: Int
}

interface GameInfoBuilder {
    /**
     * The result of the game, if any. Default is null.
     */
    var result: GameResult?

    /**
     * Provides some extra information about the game.
     * The intent of the game comment is to provide some background information
     * and/or to summarize the game itself.
     */
    var gameComment: String?

    /**
     * Provides the date when the game was played.
     */
    var gameDate: List<GameDate>

    /**
     * Provides a name for the game. The name is used to
     * easily find games within a collection.
     */
    var gameName: String?

    /**
     * Provides the place where the game was played.
     */
    var gamePlace: String?

    /**
     * Provides the rules of the game.
     */
    fun rules(block: GameRuleBuilder.() -> Unit)

    /**
     * Provides information about the white player.
     */
    fun whitePlayer(block: PlayerBuilder.() -> Unit)

    /**
     * Provides information about the black player.
     */
    fun blackPlayer(block: PlayerBuilder.() -> Unit)
}

private class DefaultGameRuleBuilder(var rules: Rules) : GameRuleBuilder {
    override var boardSize: Int
        get() = rules.boardSize
        set(value) { rules = rules.copy(boardSize = value) }

    override var komi: Double
        get() = rules.komi
        set(value) { rules = rules.copy(komi = value) }

    override var handicap: Int
        get() = rules.handicap
        set(value) { rules = rules.copy(handicap = value) }
}

private class DefaultPlayerBuilder(var player: Player) : PlayerBuilder {
    override var name: String?
        get() = player.name
        set(value) { player = player.copy(name = value) }

    override var rank: String?
        get() = player.rank
        set(value) { player = player.copy(rank = value) }

    override var team: String?
        get() = player.team
        set(value) { player = player.copy(team = value) }
}

internal class DefaultGameInfoBuilder(var gameInfo: GameInfo) : GameInfoBuilder {
    override var result: GameResult?
        get() = gameInfo.result
        set(value) { gameInfo = gameInfo.copy(result = value) }

    override var gameComment: String?
        get() = gameInfo.gameComment
        set(value) { gameInfo = gameInfo.copy(gameComment = value) }

    override var gameDate: List<GameDate>
        get() = gameInfo.gameDate
        set(value) { gameInfo = gameInfo.copy(gameDate = value) }

    override var gameName: String?
        get() = gameInfo.gameName
        set(value) { gameInfo = gameInfo.copy(gameName = value) }

    override var gamePlace: String?
        get() = gameInfo.gamePlace
        set(value) { gameInfo = gameInfo.copy(gamePlace = value) }

    override fun rules(block: GameRuleBuilder.() -> Unit) {
        val builder = DefaultGameRuleBuilder(gameInfo.rules)
        builder.block()
        gameInfo = gameInfo.copy(rules = builder.rules)
    }

    override fun whitePlayer(block: PlayerBuilder.() -> Unit) {
        val builder = DefaultPlayerBuilder(gameInfo.whitePlayer)
        builder.block()
        gameInfo = gameInfo.copy(whitePlayer = builder.player)
    }

    override fun blackPlayer(block: PlayerBuilder.() -> Unit) {
        val builder = DefaultPlayerBuilder(gameInfo.blackPlayer)
        builder.block()
        gameInfo = gameInfo.copy(blackPlayer = builder.player)
    }
}

fun gameInfo(block: GameInfoBuilder.() -> Unit): GameInfo = gameInfo(GameInfo.default, block)

fun gameInfo(gameInfo: GameInfo, block: GameInfoBuilder.() -> Unit): GameInfo {
    val builder = DefaultGameInfoBuilder(gameInfo)
    builder.block()
    return builder.gameInfo
}
