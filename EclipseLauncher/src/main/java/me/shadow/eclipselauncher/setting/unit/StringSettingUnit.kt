package me.shadow.eclipselauncher.setting.unit

import me.shadow.eclipselauncher.setting.Settings.Manager

class StringSettingUnit(key: String, defaultValue: String) : AbstractSettingUnit<String>(key, defaultValue) {
    override fun getValue() = Manager.getValue(key, defaultValue) { it }
}