package com.wp.redpacket;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.wp.redpacket.hook.ActivityHook;
import com.wp.redpacket.hook.AutoClicker;
import com.wp.redpacket.hook.RedPacketDetector;
import com.wp.redpacket.util.ConfigHelper;
import com.wp.redpacket.util.XLog;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed/LSPosed 模块入口
 *
 * 实现 IXposedHookLoadPackage，仅在微信进程内激活。
 *
 * 三层策略：
 *   第1层: Hook Activity.onCreate/onResume → View 树遍历检测红包
 *   第2层: Hook View.setOnClickListener → 捕获所有点击事件（可选增强）
 *   第3层: 后续反编译微信后补充精确类名 Hook（预留扩展）
 */
public class MainHook implements IXposedHookLoadPackage {

    // 微信包名
    private static final String WECHAT_PACKAGE = "com.tencent.mm";

    private ConfigHelper configHelper;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // 只处理微信
        if (!WECHAT_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        // processName 过滤：只在主进程激活
        if (lpparam.processName != null && !WECHAT_PACKAGE.equals(lpparam.processName)) {
            XLog.d("跳过非主进程: " + lpparam.processName);
            return;
        }

        XLog.i("========================================");
        XLog.i("微信红包助手模块已加载");
        XLog.i("微信进程: " + lpparam.processName);
        XLog.i("ClassLoader: " + lpparam.classLoader);
        XLog.i("========================================");

        try {
            initAndHook(lpparam);
        } catch (Exception e) {
            XLog.e("模块初始化失败", e);
        }
    }

    private void initAndHook(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam.classLoader;

        // ── 1) 初始化配置 ──
        // XSharedPreferences 可跨进程读取模块自身的 SharedPreferences
        XSharedPreferences xPrefs = new XSharedPreferences(
                BuildConfig.APPLICATION_ID,
                ConfigHelper.getPrefsName()
        );
        xPrefs.makeWorldReadable();
        configHelper = new ConfigHelper(xPrefs);

        // ── 2) 初始化组件 ──
        AutoClicker autoClicker = new AutoClicker(configHelper);
        RedPacketDetector detector = new RedPacketDetector(configHelper, autoClicker);

        // ── 3) 第1层: Activity 生命周期 Hook ──
        ActivityHook activityHook = new ActivityHook(configHelper, detector);
        activityHook.install(classLoader);

        // ── 4) 第2层: View OnClickListener Hook (可选增强) ──
        installClickListenerHook(classLoader);

        XLog.i("所有 Hook 安装完毕，等待红包...");
    }

    /**
     * 第2层策略：Hook View.setOnClickListener
     * 捕获所有按钮点击事件，记录日志便于分析微信的点击处理逻辑
     * 同时可以识别红包打开按钮的点击
     */
    private void installClickListenerHook(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.view.View",
                    classLoader,
                    "setOnClickListener",
                    "android.view.View$OnClickListener",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!configHelper.isEnabled()) return;

                            View view = (View) param.thisObject;
                            // 只记录可能相关的点击（减少日志噪音）
                            CharSequence text = null;
                            try {
                                text = (CharSequence) XposedHelpers.callMethod(view, "getText");
                            } catch (Exception ignored) {}

                            if (text != null) {
                                String textStr = text.toString().trim();
                                if (textStr.contains("开") || textStr.contains("红包")
                                        || textStr.contains("领取") || textStr.contains("拆")) {
                                    XLog.d("[第2层] 捕获点击注册: " + textStr
                                            + " | View: " + view.getClass().getName());
                                }
                            }
                        }
                    }
            );
            XLog.i("Hook View.setOnClickListener() → 成功（第2层）");
        } catch (Exception e) {
            XLog.w("Hook View.setOnClickListener() 失败（非致命）: " + e.getMessage());
        }
    }
}
