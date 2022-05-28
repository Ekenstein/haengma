# Haengma
[![Docs](https://img.shields.io/badge/docs-latest-informational)](https://ekenstein.github.io/haengma/)
[![Release](https://jitpack.io/v/Ekenstein/haengma.svg)](https://jitpack.io/#Ekenstein/haengma)
[![License](https://img.shields.io/github/license/ekenstein/haengma)](https://github.com/ekenstein/haengma/blob/main/LICENSE)
![Build Status](https://github.com/ekenstein/haengma/workflows/CI/badge.svg)

A small, stand-alone, easy to use SGF parser library for Kotlin.

Haengma supports type-safe deserialization of SGF files and serialization of SGF trees. 
It also comes with an easy-to-use SGF editor that supports navigation and editing of an SGF tree.  

[Haengma](https://senseis.xmp.net/?Haengma) is a Korean word which means roughly the way the stones move, or forward 
momentum (literally it means moving horse). 

The term is used to 
1. describe various basic combinations of stones and their implications 
2. discuss more intricate moves that have a sense of tesuji 
3. describe a player's style.

# Usage
### Add a dependency on haengma
For `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.Ekenstein:haengma:2.1.0")
}
```

### Deserialization
```kotlin
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.parser.from
import java.io.InputStream
import java.nio.file.Path

fun main() {
    // you can retrieve a sgf collection by deserializing a file
    val collection = SgfCollection.from(Path.of("game.sgf"))

    // ... or by deserializing a string
    val collection = SgfCollection.from("(;AB[ac:ic];B[jj])")

    // ... or by deserializing an input stream
    val inputStream: InputStream = ...
    val collection = SgfCollection.from(inputStream)
    
    // ... with the deserialized sgf collection you can filter trees by their respective game information
    val trees = collection.filter {
        it.gameName == "The ear-reddening game"
    }
}
```

### Serialization
```kotlin
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.serialization.encode
import com.github.ekenstein.sgf.serialization.encodeToString
import com.github.ekenstein.sgf.utils.nelOf
import java.nio.file.Path

fun main() {
    val tree = SgfGameTree(
        sequence = nelOf(
            SgfNode(SgfProperty.Move.B(1, 1))
        ),
        trees = emptyList()
    )

    // you can serialize a tree to an output stream
    val file = Path.of("game.sgf").toFile()
    file.createNewFile()
    file.outputStream().use { tree.encode(it) }

    // ... or if you have multiple trees you wish to serialize to an output stream
    val anotherTree = SgfGameTree(nelOf(SgfNode(SgfProperty.Move.B(2, 2))))
    file.outputStream().use { SgfCollection(nelOf(tree, anotherTree)).encode(it) }

    // ... or if you just wish to serialize the tree to a string
    val sgf = tree.encodeToString()

    // ... which prints to (;B[aa])
    println(sgf)
}
```

### Creating your own tree
```kotlin
import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.commit
import com.github.ekenstein.sgf.editor.goToPreviousNodeOrStay
import com.github.ekenstein.sgf.editor.goToRootNode
import com.github.ekenstein.sgf.editor.placeStone
import com.github.ekenstein.sgf.serialization.encodeToString

fun main() {
    // you can create your own tree
    val editor = SgfEditor {
        rules {
            boardSize = 19
            komi = 0.5
            handicap = 2
        }
        gameDate = listOf(
            GameDate.of(2022, 5, 21),
            GameDate.of(2022, 5, 22)
        )
        gameComment = "This is an example"
    }

    // ... and then start placing stones and navigating your tree
    val updatedEditor = editor
        .placeStone(SgfColor.White, 17, 3)
        .placeStone(SgfColor.Black, 16, 3)
        .placeStone(SgfColor.White, 17, 4)
        .placeStone(SgfColor.Black, 17, 5)
        .goToPreviousNodeOrStay()
        .placeStone(SgfColor.Black, 16, 5)
        .goToPreviousNodeOrStay()
        .placeStone(SgfColor.Black, 4, 4)
        .goToRootNode()
        .placeStone(SgfColor.White, 16, 16)
        .updateGameInfo {
            result = GameResult.Resignation(SgfColor.White)
            gameName = "The ear-reddening game"
        }

    // ... when you feel that you are done you can commit your editor to a tree
    val tree = updatedEditor.commit()

    // ... and the tree will contain all the branches that you've added
    val sgf = tree.encodeToString()

    // ... which prints to (;GM[1]RE[W+R]GN[The ear-reddening game]FF[4]SZ[19]KM[0]HA[2]AB[dp][pd](;W[pp])(;W[qc];B[pc];W[qd](;B[dd])(;B[pe])(;B[qe])))
    println(sgf)
}
```

### Setting up positions
```kotlin
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.addStones
import com.github.ekenstein.sgf.editor.commit
import com.github.ekenstein.sgf.editor.placeStone
import com.github.ekenstein.sgf.editor.setNextToPlay
import com.github.ekenstein.sgf.serialization.encodeToString

fun main() {
    // you can also set up a position by adding stones to the board and telling whose turn it is
    // which can be handy if you wish to set up problems.
    val editor = SgfEditor()
        .addStones(SgfColor.Black, SgfPoint(4, 4), SgfPoint(5, 5), SgfPoint(6, 6))
        .addStones(SgfColor.White, SgfPoint(16, 16))
        .setNextToPlay(SgfColor.White)
        .placeStone(SgfColor.White, 10, 10)

    // ... when you're done you can commit the changes
    val tree = editor.commit()

    // ... and the resulting sgf will be (;GM[1]FF[4]SZ[19]KM[0]AB[dd][ee][ff]AW[pp]PL[W];W[jj])
    val sgf = tree.encodeToString()
    println(sgf)
}
```

### Displaying a game
```kotlin
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.editor.SgfEditor
import com.github.ekenstein.sgf.editor.extractBoard
import com.github.ekenstein.sgf.editor.goToNextNode
import com.github.ekenstein.sgf.editor.print
import com.github.ekenstein.sgf.editor.repeat
import com.github.ekenstein.sgf.parser.from
import java.nio.file.Path

fun main() {
    val collection = SgfCollection.from(Path.of("Kamakura-Jubango-Game-5.sgf"))
    val tree = collection.trees.head

    // ... Load the game you wish to display. In this case we wish to see the position of
    // the Kamakura Jubango game 5 at the 10th move.
    val editor = SgfEditor(tree).repeat(10) { it.goToNextNode() }

    // ... extract the board from the current position
    val board = editor.extractBoard()

    // ... and print it
    println(board.print())

    // ... which prints to
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  O  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  #  .  #  .  .
    //    .  .  O  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  #  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  #  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  #  .  .  .  .  .  .  .  .  .  .  .  O  .  O  .  .
    //    .  .  .  .  O  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //    .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
}
```
