package com.wp.redpacket.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.wp.redpacket.util.ConfigHelper;

/**
 * 设置界面
 *
 * 使用 PreferenceFragment 提供简洁的设置 UI
 * 支持 Android 4.x ~ 13+
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置 SharedPreferences 名称（与模块内部保持一致）
        SharedPreferences prefs = getSharedPreferences(
                ConfigHelper.getPrefsName(), MODE_WORLD_READABLE);
        PreferenceManager.setDefaultValues(this, ConfigHelper.getPrefsName(),
                MODE_WORLD_READABLE, R.xml.preferences, false);

        // 加载 PreferenceFragment
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // 设置使用的 SharedPreferences 文件名
            getPreferenceManager().setSharedPreferencesName(
                    ConfigHelper.getPrefsName());
            getPreferenceManager().setSharedPreferencesMode(
                    MODE_WORLD_READABLE);

            // 从 XML 加载设置项
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
