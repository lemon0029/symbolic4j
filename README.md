# Symbolic4J

Symbolic4J is a Kotlin/Java library that provides access to symbolic debugging information. It wraps a native library (likely written in Rust) through JNA, enabling you to work with symbol archives like dSYM files and ProGuard mappings.

## Core Features

- **Symbolic Archive Handling**: Open and inspect symbol archives (e.g., dSYM files on macOS)
- **Symbol Cache**: Build and query fast symbol caches for efficient lookup
- **ProGuard Support**: Load and use ProGuard mapping files for deobfuscation
- **Cross-Platform**: Works on Windows, Linux, and macOS

## Quick Start

### Symbol Lookup

```kotlin
import io.nullptr.symbolic.`object`.SymbolicArchive
import io.nullptr.symbolic.`object`.SymbolicObject
import io.nullptr.symbolic.`object`.SymbolicSymCache

fun main() {
    // Path to a dSYM file or other symbol archive
    val archivePath = "path/to/symbol/archive"

    // Open the symbolic archive
    val archive: SymbolicArchive? = SymbolicArchive.open(archivePath)

    try {
        // Get the first object in the archive
        val obj: SymbolicObject? = archive?.getObject(0)

        try {
            // Build a symbol cache for fast lookups
            val cache: SymbolicSymCache? = obj?.buildSymCache()

            try {
                // Look up an address offset
                val offset = 0x12345678L
                val locations = cache?.lookup(offset.toInt())

                // Print results
                locations?.forEach { loc ->
                    println("Found symbol")
                    println("  Symbol address: 0x${loc.symAddr.toString(16)}")
                    println("  Instruction address: 0x${loc.instrAddr.toString(16)}")
                    println("  Line number: ${loc.lineNumber}")
                    println("  Symbol: ${loc.symbol?.decodeToString()}")
                    println("  File path: ${loc.path?.decodeToString()}")
                }
            } finally {
                cache?.free()
            }
        } finally {
            obj?.free()
        }
    } finally {
        archive?.free()
    }
}
```

### ProGuard Mapping

```kotlin
import io.nullptr.symbolic.proguard.SymbolicProguardMapper

fun main() {
    // Path to ProGuard mapping file
    val mappingPath = "path/to/proguard/mapping.txt"

    // Open the ProGuard mapper
    val mapper: SymbolicProguardMapper? = SymbolicProguardMapper.open(mappingPath)

    try {
        println("Mapper UUID: ${mapper?.uuid}")
        println("Has line info: ${mapper?.hasLineInfo}")

        // Additional ProGuard operations will be available here
    } finally {
        mapper?.free()
    }
}
```

## API Overview

### `SymbolicArchive`

Represents a symbol archive (e.g., dSYM file) containing one or more symbol objects.

- `open(path: String)`: Open an archive
- `getObject(index: Int)`: Get a specific symbol object from the archive
- `objectCount`: Number of objects in the archive
- `free()`: Release resources

### `SymbolicObject`

Represents a single symbol object within an archive.

- `arch`: Architecture (e.g., "x86_64")
- `codeId`: Code identifier
- `debugId`: Debug identifier (UUID format)
- `fileFormat`: File format (e.g., "macho")
- `kind`: Object kind (e.g., "dbg")
- `buildSymCache()`: Create a symbol cache from this object
- `free()`: Release resources

### `SymbolicSymCache`

Represents a fast symbol cache for efficient address lookups.

- `lookup(offset: Int)`: Look up symbol information for an address offset
- `lookup(instrAddr: Long, vmAddr: Long)`: Look up symbol information with virtual memory address
- `arch`: Architecture of the cache
- `debugId`: Debug identifier
- `version`: Cache version
- `size`: Size of the cache
- `free()`: Release resources

### `SymbolicProguardMapper`

Handles ProGuard mapping files for deobfuscation.

- `open(path: String)`: Open a ProGuard mapping file
- `uuid`: Unique identifier of the mapping
- `hasLineInfo`: Whether the mapping contains line information
- `free()`: Release resources

## Build

To build Symbolic4J:

```bash
./gradlew build
```

To build without running tests:

```bash
./gradlew build -x test
```

## Note

Symbolic4J relies on a native library that must be present in the system. The library files (DLL, SO, or DYLIB) are typically expected to be in the `external/libs` directory.
