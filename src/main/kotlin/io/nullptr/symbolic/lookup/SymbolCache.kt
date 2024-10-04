package io.nullptr.symbolic.lookup

import com.github.luben.zstd.Zstd
import com.sun.jna.PointerType
import io.nullptr.symbolic.SymbolicLibrary
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeBytes

class SymbolCache : PointerType() {

    var size: Long = -1L
    var arch: String = ""
    var debugId: String = ""
    var version: Int = -1

    private var cacheBytes: ByteArray? = null

    private var initialized = false
    private var released = false

    companion object {

        fun open(path: String): SymbolCache? {

            val filePath = Path.of(path)

            if (Files.notExists(filePath)) {
                throw IllegalArgumentException("SymCache file does not exist")
            }

            // TODO: Add support for Zstd compressed symcache files.

            val symbolicCache = SymbolicLibrary.INSTANCE.symbolic_symcache_open(path)

            symbolicCache?.init()

            return symbolicCache
        }
    }

    fun getBytes(): ByteArray {

        if (cacheBytes != null) {
            return cacheBytes!!
        }

        val buf = SymbolicLibrary.INSTANCE.symbolic_symcache_get_bytes(this)

        return buf?.getByteArray(0, size.toInt())
            ?: throw IllegalStateException("Failed to get bytes from symcache pointer")
    }

    /**
     * Dumps the symcache to a file.
     *
     * @param path The path to the dump file.
     * @param usingZstdCompression Whether to use Zstd compression.
     * @return The path to the dumped file. if Zstd compression is used, the file will have a `.zst` extension.
     */
    fun dumpToFile(path: Path, usingZstdCompression: Boolean = false): Path {

        // Append the `.zst` extension if Zstd compression is used.
        val fileName = if (!usingZstdCompression) path.fileName.toString()
        else path.fileName.toString() + ".zst"

        // Compress the bytes using Zstd if required.
        val bytes = if (!usingZstdCompression) getBytes()
        else Zstd.compress(getBytes())

        // Create the parent directories if they don't exist.
        path.parent.let {
            if (!it.toFile().exists()) {
                it.toFile().mkdirs()
            }
        }

        // Write the bytes to the file.
        val dest = path.parent.resolve(fileName)
        dest.writeBytes(bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

        return dest
    }

    @Synchronized
    fun init() {
        if (initialized) {
            return
        }

        val symbolicLibrary = SymbolicLibrary.INSTANCE

        size = symbolicLibrary.symbolic_symcache_get_size(this)
        version = symbolicLibrary.symbolic_symcache_get_version(this)

        symbolicLibrary.symbolic_symcache_get_arch(this)?.let {
            arch = it.decodeToString() ?: ""
        }

        symbolicLibrary.symbolic_symcache_get_debug_id(this)?.let {
            debugId = it.decodeToString() ?: ""
        }

        initialized = true
    }

    fun free() {
        if (released) {
            return
        }

        SymbolicLibrary.INSTANCE.symbolic_symcache_free(this)

        released = true
    }
}