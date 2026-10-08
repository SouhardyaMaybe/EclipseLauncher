package me.shadow.eclipselauncher.ui.subassembly.account

import me.shadow.eclipselauncher.pojav.value.MinecraftAccount

interface SelectAccountListener {
    fun onSelect(account: MinecraftAccount)
}
