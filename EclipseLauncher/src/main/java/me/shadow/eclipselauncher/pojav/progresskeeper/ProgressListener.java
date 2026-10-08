package me.shadow.eclipselauncher.pojav.progresskeeper;

public interface ProgressListener {
    void onProgressStarted();
    void onProgressUpdated(int progress, int resid, Object... va);
    void onProgressEnded();
}
