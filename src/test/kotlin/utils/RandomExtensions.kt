package utils

import java.util.Random

val rng = Random(42)

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
