@file:Suppress("FunctionName")

package io.nullptr.symbolic

import com.sun.jna.Library
import com.sun.jna.Native
import io.nullptr.symbolic.common.SymbolicString
import io.nullptr.symbolic.`object`.SymbolicArchive
import io.nullptr.symbolic.`object`.SymbolicObject

internal interface SymbolicLibrary : Library {

    companion object {

        @JvmStatic
        val INSTANCE: SymbolicLibrary = Native.load(SymbolicLibrary::class.java)
    }

    /**
     * Opens a symbolic archive at the given path.
     */
    fun symbolic_archive_open(path: String): SymbolicArchive?

    /**
     * Frees the given symbolic archive.
     */
    fun symbolic_archive_free(archive: SymbolicArchive)

    /**
     * Returns the number of objects in the given symbolic archive.
     */
    fun symbolic_archive_object_count(archive: SymbolicArchive): Int

    /**
     * Returns the object at the given index in the given symbolic archive.
     */
    fun symbolic_archive_get_object(archive: SymbolicArchive, index: Int): SymbolicObject?

    /**
     * Returns the architecture of the given symbolic object.
     */
    fun symbolic_object_get_arch(obj: SymbolicObject): SymbolicString.ByValue?

    /**
     * Returns the code identifier of the given symbolic object.
     */
    fun symbolic_object_get_code_id(obj: SymbolicObject): SymbolicString.ByValue?

    /**
     * Returns the debug identifier of the given symbolic object.
     */
    fun symbolic_object_get_debug_id(obj: SymbolicObject): SymbolicString.ByValue?

    /**
     * Returns the kind of the given symbolic object.
     */
    fun symbolic_object_get_kind(obj: SymbolicObject): SymbolicString.ByValue?

    /**
     * Returns the file format of the given symbolic object.
     */
    fun symbolic_object_get_file_format(obj: SymbolicObject): SymbolicString.ByValue?
}