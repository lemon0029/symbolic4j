package io.nullptr.symbolic.`object`

import com.sun.jna.Structure
import io.nullptr.symbolic.common.SymbolicString

@Structure.FieldOrder("symAddr", "instrAddr", "lineNumber", "lang", "symbol", "path")
class SymbolicSourceLocation : Structure() {

    @JvmField
    var symAddr: Long = 0

    @JvmField
    var instrAddr: Long = 0

    @JvmField
    var lineNumber: Int = 0

    @JvmField
    var lang: SymbolicString? = null

    @JvmField
    var symbol: SymbolicString? = null

    @JvmField
    var path: SymbolicString? = null
}