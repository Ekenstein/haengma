package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.GameResult
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.plus
import com.github.ekenstein.sgf.extensions.removeProperty

sealed class Overtime {
    data class ByoYomi(val periods: Int, val seconds: Int) : Overtime()
}

private val Overtime.asString
    get() = when (this) {
        is Overtime.ByoYomi -> "${periods}x$seconds"
    }

sealed class Rank {
    data class Dan(val value: Int) : Rank() {
        init {
            require(value in 1..9) {
                "The rank dan must be within the range 1-9"
            }
        }
    }
    data class Kyu(val value: Int) : Rank() {
        init {
            require(value in 1..30) {
                "The rank kyu must be within the range 1-30"
            }
        }
    }

    data class Pro(val value: Int) : Rank() {
        init {
            require(value in 1..9) {
                "The rank pro must be within the range 1-9"
            }
        }
    }

    object Unknown : Rank()
}

private val Rank.asString
    get() = when (this) {
        is Rank.Dan -> "${value}d"
        is Rank.Kyu -> "${value}k"
        is Rank.Pro -> "${value}p"
        Rank.Unknown -> "?"
    }

@SgfDslMarker
interface GameInfoNodeBuilder : NodeBuilder {
    /**
     * Sets the handicap information about the game. Note that this will not add
     * the initial position for the board. See [SetupBuilder.stones] if you wish
     * to set up the board in its initial position.
     *
     * If the [numberOfStones] is less than 2, the property will not be added to the tree.
     */
    fun handicap(numberOfStones: Int)
    fun komi(value: Double)
    fun result(value: GameResult)
    fun event(name: String)
    fun playerBlack(name: String)
    fun playerWhite(name: String)
    fun blackRank(rank: String)
    fun blackRank(rank: Rank)
    fun whiteRank(rank: String)
    fun whiteRank(rank: Rank)
    fun gameDate(vararg date: GameDate)
    fun timeLimit(value: Double)
    fun overtime(value: Overtime)
    fun overtime(value: String)
}

internal class DefaultGameInfoNodeBuilder(override var node: SgfNode) : GameInfoNodeBuilder, DefaultNodeBuilder() {
    override fun handicap(numberOfStones: Int) {
        if (numberOfStones < 2) {
            node = node.removeProperty<SgfProperty.GameInfo.HA>()
        } else {
            node += SgfProperty.GameInfo.HA(numberOfStones)
        }
    }

    override fun komi(value: Double) {
        node += SgfProperty.GameInfo.KM(value)
    }

    override fun result(value: GameResult) {
        node += SgfProperty.GameInfo.RE(value)
    }

    override fun event(name: String) {
        node += SgfProperty.GameInfo.EV(name)
    }

    override fun playerBlack(name: String) {
        node += SgfProperty.GameInfo.PB(name)
    }

    override fun playerWhite(name: String) {
        node += SgfProperty.GameInfo.PW(name)
    }

    override fun blackRank(rank: String) {
        node += SgfProperty.GameInfo.BR(rank)
    }

    override fun blackRank(rank: Rank) {
        blackRank(rank.asString)
    }

    override fun whiteRank(rank: String) {
        node += SgfProperty.GameInfo.WR(rank)
    }

    override fun whiteRank(rank: Rank) {
        whiteRank(rank.asString)
    }

    override fun gameDate(vararg date: GameDate) {
        node += SgfProperty.GameInfo.DT(date.toList())
    }

    override fun timeLimit(value: Double) {
        node += SgfProperty.GameInfo.TM(value)
    }

    override fun overtime(value: Overtime) {
        overtime(value.asString)
    }

    override fun overtime(value: String) {
        node += SgfProperty.GameInfo.OT(value)
    }
}
