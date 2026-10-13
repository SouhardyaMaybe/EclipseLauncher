/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.util.tinyfd;

import java.nio.ByteBuffer;

import org.lwjgl.PointerBuffer;

import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Native file dialogs. This class shadows the stock LWJGL binding so that the merged runtime exposes the exact
 * entry points Minecraft 26.x links against (including the {@code int}-based {@link #tinyfd_messageBox} overload)
 * while keeping full binary compatibility with older clients that use the {@code boolean} overload.
 */
public class TinyFileDialogs {

    public static final String tinyfd_version = "tinyfd_version";
    public static final String tinyfd_needs = "tinyfd_needs";
    public static final String tinyfd_response = "tinyfd_response";
    public static final String tinyfd_verbose = "tinyfd_verbose";
    public static final String tinyfd_silent = "tinyfd_silent";
    public static final String tinyfd_allowCursesDialogs = "tinyfd_allowCursesDialogs";
    public static final String tinyfd_forceConsole = "tinyfd_forceConsole";
    public static final String tinyfd_assumeGraphicDisplay = "tinyfd_assumeGraphicDisplay";
    public static final String tinyfd_winUtf8 = "tinyfd_winUtf8";

    protected TinyFileDialogs() {
        throw new UnsupportedOperationException();
    }

    static {
        Library.loadSystem(System::load, System::loadLibrary, TinyFileDialogs.class, "org.lwjgl.tinyfd", Platform.mapLibraryNameBundled("lwjgl_tinyfd"));
    }

    // ---------------------------------------------------------------------
    // Globals
    // ---------------------------------------------------------------------

    public static native long ntinyfd_getGlobalChar(long key);

    public static String tinyfd_getGlobalChar(ByteBuffer key) {
        if (CHECKS) {
            checkNT1(key);
        }
        return memASCIISafe(ntinyfd_getGlobalChar(memAddress(key)));
    }

    public static String tinyfd_getGlobalChar(CharSequence key) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nASCII(key, true);
            long __key = key == null ? 0L : stack.getPointerAddress();
            return memASCIISafe(ntinyfd_getGlobalChar(__key));
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native int ntinyfd_getGlobalInt(long key);

    public static int tinyfd_getGlobalInt(ByteBuffer key) {
        if (CHECKS) {
            checkNT1(key);
        }
        return ntinyfd_getGlobalInt(memAddress(key));
    }

    public static int tinyfd_getGlobalInt(CharSequence key) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nASCII(key, true);
            long __key = key == null ? 0L : stack.getPointerAddress();
            return ntinyfd_getGlobalInt(__key);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native int ntinyfd_setGlobalInt(long key, int value);

    public static int tinyfd_setGlobalInt(ByteBuffer key, int value) {
        if (CHECKS) {
            checkNT1(key);
        }
        return ntinyfd_setGlobalInt(memAddress(key), value);
    }

    public static int tinyfd_setGlobalInt(CharSequence key, int value) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nASCII(key, true);
            long __key = key == null ? 0L : stack.getPointerAddress();
            return ntinyfd_setGlobalInt(__key, value);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native void tinyfd_beep();

    // ---------------------------------------------------------------------
    // Notifications and message boxes
    // ---------------------------------------------------------------------

    public static native int ntinyfd_notifyPopup(long title, long message, long icon);

    public static int tinyfd_notifyPopup(ByteBuffer title, ByteBuffer message, ByteBuffer icon) {
        if (CHECKS) {
            checkNT1Safe(title);
            checkNT1Safe(message);
            checkNT1(icon);
        }
        return ntinyfd_notifyPopup(memAddressSafe(title), memAddressSafe(message), memAddress(icon));
    }

    public static int tinyfd_notifyPopup(CharSequence title, CharSequence message, CharSequence icon) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(message, true);
            long __message = message == null ? 0L : stack.getPointerAddress();
            stack.nASCII(icon, true);
            long __icon = stack.getPointerAddress();
            return ntinyfd_notifyPopup(__title, __message, __icon);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native int ntinyfd_messageBox(long title, long message, long type, long icon, int aModal);

    public static boolean tinyfd_messageBox(ByteBuffer title, ByteBuffer message, ByteBuffer type, ByteBuffer icon, boolean aModal) {
        if (CHECKS) {
            checkNT1Safe(title);
            checkNT1Safe(message);
            checkNT1(type);
            checkNT1(icon);
        }
        return ntinyfd_messageBox(memAddressSafe(title), memAddressSafe(message), memAddress(type), memAddress(icon), aModal ? 1 : 0) != 0;
    }

    public static boolean tinyfd_messageBox(CharSequence title, CharSequence message, CharSequence type, CharSequence icon, boolean aModal) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(message, true);
            long __message = message == null ? 0L : stack.getPointerAddress();
            stack.nASCII(type, true);
            long __type = stack.getPointerAddress();
            stack.nASCII(icon, true);
            long __icon = stack.getPointerAddress();
            return ntinyfd_messageBox(__title, __message, __type, __icon, aModal ? 1 : 0) != 0;
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static int tinyfd_messageBox(ByteBuffer title, ByteBuffer message, ByteBuffer type, ByteBuffer icon, int aModal) {
        if (CHECKS) {
            checkNT1Safe(title);
            checkNT1Safe(message);
            checkNT1(type);
            checkNT1(icon);
        }
        return ntinyfd_messageBox(memAddressSafe(title), memAddressSafe(message), memAddress(type), memAddress(icon), aModal);
    }

    public static int tinyfd_messageBox(CharSequence title, CharSequence message, CharSequence type, CharSequence icon, int aModal) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(message, true);
            long __message = message == null ? 0L : stack.getPointerAddress();
            stack.nASCII(type, true);
            long __type = stack.getPointerAddress();
            stack.nASCII(icon, true);
            long __icon = stack.getPointerAddress();
            return ntinyfd_messageBox(__title, __message, __type, __icon, aModal);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    // ---------------------------------------------------------------------
    // Dialogs
    // ---------------------------------------------------------------------

    public static native long ntinyfd_inputBox(long title, long message, long defaultInput);

    public static String tinyfd_inputBox(ByteBuffer title, ByteBuffer message, ByteBuffer defaultInput) {
        if (CHECKS) {
            checkNT1Safe(title);
            checkNT1Safe(message);
            checkNT1(defaultInput);
        }
        return memUTF8Safe(ntinyfd_inputBox(memAddressSafe(title), memAddressSafe(message), memAddress(defaultInput)));
    }

    public static String tinyfd_inputBox(CharSequence title, CharSequence message, CharSequence defaultInput) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(message, true);
            long __message = message == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(defaultInput, true);
            long __defaultInput = defaultInput == null ? 0L : stack.getPointerAddress();
            return memUTF8Safe(ntinyfd_inputBox(__title, __message, __defaultInput));
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native long ntinyfd_saveFileDialog(long title, long message, int aNumFilterPatterns, long aFilterPatterns, long aDefaultPathAndFileName);

    public static String tinyfd_saveFileDialog(ByteBuffer title, ByteBuffer message, PointerBuffer aFilterPatterns, ByteBuffer aDefaultPathAndFileName) {
        if (CHECKS) {
            checkNT1Safe(title);
            checkNT1Safe(message);
            checkNT1Safe(aDefaultPathAndFileName);
        }
        return memUTF8Safe(ntinyfd_saveFileDialog(
            memAddressSafe(title),
            memAddressSafe(message),
            remainingSafe(aFilterPatterns),
            memAddressSafe(aFilterPatterns),
            memAddressSafe(aDefaultPathAndFileName)
        ));
    }

    public static String tinyfd_saveFileDialog(CharSequence title, CharSequence message, PointerBuffer aFilterPatterns, CharSequence aDefaultPathAndFileName) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(message, true);
            long __message = message == null ? 0L : stack.getPointerAddress();
            int __numPatterns = remainingSafe(aFilterPatterns);
            long __patterns = memAddressSafe(aFilterPatterns);
            stack.nUTF8Safe(aDefaultPathAndFileName, true);
            long __defaultPath = aDefaultPathAndFileName == null ? 0L : stack.getPointerAddress();
            return memUTF8Safe(ntinyfd_saveFileDialog(__title, __message, __numPatterns, __patterns, __defaultPath));
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native long ntinyfd_openFileDialog(long title, long message, int aNumFilterPatterns, long aFilterPatterns, long aDefaultPathAndFileName, int aAllowMultiple);

    public static String tinyfd_openFileDialog(ByteBuffer title, ByteBuffer message, PointerBuffer aFilterPatterns, ByteBuffer aDefaultPathAndFileName, boolean aAllowMultiple) {
        if (CHECKS) {
            checkNT1Safe(title);
            checkNT1Safe(message);
            checkNT1Safe(aDefaultPathAndFileName);
        }
        return memUTF8Safe(ntinyfd_openFileDialog(
            memAddressSafe(title),
            memAddressSafe(message),
            remainingSafe(aFilterPatterns),
            memAddressSafe(aFilterPatterns),
            memAddressSafe(aDefaultPathAndFileName),
            aAllowMultiple ? 1 : 0
        ));
    }

    public static String tinyfd_openFileDialog(CharSequence title, CharSequence message, PointerBuffer aFilterPatterns, CharSequence aDefaultPathAndFileName, boolean aAllowMultiple) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(message, true);
            long __message = message == null ? 0L : stack.getPointerAddress();
            int __numPatterns = remainingSafe(aFilterPatterns);
            long __patterns = memAddressSafe(aFilterPatterns);
            stack.nUTF8Safe(aDefaultPathAndFileName, true);
            long __defaultPath = aDefaultPathAndFileName == null ? 0L : stack.getPointerAddress();
            return memUTF8Safe(ntinyfd_openFileDialog(__title, __message, __numPatterns, __patterns, __defaultPath, aAllowMultiple ? 1 : 0));
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native long ntinyfd_selectFolderDialog(long title, long defaultPath);

    public static String tinyfd_selectFolderDialog(ByteBuffer title, ByteBuffer defaultPath) {
        if (CHECKS) {
            checkNT1Safe(title);
            checkNT1Safe(defaultPath);
        }
        return memUTF8Safe(ntinyfd_selectFolderDialog(memAddressSafe(title), memAddressSafe(defaultPath)));
    }

    public static String tinyfd_selectFolderDialog(CharSequence title, CharSequence defaultPath) {
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nUTF8Safe(defaultPath, true);
            long __defaultPath = defaultPath == null ? 0L : stack.getPointerAddress();
            return memUTF8Safe(ntinyfd_selectFolderDialog(__title, __defaultPath));
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    public static native long ntinyfd_colorChooser(long title, long message, long aResultHex, long aResultRGB);

    public static String tinyfd_colorChooser(ByteBuffer title, ByteBuffer message, ByteBuffer aResultHex, ByteBuffer aResultRGB) {
        if (CHECKS) {
            checkSafe(aResultHex, 3);
            check(aResultRGB, 3);
        }
        return memUTF8Safe(ntinyfd_colorChooser(memAddressSafe(title), memAddressSafe(message), memAddressSafe(aResultHex), memAddress(aResultRGB)));
    }

    public static String tinyfd_colorChooser(CharSequence title, CharSequence message, ByteBuffer aResultHex, ByteBuffer aResultRGB) {
        if (CHECKS) {
            checkSafe(aResultHex, 3);
            check(aResultRGB, 3);
        }
        MemoryStack stack = stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(title, true);
            long __title = title == null ? 0L : stack.getPointerAddress();
            stack.nASCIISafe(message, true);
            long __message = message == null ? 0L : stack.getPointerAddress();
            return memUTF8Safe(ntinyfd_colorChooser(__title, __message, memAddressSafe(aResultHex), memAddress(aResultRGB)));
        } finally {
            stack.setPointer(stackPointer);
        }
    }

}
