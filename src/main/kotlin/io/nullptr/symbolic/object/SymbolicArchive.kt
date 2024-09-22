package io.nullptr.symbolic.`object`

import com.sun.jna.PointerType
import io.nullptr.symbolic.SymbolicLibrary

class SymbolicArchive : PointerType() {
    var objectCount: Int = 0
    private val objects = mutableMapOf<Int, SymbolicObject>()

    companion object {
        fun open(path: String): SymbolicArchive? {
            val symbolicArchive = SymbolicLibrary.INSTANCE.symbolic_archive_open(path)

            if (symbolicArchive != null) {
                symbolicArchive.objectCount = SymbolicLibrary.INSTANCE.symbolic_archive_object_count(symbolicArchive)
            }

            return symbolicArchive
        }
    }

    fun getObject(index: Int): SymbolicObject? {

        if (index < 0 || index >= objectCount) {
            throw IllegalArgumentException("Index out of bounds")
        }

        if (objects.containsKey(index)) {
            return objects[index]!!
        }

        val symObject = SymbolicLibrary.INSTANCE.symbolic_archive_get_object(this, index)

        if (symObject != null) {
            symObject.init()
            objects[index] = symObject
        }

        return symObject
    }

    fun free() {
        SymbolicLibrary.INSTANCE.symbolic_archive_free(this)
    }
}