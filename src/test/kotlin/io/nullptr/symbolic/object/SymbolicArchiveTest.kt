package io.nullptr.symbolic.`object`

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import io.nullptr.symbolic.SymbolicLibrary
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SymbolicArchiveTest {

    @BeforeEach
    fun setup() {
        val libName = when {
            Platform.isWindows() -> "symbolic_cabi.dll"
            Platform.isLinux() -> "libsymbolic_cabi.so"
            Platform.isMac() -> "libsymbolic_cabi.dylib"
            else -> throw IllegalStateException("Unsupported platform")
        }

        val libSearchPath = "external/libs"

        NativeLibrary.addSearchPath(libName, libSearchPath)
        Native.register(SymbolicLibrary::class.java, libName)
    }

    @Test
    fun `open archive electron-dsym`() {
        val filePath = "external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron"
        assertTrue(Files.exists(Path.of(filePath)))

        val symArchive = SymbolicArchive.open(filePath)
        assertNotNull(symArchive)
        assertEquals(1, symArchive!!.objectCount)
    }

    @Test
    fun `get object electron-dsym`() {
        val filePath = "external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron"
        assertTrue(Files.exists(Path.of(filePath)))

        val symArchive = SymbolicArchive.open(filePath)
        assertNotNull(symArchive)
        assertEquals(1, symArchive!!.objectCount)

        val symObject = symArchive.getObject(0)
        assertNotNull(symObject)
        assertEquals("x86_64", symObject!!.arch)
        assertEquals("cb63147ac9dc308b8ca1ee92a5042e8e", symObject.codeId)
        assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", symObject.debugId)
        assertEquals("macho", symObject.fileFormat)
        assertEquals("dbg", symObject.kind)

        assertEquals(true, symObject.hasSymtab)
        assertEquals(true, symObject.hasDebug)
        assertEquals(false, symObject.hasUnwind)
        assertEquals(false, symObject.hasSources)

        assertSame(symObject, symArchive.getObject(0))

        symObject.free()
        symArchive.free()
    }

    @Test
    fun `open archive electron-dsym invalid path`() {
        val symArchive = SymbolicArchive.open("invalid/path")
        assertNull(symArchive)
    }

    @Test
    fun `get object electron-dsym invalid index`() {
        val filePath = "external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron"
        assertTrue(Files.exists(Path.of(filePath)))

        val symArchive = SymbolicArchive.open(filePath)
        assertNotNull(symArchive)
        assertEquals(1, symArchive!!.objectCount)

        assertThrows(IllegalArgumentException::class.java) {
            symArchive.getObject(-1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            symArchive.getObject(1)
        }

        symArchive.free()
    }

    @Test
    fun `get object dwsg-exe`() {
        val filePath = "external/resources/dwsg-exe"
        assertTrue(Files.exists(Path.of(filePath)))

        val symArchive = SymbolicArchive.open(filePath)
        assertNotNull(symArchive)
        assertEquals(1, symArchive!!.objectCount)

        val symObject = symArchive.getObject(0)
        assertNotNull(symObject)
        assertEquals("arm64", symObject!!.arch)
        assertEquals("c48e36936b893d4e83d4923446ff7283", symObject.codeId)
        assertEquals("c48e3693-6b89-3d4e-83d4-923446ff7283", symObject.debugId)
        assertEquals("macho", symObject.fileFormat)
        assertEquals("exe", symObject.kind)

        assertEquals(true, symObject.hasSymtab)
        assertEquals(false, symObject.hasDebug)
        assertEquals(true, symObject.hasUnwind)
        assertEquals(false, symObject.hasSources)

        assertSame(symObject, symArchive.getObject(0))

        symObject.free()
        symArchive.free()
    }
}