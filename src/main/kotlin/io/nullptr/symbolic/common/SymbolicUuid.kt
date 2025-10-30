package io.nullptr.symbolic.common

import com.sun.jna.Structure

@Structure.FieldOrder("data")
open class SymbolicUuid : Structure() {

    @JvmField
    var data: ByteArray? = ByteArray(16)

    @OptIn(ExperimentalStdlibApi::class)
    fun decodeToString(): String {
        if (data == null) {
            return ""
        }

        return data!!.toHexString()
    }

    class ByValue : SymbolicUuid(), Structure.ByValue
}