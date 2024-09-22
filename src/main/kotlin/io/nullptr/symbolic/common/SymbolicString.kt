package io.nullptr.symbolic.common

import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder("data", "len", "owned")
open class SymbolicString : Structure() {

    @JvmField
    var data: Pointer? = Pointer.NULL

    @JvmField
    var len: Long = 0

    @JvmField
    var owned: Boolean = false

    fun decodeToString(): String {
        try {

            if (data == null) {
                throw IllegalStateException("Data is null")
            }

            if (len <= 0) {
                throw IllegalStateException("Length is less than or equal to 0")
            }

            val bytes = data?.getByteArray(0, len.toInt()) ?: throw IllegalStateException("Failed to get byte array")
            return String(bytes, Charsets.UTF_8)
        } finally {
            if (owned) {
                println("TODO: Free owned memory for SymbolicString")
            }
        }
    }

    class ByValue : SymbolicString(), Structure.ByValue
}