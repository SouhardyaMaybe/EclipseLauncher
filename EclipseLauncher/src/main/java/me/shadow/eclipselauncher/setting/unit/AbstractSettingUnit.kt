package me.shadow.eclipselauncher.setting.unit

import androidx.annotation.CheckResult
import me.shadow.eclipselauncher.setting.Settings

abstract class AbstractSettingUnit<V>(
    val key: String,
    val defaultValue: V
) {
    /**
     * @return the current setting value
     */
    abstract fun getValue(): V

    /**
     * @return store the value and return a setting builder
     */
    @CheckResult
    fun put(value: V): Settings.Manager.SettingBuilder = Settings.Manager.put(key, value!!)

    /**
     * Reset the current setting unit to its default value
     */
    fun reset() {
        Settings.Manager.put(key, defaultValue!!).save()
    }
}