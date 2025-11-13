# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build
- **Build the project**: `./gradlew build`
- **Build without running tests**: `./gradlew build -x test`
- **Clean build**: `./gradlew clean build`

### Test
- **Run all tests**: `./gradlew test`
- **Run specific test**: `./gradlew test --tests <TestClassName>`
  - Example: `./gradlew test --tests SymbolicArchiveTest`

### Documentation
- **Generate Javadoc**: `./gradlew javadoc`

### Project Structure
```
symbolic4j/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── symbolic4j/
│   │   │       ├── BufferUtils.java
│   │   │       ├── LookupResult.java
│   │   │       ├── ProguardMappingCache.java
│   │   │       ├── RemapResult.java
│   │   │       ├── SymCache.java
│   │   │       └── type/
│   │   └── kotlin/
│   │       └── io/nullptr/symbolic/
│   │           ├── common/          # Common utilities (strings, UUID)
│   │           ├── object/          # Symbolic object handling
│   │           ├── proguard/        # ProGuard mapping support
│   │           └── SymbolicLibrary.kt  # Main native library interface
│   └── test/
│       └── kotlin/
│           └── io/nullptr/symbolic/
│               ├── object/          # Object tests
│               ├── proguard/        # ProGuard tests
│               └── ...
├── external/        # External libraries and resources
├── build.gradle.kts
├── gradlew
└── README.md
```

## High-Level Architecture

### Core Components

1. **SymbolicLibrary.kt**: The main JNA interface to the underlying native symbolic library. It defines all the native functions that can be called.

2. **SymbolicArchive.kt**: Represents an archive of symbolic information (e.g., a dSYM file). It can contain multiple symbolic objects.

3. **SymbolicObject.kt**: Represents a single symbolic object within an archive. It contains metadata about the object (architecture, code ID, debug ID, file format, etc.) and can build a symbol cache.

4. **SymbolicSymCache.kt**: Represents a cache of symbolic information. It provides fast lookup functionality for addresses to source locations.

5. **SymbolicProguardMapper.kt**: Handles ProGuard mapping files for deobfuscating Android stack traces.

### Key Workflow

```
1. Open a symbolic archive using SymbolicArchive.open(path)
2. Get a SymbolicObject from the archive using getObject(index)
3. Build a SymbolicSymCache from the object using buildSymCache()
4. Use the symcache to lookup addresses using lookup(offset)
5. Free resources when done
```

### Native Integration

The library uses JNA to interact with an underlying native library (likely written in Rust based on the comments about panic hooks). The native library handles the actual symbolic parsing and lookup functionality.

### Dependencies
- JNA (Java Native Access) for native library interaction
- Zstd for compression support
- Guava for utility functions

## Example Usage

```kotlin
val filePath = "path/to/symbolic/file"
val symArchive = SymbolicArchive.open(filePath)

try {
    val symObject = symArchive?.getObject(0)

    try {
        val symCache = symObject?.buildSymCache()

        try {
            val offset = 0x12345678
            val locations = symCache?.lookup(offset)
            locations?.forEach { location ->
                println("Found symbol: ${location.functionName} at ${location.fileName}:${location.line}")
            }
        } finally {
            symCache?.free()
        }
    } finally {
        symObject?.free()
    }
} finally {
    symArchive?.free()
}
```
