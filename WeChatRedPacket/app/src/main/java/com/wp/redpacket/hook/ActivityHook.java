package com.wp.redpacket.hook;

import android.app.Activity;
import android.os.Bundle;

import com.wp.redpacket.util.ConfigHelper;
import com.wp.redpacket.util.NetworkUtil;
import com.wp.redpacket.util.XLog;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 通用 Activity 生命周期 Hook
 *
 * 第1层策略：Hook Activity.onCreate() → 每当打开任何 Activity 就检测红包相关 View
 * 这是最通用的方法，不依赖微信的任何混淆类名。
 */
public final class ActivityHook {

    private final ConfigHelper config;
    private final RedPacketDetector detector;

    public ActivityHook(ConfigHelper config, RedPacketDetector detector) {
        this.config = config;
        this.detector = detector;
    }

    /**
     * 安装 Hook
     * Hook Activity.onCreate() 和 Activity.onResume()
     *
     * @param classLoader 微信的 ClassLoader
     */
    public void install(ClassLoader classLoader) {
        XLog.i("开始安装 Activity 生命周期 Hook...");

        // ── Hook Activity.onCreate(Bundle) ──
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            if (activity == null) return;

                            // 检查模块是否启用
                            if (!config.isEnabled()) {
                                XLog.d("模块已禁用，跳过: " + activity.getClass().getName());
                                return;
                            }

                            // 检查网络条件（仅 WiFi 模式）
                            if (!NetworkUtil.shouldProceed(activity, config)) {
                                XLog.d("网络条件不满足，跳过: " + activity.getClass().getName());
                                return;
                            }

                            String activityName = activity.getClass().getName();
                            XLog.d("Activity onCreate: " + activityName);

                            // 延时 300ms 后检测（等 View 渲染完毕）
                            if (activity.getWindow() != null
                                    && activity.getWindow().getDecorView() != null) {
                                activity.getWindow().getDecorView().postDelayed(() -> {
                                    try {
                                        int found = detector.analyze(activity);
                                        if (found > 0) {
                                            XLog.i("onCreate 检测完成: " + activityName
                                                    + " | 发现 " + found + " 个目标");
                                        }
                                    } catch (Exception e) {
                                        XLog.e("onCreate 检测异常: " + activityName, e);
                                    }
                                }, 300);
                            }
                        }
                    }
            );
            XLog.i("Hook Activity.onCreate() → 成功");
        } catch (Exception e) {
            XLog.e("Hook Activity.onCreate() 失败", e);
        }

        // ── Hook Activity.onResume() ──
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            if (activity == null) return;

                            if (!config.isEnabled()) return;

                            // 检查网络条件（仅 WiFi 模式）
                            if (!NetworkUtil.shouldProceed(activity, config)) {
                                XLog.d("网络条件不满足，跳过 onResume: " + activity.getClass().getName());
                                return;
                            }

                            String activityName = activity.getClass().getName();
                            XLog.d("Activity onResume: " + activityName);

                            // onResume 时重新检测（可能从红包详情页回来）
                            if (activity.getWindow() != null
                                    && activity.getWindow().getDecorView() != null) {
                                activity.getWindow().getDecorView().postDelayed(() -> {
                                    try {
                                        int found = detector.analyze(activity);
                                        if (found > 0) {
                                            XLog.i("onResume 检测完成: " + activityName
                                                    + " | 发现 " + found + " 个目标");
                                        }
                                    } catch (Exception e) {
                                        XLog.e("onResume 检测异常: " + activityName, e);
                                    }
                                }, 500);
                            }
                        }
                    }
            );
            XLog.i("Hook Activity.onResume() → 成功");
        } catch (Exception e) {
            XLog.e("Hook Activity.onResume() 失败", e);
        }

        XLog.i("Activity 生命周期 Hook 安装完成");
    }
}
