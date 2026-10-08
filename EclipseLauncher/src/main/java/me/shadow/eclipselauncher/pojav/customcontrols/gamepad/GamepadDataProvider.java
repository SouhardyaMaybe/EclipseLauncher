package me.shadow.eclipselauncher.pojav.customcontrols.gamepad;

import me.shadow.eclipselauncher.pojav.GrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(GrabListener grabListener);
}
