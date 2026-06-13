package com.wp.redpacket.hook;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.wp.redpacket.util.ConfigHelper;
import com.wp.redpacket.util.XLog;

import java.util.ArrayList;
import java.util.List;

/**
 * View 树遍历 + 红包检测器
 *
 * 策略：不依赖微信混淆后的类名，通过以下特征识别红包相关 View：
 * 1. 文字内容匹配（"开"、"红包"、"领取"等）
 * 2. 颜色特征（红包红 #FA5151 及其近似色）
 * 3. View 类型推断（Button / TextView / 可点击 ViewGroup）
 */
public final class RedPacketDetector {

    // ── 红包关键词 ──
    private static final String[] RED_PACKET_KEYWORDS = {
            "红包", "微信红包", "发红包", "领取红包",
            "拼手气红包", "普通红包", "专属红包",
            "red packet", "lucky money"
    };

    // ── "开"按钮关键词（领取按钮）──
    private static final String[] OPEN_KEYWORDS = {
            "开", "開",
            "领取", "领取红包",
            "拆红包", "点我领取",
    };

    // ── 红包特征色 (微信红包红 #FA5151) ──
    private static final int RED_PACKET_COLOR     = 0xFFFA5151;
    private static final int COLOR_TOLERANCE      = 25;  // 色差容忍度

    // ── 上下文标记 ──
    private static final String TAG_SEEN = "wprp_seen";          // 已遍历标记
    private static final String TAG_DETECTED = "wprp_detected";  // 已检测标记

    private final ConfigHelper config;
    private final AutoClicker autoClicker;

    public RedPacketDetector(ConfigHelper config, AutoClicker autoClicker) {
        this.config = config;
        this.autoClicker = autoClicker;
    }

    // ==================== 入口 ====================

    /**
     * 分析当前 Activity 的 View 树，检测并自动点击红包
     *
     * @param activity 当前 Activity
     * @return 发现的目标数量
     */
    public int analyze(Activity activity) {
        if (activity == null) return 0;

        View rootView = activity.getWindow().getDecorView();
        if (rootView == null) return 0;

        String activityName = activity.getClass().getName();
        XLog.d("开始分析 Activity: " + activityName);

        // 收集所有目标
        List<Target> targets = new ArrayList<>();
        traverse(rootView, activityName, targets);

        if (targets.isEmpty()) {
            XLog.d("未发现红包目标: " + activityName);
            return 0;
        }

        XLog.i("发现 " + targets.size() + " 个红包目标: " + activityName);

        // 按优先级排序并点击
        int clicked = 0;
        for (Target t : targets) {
            if (autoClicker.performClick(t.view, t.uniqueId, t.description)) {
                clicked++;
            }
        }

        return clicked;
    }

    // ==================== View 树遍历 ====================

    private void traverse(View view, String activityName, List<Target> targets) {
        if (view == null) return;

        // 检查是否是目标
        Target target = detectTarget(view, activityName);
        if (target != null) {
            targets.add(target);
        }

        // 递归遍历子 View
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; i++) {
                traverse(vg.getChildAt(i), activityName, targets);
            }
        }
    }

    // ==================== 目标检测 ====================

    private static class Target {
        final View view;
        final String uniqueId;
        final String description;

        Target(View view, String uniqueId, String description) {
            this.view = view;
            this.uniqueId = uniqueId;
            this.description = description;
        }
    }

    private Target detectTarget(View view, String activityName) {
        // ---- 1) 检查是否是"开"按钮 ----
        if (view instanceof Button || view instanceof TextView) {
            String text = getViewText(view);
            if (isOpenButton(text)) {
                // 额外检查：背景颜色是否偏红
                if (hasRedBackground(view) || hasRedTint(view)) {
                    String uid = AutoClicker.makeUniqueId(activityName, text, view.hashCode());
                    return new Target(view, uid, "开按钮: " + text);
                }
                // 如果文字是"开"且 View 可点击，也视为目标
                if (isStrictOpenChar(text) && view.isClickable()) {
                    String uid = AutoClicker.makeUniqueId(activityName, text, view.hashCode());
                    return new Target(view, uid, "开按钮(文字): " + text);
                }
            }
        }

        // ---- 2) 检查是否是红包相关文字 ----
        String text = getViewText(view);
        if (containsRedPacketKeyword(text)) {
            if (view.isClickable() || isLikelyClickableContainer(view)) {
                String uid = AutoClicker.makeUniqueId(activityName, text, view.hashCode());
                return new Target(view, uid, "红包文案: " + text);
            }
        }

        // ---- 3) 检查红色背景的 ViewGroup（可能是红包气泡）----
        if (hasRedBackground(view) && view.isClickable()) {
            String uid = AutoClicker.makeUniqueId(activityName, text, view.hashCode());
            return new Target(view, uid, "红色可点击View: " + text);
        }

        // ---- 4) 检查包含"红包"文字的 ViewGroup 子节点 ----
        if (view instanceof ViewGroup && containsRedPacketKeyword(text) && isLikelyClickableContainer(view)) {
            String uid = AutoClicker.makeUniqueId(activityName, text, view.hashCode());
            return new Target(view, uid, "红包容器: " + text);
        }

        return null;
    }

    // ==================== 文字分析 ====================

    /**
     * 获取 View 上的文字
     * @VisibleForTesting
     */
    String getViewText(View view) {
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null) {
                String text = cs.toString().trim();
                if (!text.isEmpty()) return text;
            }
        }
        // ContentDescription 中包含的文字
        CharSequence cd = view.getContentDescription();
        if (cd != null) {
            String text = cd.toString().trim();
            if (!text.isEmpty()) return text;
        }
        return "";
    }

    /**
     * 是否包含"开"按钮关键词
     * @VisibleForTesting
     */
    boolean isOpenButton(String text) {
        if (text.isEmpty()) return false;
        for (String kw : OPEN_KEYWORDS) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /**
     * 是否严格等于"开"字
     * @VisibleForTesting
     */
    boolean isStrictOpenChar(String text) {
        return "开".equals(text) || "開".equals(text);
    }

    /**
     * 是否包含红包相关关键词
     * @VisibleForTesting
     */
    boolean containsRedPacketKeyword(String text) {
        if (text.isEmpty()) return false;
        for (String kw : RED_PACKET_KEYWORDS) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ==================== 颜色分析 ====================

    /**
     * 检查 View 是否有红包红色背景
     */
    private boolean hasRedBackground(View view) {
        Drawable bg = view.getBackground();
        if (bg instanceof ColorDrawable) {
            int color = ((ColorDrawable) bg).getColor();
            return isRedPacketColor(color);
        }
        return false;
    }

    /**
     * 检查 View 的染色是否为红色（ImageView tint 等）
     */
    private boolean hasRedTint(View view) {
        // 通过检查是否有红色为主的绘图缓存
        if (view.getSolidColor() == RED_PACKET_COLOR) {
            return true;
        }
        return false;
    }

    /**
     * 判断颜色是否是红包红色（含容忍度）
     * @VisibleForTesting
     */
    boolean isRedPacketColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int targetR = (RED_PACKET_COLOR >> 16) & 0xFF;
        int targetG = (RED_PACKET_COLOR >> 8) & 0xFF;
        int targetB = RED_PACKET_COLOR & 0xFF;

        int diff = Math.abs(r - targetR) + Math.abs(g - targetG) + Math.abs(b - targetB);
        return diff < COLOR_TOLERANCE;
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断 ViewGroup 是否可能是可点击的容器（如聊天气泡）
     */
    private boolean isLikelyClickableContainer(View view) {
        if (view.isClickable()) return true;
        // 检查是否有点击监听器
        if (view.hasOnClickListeners()) return true;
        // 检查子 View 是否包含可点击元素
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < Math.min(vg.getChildCount(), 5); i++) {
                View child = vg.getChildAt(i);
                if (child != null && (child.isClickable() || child.hasOnClickListeners())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断 Activity 是否是聊天界面（兜底判断）
     */
    public static boolean isLikelyChatActivity(String activityName) {
        if (activityName == null) return false;
        String lower = activityName.toLowerCase();
        // 常见聊天 Activity 关键词（未混淆时）
        return lower.contains("chat") || lower.contains("chatting")
                || lower.contains("conversation") || lower.contains("launcher");
    }
}
