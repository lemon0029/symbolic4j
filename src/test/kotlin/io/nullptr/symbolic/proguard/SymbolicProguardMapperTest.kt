package io.nullptr.symbolic.proguard

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import io.nullptr.symbolic.SymbolicLibrary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class SymbolicProguardMapperTest {

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
    fun `open a proguard mapper`() {
        val mapper = SymbolicProguardMapper.open("external/resources/proguard-mapping.txt")
        assertNotNull(mapper)
        assertNotNull(mapper!!.uuid)
        assertEquals("c4ab52c9f59a58368576248c500c738c", mapper.uuid)
        assertNotNull(mapper.hasLineInfo)
        assertTrue(mapper.hasLineInfo!!)
    }
}