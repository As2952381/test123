package com.wp.redpacket.util;

import android.content.SharedPreferences;

/**
 * SharedPreferences 配置读写封装
 * 管理模块所有可配置项
 */
public final class ConfigHelper {

    private static final String PREFS_NAME = "wechat_redpacket_config";

    // 配置 Key
    private static final String KEY_ENABLED     = "enabled";           // 总开关
    private static final String KEY_DELAY_MIN   = "delay_min";         // 最小延时(ms)
    private static final String KEY_DELAY_MAX   = "delay_max";         // 最大延时(ms)
    private static final String KEY_WIFI_ONLY   = "wifi_only";         // 仅 WiFi
    private static final String KEY_EXCLUDE_KEYWORDS = "exclude_keywords"; // 排除关键词(逗号分隔)

    // 默认值
    private static final boolean DEFAULT_ENABLED      = true;
    private static final int     DEFAULT_DELAY_MIN    = 200;   // 200ms
    private static final int     DEFAULT_DELAY_MAX    = 800;   // 800ms
    private static final boolean DEFAULT_WIFI_ONLY    = false;
    private static final String  DEFAULT_EXCLUDE      = "";

    private final SharedPreferences prefs;

    public ConfigHelper(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /**
     * 获取 SharedPreferences 文件名
     */
    public static String getPrefsName() {
        return PREFS_NAME;
    }

    // ---- Getter / Setter ----

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public int getDelayMin() {
        return prefs.getInt(KEY_DELAY_MIN, DEFAULT_DELAY_MIN);
    }

    public void setDelayMin(int delayMin) {
        prefs.edit().putInt(KEY_DELAY_MIN, delayMin).apply();
    }

    public int getDelayMax() {
        return prefs.getInt(KEY_DELAY_MAX, DEFAULT_DELAY_MAX);
    }

    public void setDelayMax(int delayMax) {
        prefs.edit().putInt(KEY_DELAY_MAX, delayMax).apply();
    }

    public boolean isWifiOnly() {
        return prefs.getBoolean(KEY_WIFI_ONLY, DEFAULT_WIFI_ONLY);
    }

    public void setWifiOnly(boolean wifiOnly) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply();
    }

    public String getExcludeKeywords() {
        return prefs.getString(KEY_EXCLUDE_KEYWORDS, DEFAULT_EXCLUDE);
    }

    public void setExcludeKeywords(String keywords) {
        prefs.edit().putString(KEY_EXCLUDE_KEYWORDS, keywords).apply();
    }

    /**
     * 检查群聊名称是否在排除列表中
     */
    public boolean isExcluded(String groupName) {
        if (groupName == null || groupName.isEmpty()) return false;
        String keywords = getExcludeKeywords();
        if (keywords.isEmpty()) return false;
        for (String kw : keywords.split(",")) {
            if (kw.trim().isEmpty()) continue;
            if (groupName.contains(kw.trim())) return true;
        }
        return false;
    }

    /**
     * 获取范围内的随机延时(ms)
     */
    public int getRandomDelay() {
        int min = getDelayMin();
        int max = getDelayMax();
        if (min < 0) min = 0;
        if (max < min) max = min;
        if (min == max) return min;
        return min + (int) (Math.random() * (max - min + 1));
    }
}
