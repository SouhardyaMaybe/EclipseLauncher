package me.shadow.eclipselauncher.core.modloaders

/**
 * A single version entry of the Fabric/Quilt meta API.
 *
 * The properties compile to instance fields named `version`, `stable` and `loader`, which is what
 * the field-reflection based JSON codec binds to - the names must not change.
 */
open class FabricVersion {
    var version: String? = null
    var stable: Boolean = false

    override fun toString(): String = version ?: "null"

    class LoaderDescriptor : FabricVersion() {
        var loader: FabricVersion? = null

        override fun toString(): String = loader?.toString() ?: "null"
    }
}
