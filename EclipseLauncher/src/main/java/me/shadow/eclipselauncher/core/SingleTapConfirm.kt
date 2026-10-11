package me.shadow.eclipselauncher.core

import android.view.GestureDetector
import android.view.MotionEvent

class SingleTapConfirm : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return true
    }
}
