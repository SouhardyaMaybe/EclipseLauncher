package me.shadow.eclipselauncher.feature.mod.modpack.install

/**
 * Key information of the modpack
 * @param name the name of the modpack, used as the name when creating the new version
 * @param type the category of the modpack (CurseForge, Modrinth, MCBBS)
 */
data class ModPackInfo(val name: String?, val type: ModPackUtils.ModPackEnum)
