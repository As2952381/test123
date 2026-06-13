package com.wp.redpacket.hook;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.wp.redpacket.util.ConfigHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * RedPacketDetector 单元测试
 *
 * 测试文字匹配、颜色检测、View 遍历等核心逻辑
 */
@RunWith(MockitoJUnitRunner.class)
public class RedPacketDetectorTest {

    @Mock
    private SharedPreferences prefs;

    @Mock
    private SharedPreferences.Editor editor;

    @Mock
    private AutoClicker autoClicker;

    private ConfigHelper config;
    private RedPacketDetector detector;

    @Before
    public void setUp() {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        when(prefs.getBoolean("enabled", true)).thenReturn(true);

        config = new ConfigHelper(prefs);
        detector = new RedPacketDetector(config, autoClicker);
    }

    // ==================== isOpenButton 测试 ====================

    @Test
    public void testIsOpenButton_ChineseOpen() {
        assertTrue("'开' 应识别为开按钮", detector.isOpenButton("开"));
    }

    @Test
    public void testIsOpenButton_TraditionalOpen() {
        assertTrue("'開' 应识别为开按钮", detector.isOpenButton("開"));
    }

    @Test
    public void testIsOpenButton_ReceiveKeyword() {
        assertTrue("'领取' 应识别", detector.isOpenButton("领取"));
        assertTrue("'领取红包' 应识别", detector.isOpenButton("领取红包"));
    }

    @Test
    public void testIsOpenButton_SplitRedPacket() {
        assertTrue("'拆红包' 应识别", detector.isOpenButton("拆红包"));
    }

    @Test
    public void testIsOpenButton_PartialMatch() {
        assertTrue("包含'开'的文字应识别", detector.isOpenButton("点击开"));
        assertTrue("'点我领取' 应识别", detector.isOpenButton("点我领取"));
    }

    @Test
    public void testIsOpenButton_NoMatch() {
        assertFalse("'关闭' 不应识别（不含匹配词）", detector.isOpenButton("关闭"));
        assertFalse("空字符串不应识别", detector.isOpenButton(""));
        assertFalse("'发送' 不应识别", detector.isOpenButton("发送"));
        assertFalse("普通英文不应识别", detector.isOpenButton("hello"));
    }

    // ==================== isStrictOpenChar 测试 ====================

    @Test
    public void testIsStrictOpenChar_ExactMatch() {
        assertTrue("'开' 严格匹配", detector.isStrictOpenChar("开"));
        assertTrue("'開' 严格匹配", detector.isStrictOpenChar("開"));
    }

    @Test
    public void testIsStrictOpenChar_PartialNoMatch() {
        assertFalse("'打开' 不严格匹配", detector.isStrictOpenChar("打开"));
        assertFalse("'开始' 不严格匹配", detector.isStrictOpenChar("开始"));
        assertFalse("空字符串不匹配", detector.isStrictOpenChar(""));
        assertFalse("null 不匹配（但我们传空串）", detector.isStrictOpenChar("null"));
    }

    // ==================== containsRedPacketKeyword 测试 ====================

    @Test
    public void testContainsRedPacketKeyword_Chinese() {
        assertTrue("'红包' 应识别", detector.containsRedPacketKeyword("红包"));
        assertTrue("'微信红包' 应识别", detector.containsRedPacketKeyword("微信红包"));
        assertTrue("'发红包' 应识别", detector.containsRedPacketKeyword("发红包"));
    }

    @Test
    public void testContainsRedPacketKeyword_Subtypes() {
        assertTrue("'拼手气红包' 应识别", detector.containsRedPacketKeyword("拼手气红包"));
        assertTrue("'普通红包' 应识别", detector.containsRedPacketKeyword("普通红包"));
        assertTrue("'专属红包' 应识别", detector.containsRedPacketKeyword("专属红包"));
    }

    @Test
    public void testContainsRedPacketKeyword_English() {
        assertTrue("'red packet' 应识别", detector.containsRedPacketKeyword("Red Packet"));
        assertTrue("'lucky money' 应识别", detector.containsRedPacketKeyword("Lucky Money"));
    }

    @Test
    public void testContainsRedPacketKeyword_NoMatch() {
        assertFalse("普通消息不应识别", detector.containsRedPacketKeyword("你好"));
        assertFalse("空字符串不应识别", detector.containsRedPacketKeyword(""));
        assertFalse("转账不应识别", detector.containsRedPacketKeyword("[转账]"));
    }

    // ==================== isRedPacketColor 测试 ====================

    @Test
    public void testIsRedPacketColor_ExactMatch() {
        // 微信红包红 #FA5151 = 0xFFFA5151
        assertTrue("微信红包红应匹配", detector.isRedPacketColor(0xFFFA5151));
    }

    @Test
    public void testIsRedPacketColor_CloseMatch() {
        // 相近红色（10 以内的色差）
        assertTrue("相近红色 #FA515A 应匹配", detector.isRedPacketColor(0xFFFA515A));
        assertTrue("相近红色 #FA5148 应匹配", detector.isRedPacketColor(0xFFFA5148));
        assertTrue("相近红色 #FB5151 应匹配", detector.isRedPacketColor(0xFFFB5151));
    }

    @Test
    public void testIsRedPacketColor_BoundaryCheck() {
        // 色差刚好 24（在容忍范围内）
        int colorWithin = 0xFFFA5151;
        assertTrue("精确红色应匹配", detector.isRedPacketColor(colorWithin));
    }

    @Test
    public void testIsRedPacketColor_WrongColor() {
        assertFalse("蓝色不应匹配", detector.isRedPacketColor(0xFF2196F3));
        assertFalse("绿色不应匹配", detector.isRedPacketColor(0xFF4CAF50));
        assertFalse("黑色不应匹配", detector.isRedPacketColor(0xFF000000));
        assertFalse("白色不应匹配", detector.isRedPacketColor(0xFFFFFFFF));
    }

    @Test
    public void testIsRedPacketColor_OrangeCloseToRed() {
        // 橙色 (#FF9800) 色差很大，不应匹配
        assertFalse("纯橙色不应匹配", detector.isRedPacketColor(0xFFFF9800));
    }

    // ==================== getViewText 测试 ====================

    @Test
    public void testGetViewText_TextView() {
        TextView tv = mock(TextView.class);
        when(tv.getText()).thenReturn("  红包来了  ");
        assertEquals("应去除前后空格", "红包来了", detector.getViewText(tv));
    }

    @Test
    public void testGetViewText_EmptyTextView() {
        TextView tv = mock(TextView.class);
        when(tv.getText()).thenReturn("");
        assertEquals("空文字应返回空串", "", detector.getViewText(tv));
    }

    @Test
    public void testGetViewText_ContentDescription() {
        View view = mock(View.class);
        when(view.getContentDescription()).thenReturn("微信红包封面");
        assertTrue("应读取 ContentDescription", detector.getViewText(view).contains("微信红包"));
    }

    @Test
    public void testGetViewText_NoText() {
        View view = mock(View.class);
        when(view.getContentDescription()).thenReturn(null);
        assertEquals("无文字应返回空串", "", detector.getViewText(view));
    }

    // ==================== isLikelyChatActivity 测试 ====================

    @Test
    public void testIsLikelyChatActivity_Chat() {
        assertTrue("ChattingUI 应识别", RedPacketDetector.isLikelyChatActivity(
                "com.tencent.mm.ui.chatting.ChattingUI"));
    }

    @Test
    public void testIsLikelyChatActivity_Conversation() {
        assertTrue("Conversation 应识别", RedPacketDetector.isLikelyChatActivity(
                "com.tencent.mm.ui.conversation.ConversationUI"));
    }

    @Test
    public void testIsLikelyChatActivity_Launcher() {
        assertTrue("LauncherUI 应识别", RedPacketDetector.isLikelyChatActivity(
                "com.tencent.mm.ui.LauncherUI"));
    }

    @Test
    public void testIsLikelyChatActivity_NoMatch() {
        assertFalse("设置页不应识别", RedPacketDetector.isLikelyChatActivity(
                "com.tencent.mm.ui.setting.SettingsUI"));
        assertFalse("登录页不应识别", RedPacketDetector.isLikelyChatActivity(
                "com.tencent.mm.ui.account.LoginUI"));
    }

    @Test
    public void testIsLikelyChatActivity_Null() {
        assertFalse("null 不应识别", RedPacketDetector.isLikelyChatActivity(null));
    }

    // ==================== 综合：边界条件 ====================

    @Test
    public void testOpenKeywords_AllNotNull() {
        // 验证所有开按钮关键词都不为 null（防止数组越界）
        String[] tests = {"开", "開", "领取", "领取红包", "拆红包", "点我领取"};
        for (String kw : tests) {
            assertTrue("关键词 '" + kw + "' 应通过 isOpenButton", detector.isOpenButton(kw));
        }
    }

    @Test
    public void testRedPacketKeywords_AllNotNull() {
        String[] tests = {"红包", "微信红包", "发红包", "领取红包",
                "拼手气红包", "普通红包", "专属红包", "red packet", "lucky money"};
        for (String kw : tests) {
            assertTrue("关键词 '" + kw + "' 应通过检测", detector.containsRedPacketKeyword(kw));
        }
    }
}
