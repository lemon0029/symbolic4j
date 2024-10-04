@file:Suppress("FunctionName")

package io.nullptr.symbolic

import com.sun.jna.Library
import com.sun.jna.Native
import io.nullptr.symbolic.common.SymbolicString
import io.nullptr.symbolic.lookup.SymbolCache
import io.nullptr.symbolic.`object`.SymbolicArchive
import io.nullptr.symbolic.`object`.SymbolicObject
import io.nullptr.symbolic.`object`.SymbolicObjectFeatures

internal interface SymbolicLibrary : Library {

    companion object {

        @JvmStatic
        val INSTANCE: SymbolicLibrary = Native.load(SymbolicLibrary::class.java)
    }

    /**
     * Initializes the symbolic library. (rust set panic hook)
     */
    fun symbolic_init()

    /**
     * Returns the last error code.
     */
    fun symbolic_err_get_last_code(): Int

    /**
     * Returns the last error message.
     */
    fun symbolic_err_get_last_message(): SymbolicString.ByValue?

    /**
     * Returns the last error backtrace.
     */
    fun symbolic_err_get_backtrace(): SymbolicString.ByValue?

    /**
     * Clears the last error.
     */
    fun symbolic_err_clear()

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

    /**
     * Returns the features of the given symbolic object.
     */
    fun symbolic_object_get_features(obj: SymbolicObject): SymbolicObjectFeatures.ByValue?

    /**
     * Frees the given symbolic object.
     */
    fun symbolic_object_free(obj: SymbolicObject)

    /**
     * Creates a symcache from the given object.
     */
    fun symbolic_symcache_from_object(obj: SymbolicObject): SymbolCache?

    /**
     * Frees the given symcache.
     */
    fun symbolic_symcache_free(cache: SymbolCache)
}