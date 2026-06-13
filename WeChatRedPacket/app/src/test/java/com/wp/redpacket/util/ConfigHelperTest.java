package com.wp.redpacket.util;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ConfigHelper 单元测试
 *
 * 测试配置读写、默认值、排除关键词匹配
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigHelperTest {

    @Mock
    private SharedPreferences prefs;

    @Mock
    private SharedPreferences.Editor editor;

    private ConfigHelper config;

    @Before
    public void setUp() {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        config = new ConfigHelper(prefs);
    }

    // ==================== 默认值测试 ====================

    @Test
    public void testDefaultEnabled() {
        when(prefs.getBoolean("enabled", true)).thenReturn(true);
        assertTrue("默认应启用", config.isEnabled());
    }

    @Test
    public void testDefaultDelayMin() {
        when(prefs.getInt("delay_min", 200)).thenReturn(200);
        assertEquals("默认最小延时 200ms", 200, config.getDelayMin());
    }

    @Test
    public void testDefaultDelayMax() {
        when(prefs.getInt("delay_max", 800)).thenReturn(800);
        assertEquals("默认最大延时 800ms", 800, config.getDelayMax());
    }

    @Test
    public void testDefaultWifiOnly() {
        when(prefs.getBoolean("wifi_only", false)).thenReturn(false);
        assertFalse("默认不限制 WiFi", config.isWifiOnly());
    }

    @Test
    public void testDefaultExcludeKeywords() {
        when(prefs.getString("exclude_keywords", "")).thenReturn("");
        assertEquals("默认无排除关键词", "", config.getExcludeKeywords());
    }

    // ==================== Setter 测试 ====================

    @Test
    public void testSetEnabled() {
        config.setEnabled(false);
        verify(editor).putBoolean("enabled", false);
        verify(editor).apply();
    }

    @Test
    public void testSetDelayMin() {
        config.setDelayMin(100);
        verify(editor).putInt("delay_min", 100);
    }

    @Test
    public void testSetDelayMax() {
        config.setDelayMax(1500);
        verify(editor).putInt("delay_max", 1500);
    }

    @Test
    public void testSetWifiOnly() {
        config.setWifiOnly(true);
        verify(editor).putBoolean("wifi_only", true);
    }

    @Test
    public void testSetExcludeKeywords() {
        config.setExcludeKeywords("工作,项目");
        verify(editor).putString("exclude_keywords", "工作,项目");
    }

    // ==================== 排除关键词匹配测试 ====================

    @Test
    public void testIsExcluded_NullName() {
        assertFalse("null 名称不应被排除", config.isExcluded(null));
    }

    @Test
    public void testIsExcluded_EmptyName() {
        assertFalse("空名称不应被排除", config.isExcluded(""));
    }

    @Test
    public void testIsExcluded_EmptyKeywords() {
        when(prefs.getString("exclude_keywords", "")).thenReturn("");
        assertFalse("空关键词不应排除任何群", config.isExcluded("工作群"));
    }

    @Test
    public void testIsExcluded_SingleMatch() {
        when(prefs.getString("exclude_keywords", "")).thenReturn("工作,通知");
        assertTrue("应排除包含'工作'的群", config.isExcluded("工作交流群"));
        assertTrue("应排除包含'通知'的群", config.isExcluded("通知群"));
    }

    @Test
    public void testIsExcluded_NoMatch() {
        when(prefs.getString("exclude_keywords", "")).thenReturn("工作,通知");
        assertFalse("不应排除'娱乐群'", config.isExcluded("娱乐群"));
        assertFalse("不应排除'朋友群'", config.isExcluded("朋友群"));
    }

    @Test
    public void testIsExcluded_WhitespaceKeywords() {
        when(prefs.getString("exclude_keywords", "")).thenReturn("  , 工作 ,  , 通知 , ");
        assertTrue("空白关键词应被忽略", config.isExcluded("工作群"));
    }

    // ==================== 随机延时测试 ====================

    @Test
    public void testGetRandomDelay_WithinRange() {
        when(prefs.getInt("delay_min", 200)).thenReturn(200);
        when(prefs.getInt("delay_max", 800)).thenReturn(800);

        // 运行 100 次验证范围
        for (int i = 0; i < 100; i++) {
            int delay = config.getRandomDelay();
            assertTrue("延时应 >= 200ms: " + delay, delay >= 200);
            assertTrue("延时应 <= 800ms: " + delay, delay <= 800);
        }
    }

    @Test
    public void testGetRandomDelay_MinEqualsMax() {
        when(prefs.getInt("delay_min", 200)).thenReturn(300);
        when(prefs.getInt("delay_max", 800)).thenReturn(300);

        int delay = config.getRandomDelay();
        assertEquals("min==max 时延时固定", 300, delay);
    }

    @Test
    public void testGetRandomDelay_MinGreaterThanMax() {
        // 异常情况：min > max 应修正
        when(prefs.getInt("delay_min", 200)).thenReturn(800);
        when(prefs.getInt("delay_max", 800)).thenReturn(200);

        int delay = config.getRandomDelay();
        assertEquals("min>max 应修正为固定值", 800, delay);
    }

    @Test
    public void testGetRandomDelay_NegativeMin() {
        when(prefs.getInt("delay_min", 200)).thenReturn(-100);
        when(prefs.getInt("delay_max", 800)).thenReturn(800);

        int delay = config.getRandomDelay();
        assertTrue("负 min 应修正为 >=0", delay >= 0);
    }
}
