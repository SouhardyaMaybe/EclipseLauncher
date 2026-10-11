package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.view.MotionEvent
import android.view.View

interface TouchEventProcessor {
    fun processTouchEvent(motionEvent: MotionEvent): Boolean
    fun cancelPendingActions()
    fun dispatchTouchEvent(event: MotionEvent, view: View) {}
}
