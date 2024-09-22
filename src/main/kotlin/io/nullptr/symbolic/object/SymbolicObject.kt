package io.nullptr.symbolic.`object`

import com.sun.jna.PointerType
import io.nullptr.symbolic.SymbolicLibrary

class SymbolicObject : PointerType() {

    lateinit var arch: String
    lateinit var codeId: String
    lateinit var debugId: String
    lateinit var kind: String
    lateinit var fileFormat: String

    private var initialized = false

    @Synchronized
    internal fun init() {
        if (initialized) {
            return
        }

        arch = SymbolicLibrary.INSTANCE.symbolic_object_get_arch(this)?.decodeToString() ?: "Unknown"
        codeId = SymbolicLibrary.INSTANCE.symbolic_object_get_code_id(this)?.decodeToString() ?: "Unknown"
        debugId = SymbolicLibrary.INSTANCE.symbolic_object_get_debug_id(this)?.decodeToString() ?: "Unknown"
        kind = SymbolicLibrary.INSTANCE.symbolic_object_get_kind(this)?.decodeToString() ?: "Unknown"
        fileFormat = SymbolicLibrary.INSTANCE.symbolic_object_get_file_format(this)?.decodeToString() ?: "Unknown"

        initialized = true
    }
}