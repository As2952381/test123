package com.wp.redpacket.util;

import de.robv.android.xposed.XposedBridge;

/**
 * 日志封装类
 * 统一使用 XposedBridge.log 输出日志
 * 日志标签: [WeChatRedPacket]
 */
public final class XLog {

    private static final String TAG = "[WeChatRedPacket]";
    private static final boolean DEBUG = true;  // 发布时可设为 false

    private XLog() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    public static void d(String msg) {
        if (DEBUG) {
            XposedBridge.log(TAG + " [DEBUG] " + msg);
        }
    }

    public static void i(String msg) {
        XposedBridge.log(TAG + " [INFO] " + msg);
    }

    public static void w(String msg) {
        XposedBridge.log(TAG + " [WARN] " + msg);
    }

    public static void e(String msg) {
        XposedBridge.log(TAG + " [ERROR] " + msg);
    }

    public static void e(String msg, Throwable t) {
        XposedBridge.log(TAG + " [ERROR] " + msg);
        XposedBridge.log(TAG + " [ERROR] " + t.getMessage());
        if (DEBUG && t.getStackTrace() != null) {
            for (StackTraceElement ste : t.getStackTrace()) {
                XposedBridge.log(TAG + "   at " + ste.toString());
            }
        }
    }
}
