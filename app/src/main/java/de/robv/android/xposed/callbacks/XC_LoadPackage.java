package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;

public final class XC_LoadPackage {
    public static class LoadPackageParam {
        public String packageName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstPackage = true;

        public LoadPackageParam(String packageName, ClassLoader classLoader, ApplicationInfo appInfo, boolean isFirstPackage) {
            this.packageName = packageName;
            this.classLoader = classLoader;
            this.appInfo = appInfo;
            this.isFirstPackage = isFirstPackage;
        }
    }
}
