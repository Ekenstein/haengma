# haengma
SGF parser and serializer written in Kotlin.

# Usage
### 1. Add a dependency on haengma
For `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.Ekenstein:haengma:1.0.0")
}
```

### 2. Write some code
```kotlin
import java.nio.file.Path
import java.io.InputStream
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.encodeToString
import com.github.ekenstein.sgf.extensions.addProperty

fun main() {
    // you can retrieve a collection by decoding a file
    val collection: SgfCollection = SgfCollection.from(Path.of("game.sgf"))  {
        preserveUnknownProperties = true // If you wish to ignore or preserve unknown properties
    }
    
    // ... or a string
    val collection: SgfCollection = SgfCollection.from("(;AB[ac:ic];B[jj])") {
        preserveUnknownProperties = true // If you wish to ignore or preserve unknown properties
    }
    
    // ... or an input stream
    val inputStream: InputStream = Path.of("game.sgf").toFile().inputStream()
    val collection: SgfCollection = SgfCollection.from(inputStream) {
        preserveUnknownProperties = true // If you wish to ignore or preserve unknown properties
    }
    
    // ... or you can create your own game tree
    val tree: SgfGameTree = SgfGameTree.newGame(size = 19, komi = 6.5, handicap = 0)
        .addProperty(SgfProperty.Root.GM(GameType.Go))
        .addProperty(SgfProperty.GameInfo.DT(GameDate.of(2022, 4, 27), GameDate.of(2022, 4, 28)))
        .addProperty(SgfProperty.Move.B(4, 4))
        .addProperty(SgfProperty.Move.W(16, 4))
    
    // ... which can be encoded to SGF
    val string: String = tree.encodeToString(tree)
    
    // ... which prints to (;GM[1]SZ[19]DT[2022-04-27,28]KM[6.5];B[dd];W[pd])
    println(string)
    
    // ... or by writing directly to an output stream
    val file = Path.of("game.sgf").toFile()
    file.createNewFile()
    file.outputStream().use { 
        tree.encode(it)
    }
}
```

### 2.1 Navigating a tree
```kotlin
import com.github.ekenstein.sgf.extensions.newGame
import com.github.ekenstein.sgf.serialization.encodeToString
import com.github.ekenstein.sgf.viewer.SgfEditor
import com.github.ekenstein.sgf.viewer.commit
import com.github.ekenstein.sgf.viewer.goToPreviousNodeOrStay
import com.github.ekenstein.sgf.viewer.goToRootNode
import com.github.ekenstein.sgf.viewer.pass
import com.github.ekenstein.sgf.viewer.placeStone

fun main() {
    val tree = SgfGameTree.newGame(boardSize = 19, komi = 6.5, handicap = 0)
    
    // build your SGF tree and navigate through it
    val editor = SgfEditor(tree)
        .placeStone(SgfColor.Black, 4, 4)
        .placeStone(SgfColor.White, 16, 4)
        .goToPreviousNodeOrStay()
        .placeStone(SgfColor.White, 16, 16)
        .placeStone(SgfColor.Black, 16, 4)
        .goToRootNode()
        .placeStone(SgfColor.Black, 3, 3)
        .placeStone(SgfColor.White, 16, 4)
        .placeStone(SgfColor.Black, 16, 16)
        .placeStone(SgfColor.White, 4, 16)

    // ... then commit your changes 
    val newTree = editor.commit()
    
    // ... and the resulting SGF would look like (;SZ[19]KM[6.5]FF[4]GM[1](;B[cc];W[pd];B[pp];W[dp])(;B[dd](;W[pp];B[pd])(;W[pd])))
    println(newTree.encodeToString())
    
    // ... you can also look at the current position by extracting the board
    val board = editor.board
    
    // ... which you can also print to a string
    println(board.print())
            
    // ... which in the current position of the editor would look
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  #  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  O  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  O  .  .  .  .  .  .  .  .  .  .  .  #  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
    //  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .
}
```