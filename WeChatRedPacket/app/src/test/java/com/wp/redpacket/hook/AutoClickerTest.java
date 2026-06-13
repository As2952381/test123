package com.wp.redpacket.hook;

import android.content.SharedPreferences;
import android.view.View;

import com.wp.redpacket.util.ConfigHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * AutoClicker 单元测试
 *
 * 测试防重复机制、延时范围、可选性检查
 */
@RunWith(MockitoJUnitRunner.class)
public class AutoClickerTest {

    @Mock
    private SharedPreferences prefs;

    @Mock
    private SharedPreferences.Editor editor;

    @Mock
    private View mockView;

    private ConfigHelper config;
    private AutoClicker autoClicker;

    @Before
    public void setUp() {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        // 设置延时为 0 方便同步测试
        when(prefs.getInt("delay_min", 200)).thenReturn(0);
        when(prefs.getInt("delay_max", 800)).thenReturn(0);

        config = new ConfigHelper(prefs);
        autoClicker = new AutoClicker(config);

        // View 默认可点击且已附加
        when(mockView.isAttachedToWindow()).thenReturn(true);
        when(mockView.isShown()).thenReturn(true);
        when(mockView.isClickable()).thenReturn(true);
        when(mockView.performClick()).thenReturn(true);

        // 每次测试前清空已处理记录
        AutoClicker.clearProcessed();
    }

    // ==================== 正常点击测试 ====================

    @Test
    public void testPerformClick_Success() {
        boolean result = autoClicker.performClick(mockView, "unique-1", "测试按钮");
        assertTrue("首次点击应成功", result);
        verify(mockView).performClick();
    }

    @Test
    public void testPerformClick_NullView() {
        boolean result = autoClicker.performClick(null, "id", "desc");
        assertFalse("null View 应返回 false", result);
    }

    @Test
    public void testPerformClick_NullUniqueId() {
        boolean result = autoClicker.performClick(mockView, null, "desc");
        assertFalse("null uniqueId 应返回 false", result);
    }

    // ==================== 防重复测试 ====================

    @Test
    public void testPerformClick_DuplicatePrevented() {
        // 第一次点击
        assertTrue(autoClicker.performClick(mockView, "dup-id", "描述1"));

        // 同 ID 第二次点击应被阻止
        boolean result = autoClicker.performClick(mockView, "dup-id", "描述2");
        assertFalse("重复 ID 应被阻止", result);
        // performClick 只应被调用一次
        verify(mockView, times(1)).performClick();
    }

    @Test
    public void testPerformClick_DifferentIdsAllowed() {
        when(mockView.isAttachedToWindow()).thenReturn(true);
        when(mockView.isShown()).thenReturn(true);
        when(mockView.isClickable()).thenReturn(true);

        assertTrue(autoClicker.performClick(mockView, "id-1", "按钮1"));
        assertTrue(autoClicker.performClick(mockView, "id-2", "按钮2"));
        verify(mockView, times(2)).performClick();
    }

    // ==================== makeUniqueId 测试 ====================

    @Test
    public void testMakeUniqueId() {
        String id1 = AutoClicker.makeUniqueId("ChatActivity", "开", 12345);
        String id2 = AutoClicker.makeUniqueId("ChatActivity", "开", 12345);
        String id3 = AutoClicker.makeUniqueId("ChatActivity", "开", 99999);

        assertEquals("相同参数应生成相同 ID", id1, id2);
        assertNotEquals("不同 hash 应生成不同 ID", id1, id3);
    }

    @Test
    public void testMakeUniqueId_NullText() {
        String id = AutoClicker.makeUniqueId("ChatActivity", null, 12345);
        assertNotNull("null 文字应生成有效 ID", id);
        assertTrue("ID 应包含 Activity 名", id.contains("ChatActivity"));
    }

    // ==================== clearProcessed 测试 ====================

    @Test
    public void testClearProcessed() {
        autoClicker.performClick(mockView, "id-clear", "测试");
        assertEquals("处理后应有 1 条记录", 1, AutoClicker.getProcessedCount());

        AutoClicker.clearProcessed();
        assertEquals("清空后应有 0 条记录", 0, AutoClicker.getProcessedCount());

        // 清空后同 ID 可以再次点击
        assertTrue(autoClicker.performClick(mockView, "id-clear", "测试2"));
    }

    // ==================== performClickImmediately 测试 ====================

    @Test
    public void testPerformClickImmediately() {
        autoClicker.performClickImmediately(mockView, "imm-id", "立即点击");
        verify(mockView).performClick();
    }

    @Test
    public void testPerformClickImmediately_DuplicatePrevented() {
        autoClicker.performClickImmediately(mockView, "imm-dup", "首次");
        autoClicker.performClickImmediately(mockView, "imm-dup", "重复");
        verify(mockView, times(1)).performClick();
    }

    // ==================== View 不可见测试 ====================

    @Test
    public void testPerformClick_ViewNotAttached() {
        when(mockView.isAttachedToWindow()).thenReturn(false);

        boolean result = autoClicker.performClick(mockView, "detached-id", "不可见");
        // 用延时0执行，View 未附加时不会调用 performClick
        assertTrue("仍会标记为处理", result);
        verify(mockView, never()).performClick();
    }

    @Test
    public void testPerformClick_ViewNotShown() {
        when(mockView.isShown()).thenReturn(false);

        autoClicker.performClick(mockView, "hidden-id", "隐藏");
        verify(mockView, never()).performClick();
    }
}
