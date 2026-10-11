-keep class com.souleven.nothingos.** { *; }
-dontwarn de.robv.android.xposed.**
-keep class de.robv.android.xposed.** { *; }

# libxposed rules
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule { public <init>(); }
