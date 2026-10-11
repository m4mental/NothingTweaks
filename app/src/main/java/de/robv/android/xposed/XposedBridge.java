package de.robv.android.xposed;

import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class XposedBridge {
    private static final String TAG = "NothingTweaks";
    public static volatile XposedInterface module = null;

    public static void init(XposedInterface xposedInterface) {
        module = xposedInterface;
    }

    public static void log(String text) {
        XposedInterface m = module;
        if (m != null) {
            try {
                m.log(Log.INFO, TAG, text);
                return;
            } catch (Throwable ignored) {}
        }
        Log.i(TAG, text);
    }

    public static void log(Throwable t) {
        XposedInterface m = module;
        if (m != null) {
            try {
                m.log(Log.ERROR, TAG, t.getMessage() != null ? t.getMessage() : "Exception", t);
                return;
            } catch (Throwable ignored) {}
        }
        Log.e(TAG, "Exception in NothingTweaks", t);
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        if (!(hookMethod instanceof Executable)) {
            throw new IllegalArgumentException("Only Executable (Method or Constructor) can be hooked: " + hookMethod);
        }
        XposedInterface m = module;
        if (m == null) {
            throw new IllegalStateException("XposedBridge has not been initialized with XposedInterface yet");
        }
        XC_MethodHook.LibXposedHookerBridge bridge = new XC_MethodHook.LibXposedHookerBridge(callback);
        XposedInterface.HookHandle handle = m.hook((Executable) hookMethod)
                .setPriority(callback.priority)
                .intercept(bridge);
        return new XC_MethodHook.Unhook(handle);
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Method method : hookClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                unhooks.add(hookMethod(method, callback));
            }
        }
        return unhooks;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Constructor<?> constructor : hookClass.getDeclaredConstructors()) {
            unhooks.add(hookMethod(constructor, callback));
        }
        return unhooks;
    }
}
