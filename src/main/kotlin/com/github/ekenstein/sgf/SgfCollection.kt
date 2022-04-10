package com.github.ekenstein.sgf

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

data class SgfCollection(val trees: List<SgfGameTree>) {
    companion object
}

data class SgfGameTree(val sequence: List<SgfNode>, val trees: List<SgfGameTree>)
data class SgfNode(val properties: Set<SgfProperty>)

sealed class SgfProperty {
    sealed class Move : SgfProperty() {
        data class B(val move: com.github.ekenstein.sgf.Move) : Move()
        data class W(val move: com.github.ekenstein.sgf.Move) : Move()
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
        data class CR(val points: List<SgfPoint>) : Markup()
        data class LB(val label: List<Pair<SgfPoint, String>>) : Markup()
        data class LN(val line: List<Pair<SgfPoint, SgfPoint>>) : Markup()
        data class MA(val points: List<SgfPoint>) : Markup()
        data class SL(val selected: List<SgfPoint>) : Markup()
        data class SQ(val points: List<SgfPoint>) : Markup()
        data class TR(val points: List<SgfPoint>) : Markup()
        data class DD(val points: List<SgfPoint>) : Markup()
    }

    sealed class Root : SgfProperty() {
        data class AP(val name: String, val version: String) : Root()
        data class FF(val format: Int) : Root()
        data class SZ(val width: Int, val height: Int) : Root() {
            constructor(size: Int) : this(size, size)
        }
        data class CA(val charset: String) : Root()
        data class GM(val game: GameType) : Root()
        data class ST(val style: Int) : Root()
    }

    sealed class GameInfo : SgfProperty() {
        data class HA(val numberOfStones: Int) : GameInfo()
        data class KM(val komi: Double) : GameInfo()
        data class EV(val event: String) : GameInfo()
        data class PB(val name: String) : GameInfo()
        data class PW(val name: String) : GameInfo()
        data class RE(val result: String) : GameInfo() {
            companion object {
                fun draw() = withLabel { "0" }

                fun resignation(winner: SgfColor) = withLabel {
                    when (winner) {
                        SgfColor.Black -> "B+R"
                        SgfColor.White -> "W+R"
                    }
                }

                fun score(winner: SgfColor, score: Double) = withLabel {
                    when (winner) {
                        SgfColor.Black -> "B+$score"
                        SgfColor.White -> "W+$score"
                    }
                }

                fun time(winner: SgfColor) = withLabel {
                    when (winner) {
                        SgfColor.Black -> "B+T"
                        SgfColor.White -> "W+T"
                    }
                }

                fun forfeit(winner: SgfColor) = withLabel {
                    when (winner) {
                        SgfColor.Black -> "B+F"
                        SgfColor.White -> "W+F"
                    }
                }

                private fun withLabel(label: () -> String) = RE(label())
            }
        }
        data class BR(val rank: String) : GameInfo()
        data class WR(val rank: String) : GameInfo()
        data class GN(val name: String) : GameInfo()
        data class DT(val date: String) : GameInfo() {
            companion object {
                fun from(vararg date: OffsetDateTime): DT {
                    require(date.isNotEmpty()) {
                        "There must be at least one date."
                    }

                    val formatted = date.joinToString(",") { dateTimeFormatter.format(it) }
                    return DT(formatted)
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
        data class VW(val points: List<SgfPoint>) : Misc()
    }

    data class Private(val identifier: String, val values: List<String>) : SgfProperty()
}

sealed class Move {
    data class Stone(val point: SgfPoint) : Move() {
        constructor(x: Int, y: Int) : this(SgfPoint(x, y))
    }
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
