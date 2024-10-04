package io.nullptr.symbolic.`object`

import com.sun.jna.PointerType
import io.nullptr.symbolic.SymbolicLibrary
import io.nullptr.symbolic.lookup.SymbolCache

class SymbolicObject : PointerType() {

    var arch: String = ""
    var codeId: String = ""

    /**
     * dwarfdump --uuid $path
     */
    var debugId: String = ""
    var kind: String = ""
    var fileFormat: String = ""

    var hasSymtab: Boolean = false
    var hasDebug: Boolean = false
    var hasUnwind: Boolean = false
    var hasSources: Boolean = false

    private var initialized = false
    private var released = false

    @Synchronized
    internal fun init() {
        if (initialized) {
            return
        }

        val symbolicLibrary = SymbolicLibrary.INSTANCE

        symbolicLibrary.symbolic_object_get_arch(this)?.let {
            arch = it.decodeToString() ?: ""
        }

        symbolicLibrary.symbolic_object_get_code_id(this)?.let {
            codeId = it.decodeToString() ?: ""
        }

        symbolicLibrary.symbolic_object_get_debug_id(this)?.let {
            debugId = it.decodeToString() ?: ""
        }

        symbolicLibrary.symbolic_object_get_kind(this)?.let {
            kind = it.decodeToString() ?: ""
        }

        symbolicLibrary.symbolic_object_get_file_format(this)?.let {
            fileFormat = it.decodeToString() ?: ""
        }

        symbolicLibrary.symbolic_object_get_features(this)?.let {
            hasSymtab = it.symtab != 0.toByte()
            hasDebug = it.debug != 0.toByte()
            hasUnwind = it.unwind != 0.toByte()
            hasSources = it.sources != 0.toByte()
        }

        initialized = true
    }

    fun createSymCache(): SymbolCache? {
        return SymbolicLibrary.INSTANCE.symbolic_symcache_from_object(this)
    }

    fun free() {
        if (released) {
            return
        }

        SymbolicLibrary.INSTANCE.symbolic_object_free(this)
        released = true
    }
}