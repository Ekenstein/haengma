# ktsgf
A small, SGF parser library for Kotlin.

# Usage
### 1. Obtain an SGF file
```
(;FF[4]GM[1]SZ[19];B[aa];W[bb];B[cc];W[dd];B[ad];W[bd])
```

### 2. Write some code
```kotlin
import java.nio.file.Path
import java.io.InputStream
import com.github.ekenstein.sgf.Sgf
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.encodeToString
import com.github.ekenstein.sgf.extensions.addProperty

fun main() {
    val sgf = Sgf { 
        isLenient = true /* if you wish to ignore malformed property values */
    }
    // you can retrieve a collection by decoding a file
    val collection: SgfCollection = sgf.decode(Path.of("game.sgf"))
    
    // ... or a string
    val collection: SgfCollection = sgf.decode("(;B[aa])")
    
    // ... or an input stream
    val inputStream: InputStream = Path.of("game.sgf").toFile().inputStream()
    val collection: SgfCollection = sgf.decode(inputStream)
    
    // or you can create your own game tree ...
    val tree: SgfGameTree = SgfGameTree.empty
        .addProperty(SgfProperty.Root.GM(GameType.Go))
        .addProperty(SgfProperty.Root.SZ(19))
        .addProperty(SgfProperty.GameInfo.KM(6.5))
        .addProperty(SgfProperty.Move.B(4, 4))
        .addProperty(SgfProperty.Move.W(16, 4))
    
    // which can be encoded to SGF ...
    val string: String = sgf.encodeToString(tree)
    
    // which prints to (;GM[1]SZ[19]KM[6.5];B[dd];W[pd])
    println(string)
}
```
