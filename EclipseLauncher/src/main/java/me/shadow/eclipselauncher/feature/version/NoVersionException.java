package me.shadow.eclipselauncher.feature.version;

public class NoVersionException extends RuntimeException {
    public NoVersionException(String message) {
        super(message);
    }
}
