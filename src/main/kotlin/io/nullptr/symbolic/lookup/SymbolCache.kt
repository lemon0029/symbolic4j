package io.nullptr.symbolic.lookup

import com.sun.jna.PointerType
import io.nullptr.symbolic.SymbolicLibrary

class SymbolCache : PointerType() {

    private var released = false

    fun free() {
        if (released) {
            return
        }

        SymbolicLibrary.INSTANCE.symbolic_symcache_free(this)

        released = true
    }
}