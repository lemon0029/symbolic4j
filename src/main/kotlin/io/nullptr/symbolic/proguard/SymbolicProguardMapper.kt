package io.nullptr.symbolic.proguard

import com.sun.jna.PointerType
import io.nullptr.symbolic.SymbolicLibrary

class SymbolicProguardMapper : PointerType() {

    var uuid: String? = null
    var hasLineInfo: Boolean? = null

    companion object {

        fun open(path: String, initializeParamMapping: Boolean = false): SymbolicProguardMapper? {
            return SymbolicLibrary.INSTANCE.let {
                val mapper = it.symbolic_proguardmapper_open(path, initializeParamMapping) ?: return null

                val symbolicUuid = it.symbolic_proguardmapper_get_uuid(mapper)
                mapper.uuid = symbolicUuid.decodeToString()
                mapper.hasLineInfo = it.symbolic_proguardmapper_has_line_info(mapper)

                mapper
            }
        }
    }
}