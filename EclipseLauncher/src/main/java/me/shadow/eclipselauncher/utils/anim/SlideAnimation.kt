package me.shadow.eclipselauncher.utils.anim

import me.shadow.eclipselauncher.anim.AnimPlayer

interface SlideAnimation {
    fun slideIn(animPlayer: AnimPlayer)
    fun slideOut(animPlayer: AnimPlayer)
}