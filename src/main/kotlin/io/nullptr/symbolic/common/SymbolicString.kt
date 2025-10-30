package io.nullptr.symbolic.common

import com.sun.jna.Pointer
import com.sun.jna.Structure
import io.nullptr.symbolic.SymbolicLibrary

@Structure.FieldOrder("data", "len", "owned")
open class SymbolicString : Structure() {

    @JvmField
    var data: Pointer? = Pointer.NULL

    @JvmField
    var len: Long = 0

    @JvmField
    var owned: Boolean = false

    fun decodeToString(): String? {
        try {

            if (data == null) {
                return null
            }

            if (len <= 0) {
                return null
            }

            val bytes = data?.getByteArray(0, len.toInt()) ?: throw IllegalStateException("Failed to get byte array")
            return String(bytes, Charsets.UTF_8)
        } finally {
            if (owned) {
                 SymbolicLibrary.INSTANCE.symbolic_str_free(this.pointer)
            }
        }
    }

    class ByValue : SymbolicString(), Structure.ByValue
}