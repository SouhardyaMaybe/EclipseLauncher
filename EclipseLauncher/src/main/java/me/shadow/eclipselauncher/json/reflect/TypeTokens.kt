package me.shadow.eclipselauncher.json.reflect

/**
 * Kotlin helper capturing the reified type argument as a [TypeToken],
 * e.g. `typeTokenOf<List<SettingAttribute>>().type`.
 */
inline fun <reified T> typeTokenOf(): TypeToken<T> = object : TypeToken<T>() {}
