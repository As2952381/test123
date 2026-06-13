package com.wp.redpacket.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

/**
 * 网络状态检测工具类
 *
 * 用于判断当前网络类型（WiFi / 移动数据 / 无网络）
 * 兼容 Android 6.0 ~ 14+
 */
public final class NetworkUtil {

    private NetworkUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断当前是否连接了 WiFi
     *
     * @param context 上下文
     * @return true 如果当前是 WiFi 连接
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            XLog.w("NetworkUtil: Context 为空，默认允许运行");
            return true; // 无法判断时默认放行
        }

        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                XLog.w("NetworkUtil: ConnectivityManager 为空，默认允许运行");
                return true;
            }

            // Android 6.0+ (API 23+) 使用 NetworkCapabilities
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = cm.getActiveNetwork();
                if (network == null) {
                    XLog.d("NetworkUtil: 无活跃网络连接");
                    return false;
                }

                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps == null) {
                    XLog.d("NetworkUtil: 无法获取网络能力信息");
                    return false;
                }

                // 检查是否有 WiFi 传输能力
                boolean hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);

                // 额外检查：某些设备上 WiFi 可能走以太网
                if (!hasWifi && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 考虑以太网为类 WiFi
                    hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                }

                XLog.d("NetworkUtil: WiFi=" + hasWifi);
                return hasWifi;

            } else {
                // Android 5.x 及以下（已淘汰，但保留兼容）
                @SuppressWarnings("deprecation")
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork == null || !activeNetwork.isConnected()) {
                    return false;
                }
                boolean isWifi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                if (!isWifi) {
                    // 以太网也算类 WiFi
                    isWifi = activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET;
                }
                return isWifi;
            }

        } catch (SecurityException e) {
            XLog.w("NetworkUtil: 缺少网络权限，默认允许运行: " + e.getMessage());
            return true;
        } catch (Exception e) {
            XLog.e("NetworkUtil: 网络检测异常，默认允许运行", e);
            return true;
        }
    }

    /**
     * 判断是否应该执行红包检测（根据 WiFi Only 配置）
     *
     * @param context    上下文
     * @param config     配置助手
     * @return true 如果可以执行检测
     */
    public static boolean shouldProceed(Context context, ConfigHelper config) {
        if (!config.isWifiOnly()) {
            // 未开启"仅 WiFi"模式，总是允许
            return true;
        }

        // 开启了"仅 WiFi"模式，检查当前网络
        boolean wifiConnected = isWifiConnected(context);
        if (!wifiConnected) {
            XLog.d("NetworkUtil: 当前非 WiFi 网络，跳过红包检测");
        }
        return wifiConnected;
    }

    /**
     * 获取当前连接的 WiFi SSID（用于日志，非必需）
     */
    public static String getWifiSSID(Context context) {
        if (context == null) return "unknown";

        try {
            WifiManager wm = (WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return "unknown";

            WifiInfo info = wm.getConnectionInfo();
            if (info == null) return "unknown";

            String ssid = info.getSSID();
            // 去掉引号
            if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            return ssid != null ? ssid : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
