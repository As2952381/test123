# ============================================
# 微信红包助手 ProGuard 混淆规则
# ============================================

# ── 保持 Xposed 入口类 ──
-keep class com.wp.redpacket.MainHook { *; }
-keep class com.wp.redpacket.MainHook

# ── 保持 Xposed 相关 ──
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# ── 保持模块自身代码（不被混淆）──
# 因为 Xposed 模块通过 xposed_init 中的类名加载
-keep class com.wp.redpacket.** { *; }
-keepclassmembers class com.wp.redpacket.** { *; }

# ── 保持 SettingsActivity（Launcher 入口）──
-keep class com.wp.redpacket.ui.SettingsActivity { *; }

# ── 保持 Android 原生 API ──
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ── 保持 SharedPreferences 相关 ──
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── 优化设置 ──
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ── 去掉日志（发布版）──
-assumenosideeffects class com.wp.redpacket.util.XLog {
    public static void d(...);
}

# ── 保持注解 ──
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ── 微信相关（不混淆对微信的反射调用）──
-dontwarn com.tencent.mm.**
-keep class com.tencent.mm.** { *; }
