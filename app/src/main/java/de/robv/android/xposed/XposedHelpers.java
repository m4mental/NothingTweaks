package de.robv.android.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class XposedHelpers {
    private static final WeakHashMap<Object, Map<String, Object>> additionalFields = new WeakHashMap<>();

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundError(e);
        }
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        try {
            return findClass(className, classLoader);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback) {
        List<Class<?>> list = new ArrayList<>();
        for (int i = 0; i < parameterTypesAndCallback.length - 1; i++) {
            Object item = parameterTypesAndCallback[i];
            if (item instanceof Class) {
                list.add((Class<?>) item);
            } else if (item instanceof String) {
                list.add(findClass((String) item, classLoader));
            } else {
                throw new IllegalArgumentException("Parameter type must be Class or String, got: " + item);
            }
        }
        return list.toArray(new Class<?>[0]);
    }

    private static Class<?>[] getParameterClassesOnly(ClassLoader classLoader, Object[] parameterTypes) {
        List<Class<?>> list = new ArrayList<>();
        for (Object item : parameterTypes) {
            if (item instanceof Class) {
                list.add((Class<?>) item);
            } else if (item instanceof String) {
                list.add(findClass((String) item, classLoader));
            } else {
                throw new IllegalArgumentException("Parameter type must be Class or String, got: " + item);
            }
        }
        return list.toArray(new Class<?>[0]);
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("No callback specified");
        }
        Object last = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        if (!(last instanceof XC_MethodHook)) {
            throw new IllegalArgumentException("Last parameter must be an instance of XC_MethodHook");
        }
        XC_MethodHook callback = (XC_MethodHook) last;
        Class<?>[] paramClasses = getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback);
        Method method = findMethodExact(clazz, methodName, (Object[]) paramClasses);
        return XposedBridge.hookMethod(method, callback);
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        Class<?> clazz = findClass(className, classLoader);
        return findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("No callback specified");
        }
        Object last = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        if (!(last instanceof XC_MethodHook)) {
            throw new IllegalArgumentException("Last parameter must be an instance of XC_MethodHook");
        }
        XC_MethodHook callback = (XC_MethodHook) last;
        Class<?>[] paramClasses = getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback);
        Constructor<?> constructor = findConstructorExact(clazz, (Object[]) paramClasses);
        return XposedBridge.hookMethod(constructor, callback);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        Class<?> clazz = findClass(className, classLoader);
        return findAndHookConstructor(clazz, parameterTypesAndCallback);
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
        Class<?>[] paramClasses = getParameterClassesOnly(clazz.getClassLoader(), parameterTypes);
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, paramClasses);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodError(clazz.getName() + "#" + methodName);
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
        try {
            return findMethodExact(clazz, methodName, parameterTypes);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
        Class<?>[] paramClasses = getParameterClassesOnly(clazz.getClassLoader(), parameterTypes);
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(paramClasses);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(clazz.getName() + "#<init>");
        }
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Object... parameterTypes) {
        try {
            return findConstructorExact(clazz, parameterTypes);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldError(clazz.getName() + "#" + fieldName);
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        Class<?> current = clazz;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().equals(type)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldError(clazz.getName() + " has no field of type " + type.getName());
    }

    public static Object getObjectField(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            return findField(obj.getClass(), fieldName).get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        if (obj == null) return;
        try {
            findField(obj.getClass(), fieldName).set(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getBoolean(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        try {
            findField(obj.getClass(), fieldName).setBoolean(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static int getIntField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getInt(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        try {
            findField(obj.getClass(), fieldName).setInt(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static long getLongField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getLong(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setLongField(Object obj, String fieldName, long value) {
        try {
            findField(obj.getClass(), fieldName).setLong(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static float getFloatField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getFloat(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setFloatField(Object obj, String fieldName, float value) {
        try {
            findField(obj.getClass(), fieldName).setFloat(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        try {
            findField(clazz, fieldName).set(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getBoolean(null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        try {
            findField(clazz, fieldName).setBoolean(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static int getStaticIntField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getInt(null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
        try {
            findField(clazz, fieldName).setInt(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getSurroundingThis(Object obj) {
        return getObjectField(obj, "this$0");
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        if (obj == null) return null;
        try {
            Method method = findMethodBestMatch(obj.getClass(), methodName, args);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try {
            Method method = findMethodBestMatch(clazz, methodName, args);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetError(e.getCause());
        }
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        int length = args != null ? args.length : 0;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == length) {
                try {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new NoSuchMethodError(clazz.getName() + " constructor matching argument count " + length);
    }

    private static Method findMethodBestMatch(Class<?> clazz, String methodName, Object[] args) {
        int length = args != null ? args.length : 0;
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterTypes().length == length) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodError(clazz.getName() + "#" + methodName);
    }

    public static synchronized Object getAdditionalInstanceField(Object obj, String key) {
        if (obj == null) return null;
        Map<String, Object> map = additionalFields.get(obj);
        return map != null ? map.get(key) : null;
    }

    public static synchronized Object setAdditionalInstanceField(Object obj, String key, Object value) {
        if (obj == null) return null;
        Map<String, Object> map = additionalFields.computeIfAbsent(obj, k -> new HashMap<>());
        return map.put(key, value);
    }

    public static synchronized Object removeAdditionalInstanceField(Object obj, String key) {
        if (obj == null) return null;
        Map<String, Object> map = additionalFields.get(obj);
        return map != null ? map.remove(key) : null;
    }

    public static class ClassNotFoundError extends Error {
        public ClassNotFoundError(Throwable cause) { super(cause); }
    }

    public static class InvocationTargetError extends Error {
        public InvocationTargetError(Throwable cause) { super(cause); }
    }
}
