package io.nullptr.symbolic.`object`

import com.sun.jna.Structure

@Structure.FieldOrder("symtab", "debug", "unwind", "sources")
internal open class SymbolicObjectFeatures : Structure() {

    /**
     * Whether the object has a symbol table.
     */
    @JvmField
    val symtab: Byte = 0

    /**
     * Whether the object has debug information.
     */
    @JvmField
    val debug: Byte = 0

    /**
     * Whether the object has unwind information.
     */
    @JvmField
    val unwind: Byte = 0

    /**
     * Whether the object has source information.
     */
    @JvmField
    val sources: Byte = 0

    class ByValue : SymbolicObjectFeatures(), Structure.ByValue
}