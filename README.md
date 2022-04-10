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
import com.github.ekenstein.sgf.Sgf
import com.github.ekenstein.sgf.SgfCollection

fun main() {
    val sgf = Sgf { /* configure */ }
    val collection: SgfCollection = sgf.decode(Path.of("game.sgf"))
    val raw: String = sgf.encodeToString(collection)
}
```
