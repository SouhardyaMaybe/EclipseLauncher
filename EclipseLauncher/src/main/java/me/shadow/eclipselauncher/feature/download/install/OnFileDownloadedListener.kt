package me.shadow.eclipselauncher.feature.download.install

import java.io.File

fun interface OnFileDownloadedListener {
    fun onEnded(file: File)
}