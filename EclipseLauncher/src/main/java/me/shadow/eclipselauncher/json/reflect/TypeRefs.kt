package me.shadow.eclipselauncher.json.reflect

/**
 * Kotlin helper capturing the reified type argument as a [TypeRef],
 * e.g. `typeRefOf<List<SettingAttribute>>().type`.
 */
inline fun <reified T> typeRefOf(): TypeRef<T> = object : TypeRef<T>() {}
