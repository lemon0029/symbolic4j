# Symbolic4J

```kotlin
@Test
fun `get object`() {
    val libName = when {
        Platform.isWindows() -> "symbolic_cabi.dll"
        Platform.isLinux() -> "libsymbolic_cabi.so"
        Platform.isMac() -> "libsymbolic_cabi.dylib"
        else -> throw IllegalStateException("Unsupported platform")
    }

    val libSearchPath = "external/libs"

    NativeLibrary.addSearchPath(libName, libSearchPath)
    Native.register(SymbolicLibrary::class.java, libName)
    
    val filePath = "external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron"
    assertTrue(Files.exists(Path.of(filePath)))

    val symArchive = SymbolicArchive.open(filePath)
    assertNotNull(symArchive)
    assertEquals(1, symArchive!!.objectCount)

    assertDoesNotThrow {
        val symObject = symArchive.getObject(0)
        assertNotNull(symObject)
        assertEquals("x86_64", symObject!!.arch)
        assertEquals("cb63147ac9dc308b8ca1ee92a5042e8e", symObject.codeId)
        assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", symObject.debugId)
        assertEquals("macho", symObject.fileFormat)
        assertEquals("dbg", symObject.kind)

        val singleton = symArchive.getObject(0)
        assertSame(symObject, singleton)
    }
}
```