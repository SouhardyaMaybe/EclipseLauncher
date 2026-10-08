package me.shadow.eclipselauncher.pojav.customcontrols.mouse;

import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;

import android.os.Handler;

import me.shadow.eclipselauncher.setting.AllStaticSettings;

import me.shadow.eclipselauncher.pojav.LwjglGlfwKeycode;

public class DropGesture implements Runnable{
    private final Handler mHandler;
    private boolean mActive;

    public DropGesture(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public void submit() {
        if(!mActive) {
            mActive = true;
            mHandler.postDelayed(this, AllStaticSettings.timeLongPressTrigger);
        }
    }

    public void cancel() {
        mActive = false;
        mHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        if(!mActive) return;
        sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_Q);
        mHandler.postDelayed(this, 250);
    }
}
