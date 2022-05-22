package utils

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.GameResult
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.editor.GameInfo
import com.github.ekenstein.sgf.editor.Player
import com.github.ekenstein.sgf.editor.Rules
import java.util.Random

val rng = Random(42)

fun Random.split(): Pair<Random, Random> {
    val seed = nextInt().toLong()
    val next = Random(seed)
    return this to next
}

fun <T> Random.nextItem(list: List<T>): T {
    require(list.isNotEmpty()) {
        "The list must not be empty."
    }

    val index = nextInt(list.size)
    return list[index]
}

fun <T> Random.nextList(from: List<T>): List<T> {
    require(from.isNotEmpty()) {
        "The list must not be empty"
    }

    val numberOfItems = nextInt(100)
    return (1..numberOfItems).fold(emptyList()) { list, _ ->
        list + nextItem(from)
    }
}

const val DIGITS = "0123456789"
const val LC_LETTERS = "abcdefghijklmnopqrstuvwxyz"
const val UC_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

fun Random.nextString(alphabet: String = DIGITS + LC_LETTERS + UC_LETTERS) =
    nextList(alphabet.toList()).joinToString("")

fun <T> Random.orNull(block: Random.() -> T): T? {
    val weight = nextDouble()
    return if (weight < 0.5) {
        null
    } else {
        block()
    }
}

fun Random.nextRules() = Rules(
    boardSize = nextItem(listOf(9, 13, 19)),
    komi = nextItem(listOf(0.0, 0.5, 6.5, 7.5)),
    handicap = nextItem(listOf(0, *(2..9).toList().toTypedArray()))
)

val ranks: List<String>
    get() {
        val kyu = (1..30).map { rank -> "$rank kyu" }
        val dan = (1..9).map { rank -> "$rank dan" }

        return kyu + dan + "?"
    }

fun Random.nextPlayer() = Player(
    name = orNull { nextString() },
    rank = orNull { nextItem(ranks) },
    team = orNull { nextString() }
)

fun Random.nextColor() = nextItem(listOf(SgfColor.Black, SgfColor.White))

fun Random.nextGameResult(): GameResult {
    val results = listOf(
        GameResult.Unknown,
        GameResult.Draw,
        GameResult.Suspended,
        GameResult.Resignation(nextColor()),
        GameResult.Time(nextColor()),
        GameResult.Forfeit(nextColor()),
        GameResult.Wins(nextColor()),
        GameResult.Score(nextColor(), nextItem(listOf(0.5, 3.5, 23.5)))
    )

    return nextItem(results)
}

fun Random.nextGameDate(): GameDate = nextItem(
    listOf(
        GameDate.of(1940),
        GameDate.of(1940, 12, 23),
        GameDate.of(1940, 12)
    )
)

fun Random.nextGameInfo() = GameInfo(
    rules = nextRules(),
    blackPlayer = nextPlayer(),
    whitePlayer = nextPlayer(),
    result = orNull { nextGameResult() },
    gameComment = orNull { nextString() },
    gameName = orNull { nextString() },
    gamePlace = orNull { nextString() },
    gameDate = listOfNotNull(orNull { nextGameDate() })
)

tailrec fun Random.run(n: Int, block: Random.() -> Unit) {
    if (n < 0) {
        return
    }

    block()
    val (_, next) = split()
    next.run(n - 1, block)
}
