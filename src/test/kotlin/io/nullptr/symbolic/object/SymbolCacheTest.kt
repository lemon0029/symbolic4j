package io.nullptr.symbolic.`object`

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import io.nullptr.symbolic.SymbolicLibrary
import org.junit.jupiter.api.Assertions
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
        Assertions.assertTrue(Files.exists(Path.of(filePath)))

        val symArchive = SymbolicArchive.open(filePath)
        Assertions.assertNotNull(symArchive)
        Assertions.assertEquals(1, symArchive!!.objectCount)

        val symObject = symArchive.getObject(0)
        Assertions.assertNotNull(symObject)

        val symCache = symObject!!.buildSymCache()
        Assertions.assertNotNull(symCache)

        Assertions.assertEquals(8, symCache!!.version)
        Assertions.assertEquals(596, symCache.size)
        Assertions.assertEquals("x86_64", symCache.arch)
        Assertions.assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", symCache.debugId)

        symCache.free()
    }

    @Test
    fun `create symbol cache from a super big given object`() {
        val dir =
            "/Users/nullptr/Downloads/electron-v34.0.0-alpha.3-mas-arm64-dsym-snapshot/v8_context_snapshot_generator.dSYM"
        val filePath = "$dir/Contents/Resources/DWARF/v8_context_snapshot_generator"

        Assertions.assertTrue(Files.exists(Path.of(filePath)))

        val symArchive = SymbolicArchive.open(filePath)
        Assertions.assertNotNull(symArchive)
        Assertions.assertEquals(1, symArchive!!.objectCount)

        val symObject = symArchive.getObject(0)
        Assertions.assertNotNull(symObject)

        val symCache = symObject!!.buildSymCache()
        Assertions.assertNotNull(symCache)

        Assertions.assertEquals(8, symCache!!.version)
        Assertions.assertEquals(596, symCache.size)
        Assertions.assertEquals("x86_64", symCache.arch)
        Assertions.assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", symCache.debugId)

        symCache.free()
    }

    @Test
    fun `dump symbol cache to a file`() {
        val dir = Path.of("external/resources/Electron.app.dSYM/Contents/Resources/DWARF")
        val filePath = dir.resolve("Electron")
        Assertions.assertTrue(Files.exists(filePath))

        val symArchive = SymbolicArchive.open(filePath.toString())
        Assertions.assertNotNull(symArchive)
        Assertions.assertEquals(1, symArchive!!.objectCount)

        val symObject = symArchive.getObject(0)
        Assertions.assertNotNull(symObject)

        val symCache = symObject!!.buildSymCache()
        Assertions.assertNotNull(symCache)

        val bytes = symCache!!.getBytes()
        Assertions.assertNotNull(bytes)
        Assertions.assertEquals(596, bytes.size)

        val path = dir.resolve("Electron.sym")

        val symCacheFile = symCache.dumpToFile(path, false)
        Assertions.assertTrue(Files.exists(symCacheFile))

        val compressedSymCacheFile = symCache.dumpToFile(path, true)

        Assertions.assertTrue(compressedSymCacheFile.fileName.toString().endsWith(".zst"))
        Assertions.assertTrue(Files.exists(compressedSymCacheFile))

        SymbolicSymCache.open(path.toString())?.let {
            Assertions.assertEquals(8, it.version)
            Assertions.assertEquals(596, it.size)
            Assertions.assertEquals("x86_64", it.arch)
            Assertions.assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", it.debugId)
        }

        symCache.free()
    }


    @Test
    fun `lookup symbol cache from a file`() {
        val filePath = "external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron.sym"
        Assertions.assertTrue(Files.exists(Path.of(filePath)))

        val symCache = SymbolicSymCache.open(filePath)
        Assertions.assertNotNull(symCache)

        Assertions.assertEquals(8, symCache!!.version)
        Assertions.assertEquals(596, symCache.size)
        Assertions.assertEquals("x86_64", symCache.arch)
        Assertions.assertEquals("cb63147a-c9dc-308b-8ca1-ee92a5042e8e", symCache.debugId)

        val sourceLocations = symCache.lookup((0x107BB9F25 - 0x107BB9000).toInt())

        symCache.free()
    }
}