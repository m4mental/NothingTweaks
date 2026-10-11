package de.robv.android.xposed;

import android.content.SharedPreferences;
import io.github.libxposed.api.XposedInterface;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class XSharedPreferences implements SharedPreferences {
    private final String packageName;
    private final String prefFileName;
    private SharedPreferences delegate;

    public XSharedPreferences(String packageName, String prefFileName) {
        this.packageName = packageName;
        this.prefFileName = prefFileName != null ? prefFileName : "settings";
        reload();
    }

    public XSharedPreferences(String packageName) {
        this(packageName, "settings");
    }

    public boolean makeWorldReadable() {
        return true;
    }

    public void reload() {
        try {
            XposedInterface m = XposedBridge.module;
            if (m != null) {
                delegate = m.getRemotePreferences(prefFileName);
            }
        } catch (Throwable ignored) {}
    }

    public File getFile() {
        return new File("/data/user_de/0/" + packageName + "/shared_prefs/" + prefFileName + ".xml");
    }

    @Override
    public Map<String, ?> getAll() {
        return delegate != null ? delegate.getAll() : Collections.emptyMap();
    }

    @Override
    public String getString(String key, String defValue) {
        return delegate != null ? delegate.getString(key, defValue) : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return delegate != null ? delegate.getStringSet(key, defValues) : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        return delegate != null ? delegate.getInt(key, defValue) : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        return delegate != null ? delegate.getLong(key, defValue) : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        return delegate != null ? delegate.getFloat(key, defValue) : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return delegate != null ? delegate.getBoolean(key, defValue) : defValue;
    }

    @Override
    public boolean contains(String key) {
        return delegate != null && delegate.contains(key);
    }

    @Override
    public Editor edit() {
        return delegate != null ? delegate.edit() : null;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (delegate != null) delegate.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (delegate != null) delegate.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
