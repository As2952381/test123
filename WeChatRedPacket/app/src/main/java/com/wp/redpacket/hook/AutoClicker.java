package com.wp.redpacket.hook;

import android.view.View;

import com.wp.redpacket.util.ConfigHelper;
import com.wp.redpacket.util.XLog;

import java.util.HashSet;
import java.util.Set;

/**
 * 自动点击执行器
 * 负责延时、模拟点击、防重复处理
 */
public final class AutoClicker {

    // 已处理的消息/红包 ID 集合（防重复，微信进程存活期间有效）
    private static final Set<String> processedIds = new HashSet<>();
    // 上次点击时间戳（防止过于频繁操作）
    private static long lastClickTime = 0;
    // 两次点击最小间隔 (ms)
    private static final long MIN_CLICK_INTERVAL = 500;

    private final ConfigHelper config;

    public AutoClicker(ConfigHelper config) {
        this.config = config;
    }

    /**
     * 对目标 View 执行点击操作
     *
     * @param view       目标 View
     * @param uniqueId   唯一标识（用于防重复）
     * @param description 操作描述（用于日志）
     * @return true 如果执行了点击，false 如果被跳过
     */
    public boolean performClick(View view, String uniqueId, String description) {
        if (view == null || uniqueId == null) return false;

        // 1. 检查是否已处理过
        if (processedIds.contains(uniqueId)) {
            XLog.d("跳过已处理: " + description + " | ID: " + uniqueId);
            return false;
        }

        // 2. 检查点击间隔
        long now = System.currentTimeMillis();
        if (now - lastClickTime < MIN_CLICK_INTERVAL) {
            XLog.d("点击过于频繁，跳过: " + description);
            return false;
        }

        // 3. 计算延时
        int delay = config.getRandomDelay();
        XLog.i("发现目标: " + description + " | 延时: " + delay + "ms");

        // 4. 标记已处理
        processedIds.add(uniqueId);

        // 5. 延时后执行点击
        if (delay > 0) {
            view.postDelayed(() -> executeClick(view, uniqueId, description), delay);
        } else {
            // 延时为 0 时直接在 UI 线程执行
            if (view.isAttachedToWindow()) {
                executeClick(view, uniqueId, description);
            }
        }

        lastClickTime = now;
        return true;
    }

    /**
     * 立即执行点击（无延时）
     */
    public void performClickImmediately(View view, String uniqueId, String description) {
        if (view == null || uniqueId == null) return;
        if (processedIds.contains(uniqueId)) return;

        processedIds.add(uniqueId);
        if (view.isAttachedToWindow()) {
            executeClick(view, uniqueId, description);
        }
    }

    private void executeClick(View view, String uniqueId, String description) {
        try {
            if (view.isAttachedToWindow() && view.isShown() && view.isClickable()) {
                boolean result = view.performClick();
                XLog.i((result ? "点击成功" : "点击失败") + ": " + description + " | ID: " + uniqueId);
            } else {
                // 对于不可点击的 View（如 RelativeLayout 红包气泡），模拟触摸事件
                if (view.isAttachedToWindow() && view.isShown()) {
                    boolean result = view.performClick();
                    XLog.i((result ? "模拟点击成功" : "模拟点击失败") + ": " + description + " | ID: " + uniqueId);
                } else {
                    XLog.d("View 不可见或未附加，跳过点击: " + description);
                }
            }
        } catch (Exception e) {
            XLog.e("点击执行异常: " + description, e);
        }
    }

    /**
     * 生成唯一标识
     */
    public static String makeUniqueId(String activityClass, String viewText, int viewHash) {
        return activityClass + "|" + (viewText != null ? viewText : "") + "|" + viewHash;
    }

    /**
     * 清空已处理记录
     */
    public static void clearProcessed() {
        processedIds.clear();
        XLog.d("已处理记录已清空");
    }

    /**
     * 获取已处理数量
     */
    public static int getProcessedCount() {
        return processedIds.size();
    }
}
