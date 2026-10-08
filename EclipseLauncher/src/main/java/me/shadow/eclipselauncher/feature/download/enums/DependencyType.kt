package me.shadow.eclipselauncher.feature.download.enums

/**
 * Dependency types of a mod, each with its own representative color to make them easier to tell apart
 * @param curseforge the name of the type on CurseForge
 * @param modrinth the name of the type on Modrinth
 * @param color the representative color of the type
 */
enum class DependencyType(val curseforge: String?, val modrinth: String?, val color: Int) {
    /**
     * Required: this dependency is necessary for the project; without it the project cannot run properly
     *
     * CurseForge: "3"
     * Modrinth: "required"
     * Color: 0x4CFF9800 (orange, Alpha 30%)
     */
    REQUIRED("3", "required", 0x4CFF9800),

    /**
     * Optional: these dependencies are not required, but they can add extra functionality or features to the project
     *
     * CurseForge: "2"
     * Modrinth: "optional"
     * Color: 0x4C34C759 (light green, Alpha 30%)
     */
    OPTIONAL("2", "optional", 0x4C34C759),

    /**
     * Incompatible: this dependency conflicts with other specific projects or dependencies; using them together is not recommended and may cause errors or failures
     *
     * CurseForge: "5"
     * Modrinth: "incompatible"
     * Color: 0x4CEF5350 (light red, Alpha 30%)
     */
    INCOMPATIBLE("5", "incompatible", 0x4CEF5350),

    /**
     * Embedded: these dependencies are already included in the project, so users do not install them separately; they are part of the project and keep it running properly
     *
     * CurseForge: "1"
     * Modrinth: "embedded"
     * Color: 0x4CFFD54F (light yellow, Alpha 30%)
     */
    EMBEDDED("1", "embedded", 0x4CFFD54F),

    /**
     * Tool: these dependencies are tools for developing or operating the project; they are not themselves required for the project to run
     *
     * CurseForge: "4"
     * Modrinth: null
     * Color: 0x4CBDBDBD (gray, Alpha 30%)
     */
    TOOL("4", null, 0x4CBDBDBD),

    /**
     * Include: these are files or resources contained in the project; although they are not core features, they can provide extra support or functionality
     *
     * CurseForge: "6"
     * Modrinth: null
     * Color: 0x4C9575CD (purple, Alpha 30%)
     */
    INCLUDE("6", null, 0x4C9575CD)
}