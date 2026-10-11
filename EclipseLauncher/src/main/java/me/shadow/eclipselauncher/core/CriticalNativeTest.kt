package me.shadow.eclipselauncher.core

import dalvik.annotation.optimization.CriticalNative

/**
 * The native input bridge (jni/input_bridge_v3.c) finds this class, registers
 * [testCriticalNative] through RegisterNatives and invokes [invokeTest] to detect whether the
 * VM supports the CriticalNative calling convention. Both members must stay static natives on
 * this exact class name - the JNI lookup derives the class and method from it.
 */
object CriticalNativeTest {
    @CriticalNative
    @JvmStatic
    external fun testCriticalNative(arg0: Int, arg1: Int)

    @JvmStatic
    fun invokeTest() {
        testCriticalNative(0, 0)
    }
}
