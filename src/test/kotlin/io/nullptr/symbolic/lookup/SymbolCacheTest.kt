package io.nullptr.symbolic.lookup

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import io.nullptr.symbolic.SymbolicLibrary
import io.nullptr.symbolic.`object`.SymbolicArchive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SymbolCacheTest {

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
    fun `create symbol cache from a given object`() {
        val filePath = "external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron"
        assertTrue(Files.exists(Path.of(filePath)))

        val symArchive = SymbolicArchive.open(filePath)
        assertNotNull(symArchive)
        assertEquals(1, symArchive!!.objectCount)

        val symObject = symArchive.getObject(0)
        assertNotNull(symObject)

        val symCache = symObject!!.createSymCache()
        assertNotNull(symCache)

        assertEquals(8, symCache!!.version)
        assertEquals(596, symCache.size)
        assertEquals("x86_64", symCache.arch)
        assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", symCache.debugId)

        symCache.free()
    }

    @Test
    fun `dump symbol cache to a file`() {
        val dir = Path.of("external/resources/Electron.app.dSYM/Contents/Resources/DWARF")
        val filePath = dir.resolve("Electron")
        assertTrue(Files.exists(filePath))

        val symArchive = SymbolicArchive.open(filePath.toString())
        assertNotNull(symArchive)
        assertEquals(1, symArchive!!.objectCount)

        val symObject = symArchive.getObject(0)
        assertNotNull(symObject)

        val symCache = symObject!!.createSymCache()
        assertNotNull(symCache)

        val bytes = symCache!!.getBytes()
        assertNotNull(bytes)
        assertEquals(596, bytes.size)

        val path = dir.resolve("Electron.sym")

        val symCacheFile = symCache.dumpToFile(path, false)
        assertTrue(Files.exists(symCacheFile))

        val compressedSymCacheFile = symCache.dumpToFile(path, true)

        assertTrue(compressedSymCacheFile.fileName.toString().endsWith(".zst"))
        assertTrue(Files.exists(compressedSymCacheFile))

        SymbolCache.open(path.toString())?.let {
            assertEquals(8, it.version)
            assertEquals(596, it.size)
            assertEquals("x86_64", it.arch)
            assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", it.debugId)
        }

        symCache.free()
    }
}