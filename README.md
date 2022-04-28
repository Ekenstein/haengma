# ktsgf
A small, SGF parser library for Kotlin.

# Usage
### 1. Add a dependency on ktsgf
For `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.Ekenstein:ktsgf:0.1.1")
}
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
    val tree: SgfGameTree = SgfGameTree.empty
        .addProperty(SgfProperty.Root.GM(GameType.Go))
        .addProperty(SgfProperty.Root.SZ(19))
        .addProperty(SgfProperty.GameInfo.DT(GameDate.of(2022, 4, 27), GameDate.of(2022, 4, 28)))
        .addProperty(SgfProperty.GameInfo.KM(6.5))
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
