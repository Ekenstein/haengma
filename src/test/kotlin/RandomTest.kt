import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.GameResult
import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.editor.GameInfo
import com.github.ekenstein.sgf.editor.Player
import com.github.ekenstein.sgf.editor.Rules
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.getGameInfo
import com.github.ekenstein.sgf.editor.goToLastNode
import com.github.ekenstein.sgf.editor.goToLeftMostChildTree
import com.github.ekenstein.sgf.editor.goToNextNode
import com.github.ekenstein.sgf.editor.goToNextTree
import com.github.ekenstein.sgf.editor.goToParentTree
import com.github.ekenstein.sgf.editor.goToPreviousNode
import com.github.ekenstein.sgf.editor.goToPreviousTree
import com.github.ekenstein.sgf.editor.goToRootNode
import com.github.ekenstein.sgf.editor.nextToPlay
import com.github.ekenstein.sgf.editor.pass
import com.github.ekenstein.sgf.editor.placeStone
import com.github.ekenstein.sgf.editor.stay
import com.github.ekenstein.sgf.toPropertySet
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.orStay
import com.github.ekenstein.sgf.utils.toNelUnsafe
import kotlin.random.Random

typealias Gen<T> = Random.() -> T

interface RandomTest {
    companion object {
        private val random = Random(42)

        fun <L, R> Random.pair(first: Gen<L>, second: Gen<R>): Pair<L, R> = first() to second()

        fun <T> Random.valueOrNull(weight: Double = 0.5, gen: Gen<T>): T? {
            val value = nextDouble(0.0, 1.0)
            return if (value <= weight) {
                null
            } else {
                gen()
            }
        }

        fun Random.string(range: IntRange = 0..10, charRange: CharRange = ' '..'z'): String {
            val charSequence = charRange.toList()
            return list(range) {
                item(charSequence)
            }.joinToString("")
        }

        fun <T> Random.item(list: List<T>): T {
            require(list.isNotEmpty()) {
                "The list must not be empty."
            }

            val index = nextInt(list.size)
            return list[index]
        }

        fun <T> Random.list(range: IntRange = 1..10, gen: Gen<T>): List<T> {
            val size = nextInt(range.first, range.last + 1)
            return list(size, gen)
        }

        fun <T> Random.list(numValues: Int, gen: Gen<T>) = (1..numValues).map { gen() }

        inline fun <reified T : Enum<T>> Random.enum() = item(enumValues<T>().toList())
    }

    val random: Random
        get() = RandomTest.random

    fun Random.point(size: Int = 19) = SgfPoint(
        nextInt(1, size + 1),
        nextInt(1, size + 1)
    )

    val Random.move
        get() = item(
            listOf(
                { Move.Pass },
                { Move.Stone(point()) }
            )
        ).invoke()

    val Random.gameResult
        get() = item(
            listOf(
                { GameResult.Unknown },
                { GameResult.Wins(enum()) },
                { GameResult.Score(enum(), nextDouble()) },
                { GameResult.Time(enum()) },
                { GameResult.Draw },
                { GameResult.Resignation(enum()) },
                { GameResult.Forfeit(enum()) },
                { GameResult.Suspended }
            )
        ).invoke()

    val Random.gameDate
        get() = item(
            listOf(
                GameDate.of(2019),
                GameDate.of(2019, 3),
                GameDate.of(2019, 2, 28)
            )
        )

    val Random.moveProperty
        get() = item(
            listOf(
                { SgfProperty.Move.B(move) },
                { SgfProperty.Move.W(move) },
                { SgfProperty.Move.KO },
                { SgfProperty.Move.MN(nextInt()) }
            )
        ).invoke()

    val Random.setupProperty
        get() = item(
            listOf(
                { SgfProperty.Setup.PL(enum()) },
                { SgfProperty.Setup.AB(list(1..10) { point() }.toSet()) },
                { SgfProperty.Setup.AW(list(1..10) { point() }.toSet()) },
                { SgfProperty.Setup.AE(list(1..10) { point() }.toSet()) }
            )
        ).invoke()

    val Random.nodeAnnotationProperty
        get() = item(
            listOf(
                { SgfProperty.NodeAnnotation.C(string(0..100)) },
                { SgfProperty.NodeAnnotation.DM(enum()) },
                { SgfProperty.NodeAnnotation.GB(enum()) },
                { SgfProperty.NodeAnnotation.HO(enum()) },
                { SgfProperty.NodeAnnotation.N(string(0..10)) },
                { SgfProperty.NodeAnnotation.UC(enum()) },
                { SgfProperty.NodeAnnotation.V(nextDouble()) }
            )
        ).invoke()

    val Random.moveAnnotationProperty
        get() = item(
            listOf(
                { SgfProperty.MoveAnnotation.BM(enum()) },
                { SgfProperty.MoveAnnotation.DO },
                { SgfProperty.MoveAnnotation.IT },
                { SgfProperty.MoveAnnotation.TE(enum()) },
            )
        ).invoke()

    val Random.markupProperty
        get() = item(
            listOf(
                {
                    val points = list(1..10) {
                        pair({ point() }, { point() })
                    }
                    SgfProperty.Markup.AR(points)
                },
                {
                    val labels = list(1..10) {
                        pair({ point() }, { string(1..100) })
                    }
                    SgfProperty.Markup.LB(labels)
                },
                { SgfProperty.Markup.CR(list(1..10) { point() }.toSet()) },
                { SgfProperty.Markup.MA(list(1..10) { point() }.toSet()) },
                {
                    val lines = list(1..10) {
                        pair({ point() }, { point() })
                    }

                    SgfProperty.Markup.LN(lines)
                },
                { SgfProperty.Markup.SL(list(1..10) { point() }.toSet()) },
                { SgfProperty.Markup.SQ(list(1..10) { point() }.toSet()) },
                { SgfProperty.Markup.TR(list(1..10) { point() }.toSet()) },
                { SgfProperty.Markup.DD(list(1..10) { point() }.toSet()) }
            )
        ).invoke()

    val Random.rootProperty
        get() = item(
            listOf(
                { SgfProperty.Root.AP(string(0..10), string(0..10)) },
                { SgfProperty.Root.FF(nextInt(1, 4)) },
                { SgfProperty.Root.SZ(nextInt(1, 20)) },
                { SgfProperty.Root.CA(item(listOf(Charsets.ISO_8859_1, Charsets.UTF_8))) },
                { SgfProperty.Root.GM(enum()) },
                { SgfProperty.Root.ST(nextInt(0, 3)) },
            )
        ).invoke()

    val Random.gameInfoProperty
        get() = item(
            listOf(
                {
                    val handicap = (2..9).toList()
                    SgfProperty.GameInfo.HA(item(handicap))
                },
                { SgfProperty.GameInfo.KM(nextDouble()) },
                { SgfProperty.GameInfo.EV(string(0..10)) },
                { SgfProperty.GameInfo.PB(string(0..10)) },
                { SgfProperty.GameInfo.PW(string(0..10)) },
                { SgfProperty.GameInfo.RE(gameResult) },
                { SgfProperty.GameInfo.BR(string(0..10)) },
                { SgfProperty.GameInfo.WR(string(0..10)) },
                { SgfProperty.GameInfo.GN(string(0..10)) },
                { SgfProperty.GameInfo.DT(list(1..3) { gameDate }) },
                { SgfProperty.GameInfo.TM(nextDouble()) },
                { SgfProperty.GameInfo.SO(string(0..10)) },
                { SgfProperty.GameInfo.GC(string(0..10)) },
                { SgfProperty.GameInfo.ON(string(0..10)) },
                { SgfProperty.GameInfo.OT(string(0..10)) },
                { SgfProperty.GameInfo.RO(string(0..10)) },
                { SgfProperty.GameInfo.RU(string(0..10)) },
                { SgfProperty.GameInfo.US(string(0..10)) },
                { SgfProperty.GameInfo.WT(string(0..10)) },
                { SgfProperty.GameInfo.BT(string(0..10)) },
                { SgfProperty.GameInfo.AN(string(0..10)) },
                { SgfProperty.GameInfo.CP(string(0..10)) },
                { SgfProperty.GameInfo.PC(string(0..10)) }
            )
        ).invoke()

    val Random.timingProperty
        get() = item(
            listOf(
                { SgfProperty.Timing.BL(nextDouble()) },
                { SgfProperty.Timing.WL(nextDouble()) },
                { SgfProperty.Timing.OB(nextInt()) },
                { SgfProperty.Timing.OW(nextInt()) }
            )
        ).invoke()

    val Random.miscProperty
        get() = item(
            listOf(
                {
                    val value = valueOrNull { pair({ nextInt() }, { string() }) }
                    SgfProperty.Misc.FG(value)
                },
                { SgfProperty.Misc.PM(nextInt()) },
                { SgfProperty.Misc.VW(list(1..10) { point() }.toSet()) }
            )
        ).invoke()

    val Random.privateProperty
        get() = SgfProperty.Private(
            identifier = string(1..3, 'A'..'Z'),
            values = list(0..3) { string() }
        )

    val Random.anyProperty
        get() = item(
            listOf(
                { moveProperty },
                { setupProperty },
                { nodeAnnotationProperty },
                { moveAnnotationProperty },
                { markupProperty },
                { rootProperty },
                { gameInfoProperty },
                { timingProperty },
                { miscProperty },
//                { privateProperty }
            )
        ).invoke()

    val Random.node: SgfNode
        get() {
            val properties = list {
                anyProperty
            }.toPropertySet()

            return SgfNode(properties)
        }

    fun Random.gameTree(size: Int = 3): SgfGameTree {
        val sequence = list { node }.toNelUnsafe()
        val tree = SgfGameTree(
            sequence = sequence,
            trees = if (size > 0) {
                list(1..size) { gameTree(size - 1) }
            } else {
                emptyList()
            }
        )

        return tree
    }

    val Random.gameInfo: GameInfo
        get() = item(
            listOf(
                {
                    GameInfo(
                        rules = Rules(
                            boardSize = item(listOf(9, 13, 19)),
                            komi = item(listOf(0.5, 6.5, 7.5)),
                            handicap = item((2..9) + 0)
                        ),
                        blackPlayer = Player(
                            name = valueOrNull { string() },
                            rank = valueOrNull { string() },
                            team = valueOrNull { string() }
                        ),
                        whitePlayer = Player(
                            name = valueOrNull { string() },
                            rank = valueOrNull { string() },
                            team = valueOrNull { string() }
                        ),
                        result = valueOrNull { gameResult },
                        gameComment = valueOrNull { string() },
                        gameDate = list(0..3) { gameDate },
                        gameName = valueOrNull { string() },
                        gamePlace = valueOrNull { string() }
                    )
                },
                { GameInfo.default }
            )
        ).invoke()

    private val Random.movement: (SgfEditor) -> MoveResult<SgfEditor>
        get() = item(
            listOf(
                { it.goToRootNode().stay() },
                { it.goToLastNode().stay() },
                { it.goToParentTree() },
                { it.goToPreviousNode() },
                { it.goToNextNode() },
                { it.goToLeftMostChildTree() },
                { it.goToNextTree() },
                { it.goToPreviousTree() }
            )
        )

    private fun Random.execution(size: Int): (SgfEditor) -> SgfEditor = item(
        listOf(
            { it.placeStone(it.nextToPlay(), point(size), true) },
            { it.pass(it.nextToPlay()) }
        )
    )

    fun Random.navigate(editor: SgfEditor) = movement(editor).orStay()
    fun Random.executeMove(editor: SgfEditor, size: Int) = execution(size)(editor)

    fun Random.performOperations(editor: SgfEditor, numberOfOperations: IntRange = 1..10): SgfEditor {
        val boardSize = editor.getGameInfo().rules.boardSize
        val operations: List<Random.(SgfEditor) -> SgfEditor> = listOf(
            { navigate(it) },
            { executeMove(it, boardSize) }
        )

        return list(numberOfOperations) {
            item(operations)
        }.fold(editor) { e, o -> o(e) }
    }
}
