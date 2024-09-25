package io.nullptr.symbolic

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import io.nullptr.symbolic.`object`.SymbolicArchive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SymbolicPanicHookTest {

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

        SymbolicLibrary.INSTANCE.symbolic_init()
    }

    @Test
    fun `no panic`() {
        val symArchive = SymbolicArchive.open("external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron")
        assertNotNull(symArchive)
        assertEquals(1, symArchive!!.objectCount)

        val errCode = SymbolicLibrary.INSTANCE.symbolic_err_get_last_code()
        assertEquals(0, errCode)

        val errMessage = SymbolicLibrary.INSTANCE.symbolic_err_get_last_message()
        assertNotNull(errMessage)
        assertNull(errMessage!!.decodeToString())

        val backtrace = SymbolicLibrary.INSTANCE.symbolic_err_get_backtrace()
        assertNotNull(backtrace)
        assertNull(backtrace?.decodeToString())

        SymbolicLibrary.INSTANCE.symbolic_err_clear()
    }

}