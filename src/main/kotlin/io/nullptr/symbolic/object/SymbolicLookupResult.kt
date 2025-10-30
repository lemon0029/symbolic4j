package io.nullptr.symbolic.`object`

import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder("itemPointer", "len")
internal open class SymbolicLookupResult : Structure() {

    @JvmField
    var itemPointer: Pointer? = Pointer.NULL

    @JvmField
    var len: Long = 0

    fun getItems(): List<SymbolicSourceLocation> {

        if (len == 0L || itemPointer == Pointer.NULL) return emptyList()

        val result = mutableListOf<SymbolicSourceLocation>()

        (0 until len).forEach { _ ->
            val item = newInstance(SymbolicSourceLocation::class.java, itemPointer)
            item.read()
            result.add(item)
        }

        return emptyList()
    }

    class ByValue : SymbolicLookupResult(), Structure.ByValue
}