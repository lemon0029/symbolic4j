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

        symCache!!.free()
    }
}