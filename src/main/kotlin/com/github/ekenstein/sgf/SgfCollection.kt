package com.github.ekenstein.sgf

import java.nio.charset.Charset
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.GregorianCalendar

data class SgfCollection(val trees: List<SgfGameTree>) {
    companion object
}

data class SgfGameTree(val sequence: List<SgfNode>, val trees: List<SgfGameTree>) {
    companion object {
        val empty = SgfGameTree(emptyList(), emptyList())
    }
}
data class SgfNode(val properties: Set<SgfProperty>) {
    constructor(vararg property: SgfProperty) : this(property.toSet())
}

sealed class SgfProperty {
    sealed class Move : SgfProperty() {
        data class B(val move: com.github.ekenstein.sgf.Move) : Move() {
            constructor(x: Int, y: Int) : this(com.github.ekenstein.sgf.Move.Stone(SgfPoint(x, y)))
            companion object {
                fun pass() = B(com.github.ekenstein.sgf.Move.Pass)
            }
        }
        data class W(val move: com.github.ekenstein.sgf.Move) : Move() {
            constructor(x: Int, y: Int) : this(com.github.ekenstein.sgf.Move.Stone(SgfPoint(x, y)))
            companion object {
                fun pass() = B(com.github.ekenstein.sgf.Move.Pass)
            }
        }
        object KO : Move()
        data class MN(val number: Int) : Move()
    }

    sealed class Setup : SgfProperty() {
        data class AB(val points: Set<SgfPoint>) : Setup()
        data class AW(val points: Set<SgfPoint>) : Setup()
        data class AE(val points: Set<SgfPoint>) : Setup()
        data class PL(val color: SgfColor) : Setup()
    }

    sealed class NodeAnnotation : SgfProperty() {
        data class C(val comment: String) : NodeAnnotation()
        data class DM(val value: SgfDouble) : NodeAnnotation()
        data class GB(val value: SgfDouble) : NodeAnnotation()
        data class GW(val value: SgfDouble) : NodeAnnotation()
        data class HO(val value: SgfDouble) : NodeAnnotation()
        data class N(val name: String) : NodeAnnotation()
        data class UC(val value: SgfDouble) : NodeAnnotation()
        data class V(val value: Double) : NodeAnnotation()
    }

    sealed class MoveAnnotation : SgfProperty() {
        data class BM(val value: SgfDouble) : MoveAnnotation()
        object DO : MoveAnnotation()
        object IT : MoveAnnotation()
        data class TE(val value: SgfDouble) : MoveAnnotation()
    }

    sealed class Markup : SgfProperty() {
        data class AR(val points: List<Pair<SgfPoint, SgfPoint>>) : Markup()
        data class CR(val points: Set<SgfPoint>) : Markup()
        data class LB(val label: List<Pair<SgfPoint, String>>) : Markup()
        data class LN(val line: List<Pair<SgfPoint, SgfPoint>>) : Markup()
        data class MA(val points: Set<SgfPoint>) : Markup()
        data class SL(val selected: Set<SgfPoint>) : Markup()
        data class SQ(val points: Set<SgfPoint>) : Markup()
        data class TR(val points: Set<SgfPoint>) : Markup()
        data class DD(val points: Set<SgfPoint>) : Markup()
    }

    sealed class Root : SgfProperty() {
        data class AP(val name: String, val version: String) : Root()
        data class FF(val format: Int) : Root()
        data class SZ(val width: Int, val height: Int) : Root() {
            constructor(size: Int) : this(size, size)
        }
        data class CA(val charset: Charset) : Root()
        data class GM(val game: GameType) : Root()
        data class ST(val style: Int) : Root()
    }

    sealed class GameInfo : SgfProperty() {
        data class HA(val numberOfStones: Int) : GameInfo()
        data class KM(val komi: Double) : GameInfo()
        data class EV(val event: String) : GameInfo()
        data class PB(val name: String) : GameInfo()
        data class PW(val name: String) : GameInfo()
        data class RE(val result: GameResult) : GameInfo()
        data class BR(val rank: String) : GameInfo()
        data class WR(val rank: String) : GameInfo()
        data class GN(val name: String) : GameInfo()
        data class DT(val dates: List<GameDate>) : GameInfo() {
            constructor(vararg date: GameDate) : this(date.toList())
            init {
                require(dates.isNotEmpty()) {
                    "The game dates must not be empty"
                }
            }
        }
        data class TM(val timeLimit: Double) : GameInfo()
        data class SO(val source: String) : GameInfo()
        data class GC(val comment: String) : GameInfo()
        data class ON(val opening: String) : GameInfo()
        data class OT(val overtime: String) : GameInfo()
        data class RO(val round: String) : GameInfo()
        data class RU(val rules: String) : GameInfo()
        data class US(val user: String) : GameInfo()
        data class WT(val team: String) : GameInfo()
        data class BT(val team: String) : GameInfo()
        data class AN(val annotation: String) : GameInfo()
        data class CP(val copyright: String) : GameInfo()
        data class PC(val place: String) : GameInfo()
    }

    sealed class Timing : SgfProperty() {
        data class BL(val timeLeft: Double) : Timing()
        data class WL(val timeLeft: Double) : Timing()
        data class OB(val overtimeStones: Int) : Timing()
        data class OW(val overtimeStones: Int) : Timing()
    }

    sealed class Misc : SgfProperty() {
        data class FG(val value: Pair<Int, String>?) : Misc() {
            constructor() : this(null)
            constructor(diagramName: String, flag: Int) : this(flag to diagramName)
        }
        data class PM(val printMoveMode: Int) : Misc()
        data class VW(val points: Set<SgfPoint>) : Misc()
    }

    data class Private(val identifier: String, val values: List<String>) : SgfProperty()
}

sealed class Move {
    data class Stone(val point: SgfPoint) : Move()
    object Pass : Move()
}

data class SgfPoint(val x: Int, val y: Int)

enum class SgfColor {
    Black,
    White;
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
