package com.github.ekenstein.sgf

import com.github.ekenstein.sgf.utils.NonEmptyList
import com.github.ekenstein.sgf.utils.nelOf
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Represents a collection of [SgfGameTree]. It is mandatory to have at least one [SgfGameTree]
 * in a [SgfCollection].
 */
data class SgfCollection(val trees: NonEmptyList<SgfGameTree>) {
    constructor(tree: SgfGameTree, vararg trees: SgfGameTree) : this(nelOf(tree, *trees))
    /**
     * Filter game trees by its game information.
     */
    fun filter(predicate: (GameInfo) -> Boolean): List<SgfGameTree> = trees.filter {
        val gameInfo = it.sequence.head.getGameInfo()
        predicate(gameInfo)
    }

    companion object
}

/**
 * Represents an SGF tree. A [SgfGameTree] has 1 or more [SgfNode] and 0 or more [trees] at the end
 * of the [sequence].
 */
data class SgfGameTree(val sequence: NonEmptyList<SgfNode>, val trees: List<SgfGameTree>) {
    constructor(sequence: NonEmptyList<SgfNode>) : this(sequence, emptyList())
    constructor(node: SgfNode, vararg nodes: SgfNode) : this(nelOf(node, *nodes))
}

/**
 * Represents a node in a [SgfGameTree]. A node contains 0 or more unique [SgfProperty].
 * E.g. a node can't contain two or more properties of the same type, such as [SgfProperty.Move.B]
 */
data class SgfNode(val properties: PropertySet) {
    constructor(vararg property: SgfProperty) : this(propertySetOf(*property))

    /**
     * Returns the property corresponding to the given type [T] or null if there is no such property
     * on this node.
     */
    inline fun <reified T : SgfProperty> property() = properties.filterIsInstance<T>().singleOrNull()

    /**
     * Checks whether this node has a property of type [T] or not. True if it has a property of that type,
     * otherwise false.
     */
    inline fun <reified T : SgfProperty> hasProperty() = properties.filterIsInstance<T>().any()
}

sealed class SgfProperty {
    internal abstract val identifier: String

    sealed class Move : SgfProperty() {
        data class B(val move: com.github.ekenstein.sgf.Move) : Move() {
            constructor(x: Int, y: Int) : this(com.github.ekenstein.sgf.Move.Stone(SgfPoint(x, y)))
            companion object {
                fun pass() = B(com.github.ekenstein.sgf.Move.Pass)
            }

            override val identifier: String = "B"
        }
        data class W(val move: com.github.ekenstein.sgf.Move) : Move() {
            constructor(x: Int, y: Int) : this(com.github.ekenstein.sgf.Move.Stone(SgfPoint(x, y)))
            companion object {
                fun pass() = B(com.github.ekenstein.sgf.Move.Pass)
            }

            override val identifier: String = "W"
        }
        object KO : Move() {
            override val identifier: String = "KO"
        }
        data class MN(val number: Int) : Move() {
            override val identifier: String = "MN"
        }
    }

    sealed class Setup : SgfProperty() {
        data class AB(val points: Set<SgfPoint>) : Setup() {
            override val identifier: String = "AB"
        }

        data class AW(val points: Set<SgfPoint>) : Setup() {
            override val identifier: String = "AW"
        }
        data class AE(val points: Set<SgfPoint>) : Setup() {
            override val identifier: String = "AE"
        }
        data class PL(val color: SgfColor) : Setup() {
            override val identifier: String = "PL"
        }
    }

    sealed class NodeAnnotation : SgfProperty() {
        data class C(val comment: String) : NodeAnnotation() {
            override val identifier: String = "C"
        }
        data class DM(val value: SgfDouble) : NodeAnnotation() {
            override val identifier: String = "DM"
        }
        data class GB(val value: SgfDouble) : NodeAnnotation() {
            override val identifier: String = "GB"
        }
        data class GW(val value: SgfDouble) : NodeAnnotation() {
            override val identifier: String = "GW"
        }
        data class HO(val value: SgfDouble) : NodeAnnotation() {
            override val identifier: String = "HO"
        }
        data class N(val name: String) : NodeAnnotation() {
            override val identifier: String = "N"
        }
        data class UC(val value: SgfDouble) : NodeAnnotation() {
            override val identifier: String = "UC"
        }
        data class V(val value: Double) : NodeAnnotation() {
            override val identifier: String = "V"
        }
    }

    sealed class MoveAnnotation : SgfProperty() {
        data class BM(val value: SgfDouble) : MoveAnnotation() {
            override val identifier: String = "BM"
        }
        object DO : MoveAnnotation() {
            override val identifier: String = "DO"
        }
        object IT : MoveAnnotation() {
            override val identifier: String = "IT"
        }
        data class TE(val value: SgfDouble) : MoveAnnotation() {
            override val identifier: String = "TE"
        }
    }

    sealed class Markup : SgfProperty() {
        data class AR(val points: List<Pair<SgfPoint, SgfPoint>>) : Markup() {
            override val identifier: String = "AR"
        }
        data class CR(val points: Set<SgfPoint>) : Markup() {
            override val identifier: String = "CR"
        }
        data class LB(val label: List<Pair<SgfPoint, String>>) : Markup() {
            override val identifier: String = "LB"
        }
        data class LN(val line: List<Pair<SgfPoint, SgfPoint>>) : Markup() {
            override val identifier: String = "LN"
        }
        data class MA(val points: Set<SgfPoint>) : Markup() {
            override val identifier: String = "MA"
        }
        data class SL(val selected: Set<SgfPoint>) : Markup() {
            override val identifier: String = "SL"
        }
        data class SQ(val points: Set<SgfPoint>) : Markup() {
            override val identifier: String = "SQ"
        }
        data class TR(val points: Set<SgfPoint>) : Markup() {
            override val identifier: String = "TR"
        }
        data class DD(val points: Set<SgfPoint>) : Markup() {
            override val identifier: String = "DD"
        }
    }

    sealed class Root : SgfProperty() {
        data class AP(val name: String, val version: String) : Root() {
            override val identifier: String = "AP"
        }
        data class FF(val format: Int) : Root() {
            override val identifier: String = "FF"
        }

        /**
         * Defines the size of the board. If only a single value
         * is given, the board is a square; with two numbers given,
         * rectangular boards are possible.
         * If a rectangular board is specified, the first number specifies
         * the number of columns, the second provides the number of rows.
         * The valid range for SZ is any size greater or equal to 1x1.
         */
        data class SZ(val width: Int, val height: Int) : Root() {
            override val identifier: String = "SZ"
            constructor(size: Int) : this(size, size)

            init {
                require(width >= 1 && height >= 1) {
                    "The size of the board must be greater or equal to 1x1."
                }
            }
        }
        data class CA(val charset: Charset) : Root() {
            override val identifier: String = "CA"
        }
        data class GM(val game: GameType) : Root() {
            override val identifier: String = "GM"
        }
        data class ST(val style: Int) : Root() {
            init {
                require(style in 0..3) {
                    "The style value must be between 0 and 3"
                }
            }
            override val identifier: String = "ST"
        }
    }

    sealed class GameInfo : SgfProperty() {
        /**
         * Defines the number of handicap stones (>=2).
         * If there is a handicap, the position should be set up with
         * AB within the same node.
         * HA itself doesn't add any stones to the board, nor does
         * it imply any particular way of placing the handicap stones.
         */
        data class HA(val numberOfStones: Int) : GameInfo() {
            override val identifier: String = "HA"
            init {
                require(numberOfStones >= 2) {
                    "Handicap must be larger or equal to 2"
                }
            }
        }
        data class KM(val komi: Double) : GameInfo() {
            override val identifier: String = "KM"
        }
        data class EV(val event: String) : GameInfo() {
            override val identifier: String = "EV"
        }
        data class PB(val name: String) : GameInfo() {
            override val identifier: String = "PB"
        }
        data class PW(val name: String) : GameInfo() {
            override val identifier: String = "PW"
        }
        data class RE(val result: GameResult) : GameInfo() {
            override val identifier: String = "RE"
        }
        data class BR(val rank: String) : GameInfo() {
            override val identifier: String = "BR"
        }
        data class WR(val rank: String) : GameInfo() {
            override val identifier: String = "WR"
        }

        /**
         * Provides a name for the game. The name is used to
         * easily find games within a collection.
         * The name should therefore contain some helpful information
         * for identifying the game. 'GameName' could also be used
         * as the file-name, if a collection is split into
         * single files.
         */
        data class GN(val name: String) : GameInfo() {
            override val identifier: String = "GN"
        }
        data class DT(val dates: List<GameDate>) : GameInfo() {
            override val identifier: String = "DT"
            constructor(vararg date: GameDate) : this(date.toList())
            init {
                require(dates.isNotEmpty()) {
                    "The game dates must not be empty"
                }
            }
        }

        /**
         * Provides the time limits of the game.
         * The time limit is given in seconds.
         */
        data class TM(val timeLimitInSeconds: Double) : GameInfo() {
            override val identifier: String = "TM"
        }
        data class SO(val source: String) : GameInfo() {
            override val identifier: String = "SO"
        }
        data class GC(val comment: String) : GameInfo() {
            override val identifier: String = "GC"
        }
        data class ON(val opening: String) : GameInfo() {
            override val identifier: String = "ON"
        }
        data class OT(val overtime: String) : GameInfo() {
            override val identifier: String = "OT"
        }
        data class RO(val round: String) : GameInfo() {
            override val identifier: String = "RO"
        }
        data class RU(val rules: String) : GameInfo() {
            override val identifier: String = "RU"
        }
        data class US(val user: String) : GameInfo() {
            override val identifier: String = "US"
        }
        data class WT(val team: String) : GameInfo() {
            override val identifier: String = "WT"
        }
        data class BT(val team: String) : GameInfo() {
            override val identifier: String = "BT"
        }
        data class AN(val annotation: String) : GameInfo() {
            override val identifier: String = "AN"
        }
        data class CP(val copyright: String) : GameInfo() {
            override val identifier: String = "CP"
        }
        data class PC(val place: String) : GameInfo() {
            override val identifier: String = "PC"
        }
    }

    sealed class Timing : SgfProperty() {
        data class BL(val timeLeft: Double) : Timing() {
            override val identifier: String = "BL"
        }
        data class WL(val timeLeft: Double) : Timing() {
            override val identifier: String = "WL"
        }
        data class OB(val overtimeStones: Int) : Timing() {
            override val identifier: String = "OB"
        }
        data class OW(val overtimeStones: Int) : Timing() {
            override val identifier: String = "OW"
        }
    }

    sealed class Misc : SgfProperty() {
        data class FG(val value: Pair<Int, String>?) : Misc() {
            override val identifier: String = "FG"
            constructor() : this(null)
            constructor(diagramName: String, flag: Int) : this(flag to diagramName)
        }
        data class PM(val printMoveMode: Int) : Misc() {
            override val identifier: String = "PM"
        }
        data class VW(val points: Set<SgfPoint>) : Misc() {
            override val identifier: String = "VW"
        }
    }

    data class Private(override val identifier: String, val values: List<String>) : SgfProperty()
}

sealed class Move {
    data class Stone(val point: SgfPoint) : Move()
    object Pass : Move()
}

val Move.asPointOrNull: SgfPoint?
    get() = when (this) {
        Move.Pass -> null
        is Move.Stone -> point
    }

data class SgfPoint(val x: Int, val y: Int)

enum class SgfColor {
    Black,
    White;
}

fun SgfColor.flip(): SgfColor = when (this) {
    SgfColor.Black -> SgfColor.White
    SgfColor.White -> SgfColor.Black
}

enum class SgfDouble {
    Normal,
    Emphasized
}

sealed class GameResult {
    object Draw : GameResult()
    object Unknown : GameResult()
    object Suspended : GameResult()
    data class Time(val winner: SgfColor) : GameResult()
    data class Forfeit(val winner: SgfColor) : GameResult()
    data class Resignation(val winner: SgfColor) : GameResult()
    data class Score(val winner: SgfColor, val score: Double) : GameResult()
    data class Wins(val winner: SgfColor) : GameResult()
}

enum class GameType(internal val value: Int) {
    Go(1),
    Othello(2),
    Chess(3),
    GomokuRenju(4),
    NineMensMorris(5),
    Backgammon(6),
    ChineseChess(7),
    Shogi(8),
    LinesOfAction(9),
    Ataxx(10),
    Hex(11),
    Jungle(12),
    Neutron(13),
    PhilosophersFootball(14),
    Quadrature(15),
    Trax(16),
    Tantrix(17),
    Amazons(18),
    Octi(19),
    Gess(20),
    Twixt(21),
    Zertz(22),
    Plateau(23),
    Yinsh(24),
    Punct(25),
    Gobblet(26),
    Hive(27),
    Exxit(28),
    Hnefatal(29),
    Kuba(30),
    Tripples(31),
    Chase(32),
    TumblingDown(33),
    Sahara(34),
    Byte(35),
    Focus(36),
    Dvonn(37),
    Tamsk(38),
    Gipf(39),
    Kropki(40);
}

sealed class GameDate {
    data class YearAndMonth(val year: Int, val month: Int) : GameDate() {
        init {
            ChronoField.YEAR.checkValidValue(year.toLong())
            ChronoField.MONTH_OF_YEAR.checkValidValue(month.toLong())
        }
    }

    data class Year(val year: Int) : GameDate() {
        init {
            ChronoField.YEAR.checkValidValue(year.toLong())
        }
    }

    data class Date(val year: Int, val month: Int, val day: Int) : GameDate() {
        init {
            ChronoField.YEAR.checkValidValue(year.toLong())
            ChronoField.MONTH_OF_YEAR.checkValidValue(month.toLong())

            val calendar = GregorianCalendar(year, month, 1)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            require(day in 1..daysInMonth) {
                "The day in month for the year $year and month $month must be within the range 1-$daysInMonth"
            }
        }

        constructor(date: LocalDate) : this(date.year, date.monthValue, date.dayOfMonth)
    }

    companion object {
        fun of(year: Int) = Year(year)
        fun of(year: Int, month: Int) = YearAndMonth(year, month)
        fun of(year: Int, month: Int, day: Int) = Date(year, month, day)
    }
}
