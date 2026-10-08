package me.shadow.eclipselauncher.event.value

import me.shadow.eclipselauncher.feature.version.install.Addon
import me.shadow.eclipselauncher.feature.version.install.InstallTaskItem

/**
 * 安装任务开始时，将使用这个事件进行通知
 * @see me.shadow.eclipselauncher.ui.fragment.InstallGameFragment
 * @param minecraftVersion MC原版版本
 * @param customVersionName 自定义的版本文件夹名称
 * @param taskMap 安装任务
 */
class InstallGameEvent(
    val minecraftVersion: String,
    val customVersionName: String,
    val taskMap: Map<Addon, InstallTaskItem>
)