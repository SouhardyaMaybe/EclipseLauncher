package me.shadow.eclipselauncher.core.customcontrols.gamepad;

import me.shadow.eclipselauncher.core.GrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(GrabListener grabListener);
}
