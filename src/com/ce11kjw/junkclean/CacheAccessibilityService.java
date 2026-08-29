package com.ce11kjw.junkclean;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * 无 root 自动清缓存服务（Apex 思路）。
 *
 * 工作方式：宿主把要清理的包名队列写入静态字段并逐个 startActivity 打开
 * 「应用信息」页，本服务监听窗口内容，自动找到「存储」→「清除缓存」按钮并点击。
 *
 * 说明：Android 无 root 时无法直接删除其它应用缓存，
 * 这是官方允许的唯一自动化途径（用户需手动授予无障碍权限）。
 */
public class CacheAccessibilityService extends AccessibilityService {

    /** 待处理包名队列（宿主填充） */
    public static final java.util.ArrayDeque<String> QUEUE = new java.util.ArrayDeque<String>();
    /** 是否正在自动清理 */
    public static volatile boolean running = false;
    /** 已处理计数（供 UI 显示） */
    public static volatile int done = 0;
    /** 服务是否已连接 */
    public static volatile boolean connected = false;

    private static CacheAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 各语言「存储」「清除缓存」按钮文案
    private static final String[] STORAGE_LABELS = {
            "存储", "存储空间", "存储和缓存", "Storage", "Storage & cache", "Storage and cache"
    };
    private static final String[] CLEAR_CACHE_LABELS = {
            "清除缓存", "清理缓存", "清空缓存", "Clear cache", "Clear Cache", "CLEAR CACHE"
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        connected = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connected = false;
        instance = null;
    }

    /** 是否已开启无障碍权限 */
    public static boolean isEnabled() {
        return connected && instance != null;
    }

    /** 启动批量自动清理：宿主调用 */
    public static void startBatch(android.content.Context ctx, List<String> packages) {
        QUEUE.clear();
        QUEUE.addAll(packages);
        done = 0;
        running = true;
        openNext(ctx);
    }

    /** 打开队列里下一个应用信息页 */
    private static void openNext(android.content.Context ctx) {
        String pkg = QUEUE.poll();
        if (pkg == null) {
            running = false;
            return;
        }
        Intent it = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + pkg));
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(it);
        } catch (Exception ignored) {
            done++;
            openNext(ctx);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!running) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // 先找「清除缓存」按钮，找到直接点
        AccessibilityNodeInfo clearBtn = findByLabels(root, CLEAR_CACHE_LABELS);
        if (clearBtn != null) {
            boolean clicked = clickNodeOrParent(clearBtn);
            if (clicked) {
                // 点完等一下，进入下一个
                handler.postDelayed(new Runnable() {
                    public void run() {
                        done++;
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                performGlobalAction(GLOBAL_ACTION_BACK);
                                openNext(CacheAccessibilityService.this);
                            }
                        }, 400);
                    }
                }, 500);
            }
            return;
        }

        // 没有「清除缓存」，可能在应用信息页，先点「存储」
        AccessibilityNodeInfo storageBtn = findByLabels(root, STORAGE_LABELS);
        if (storageBtn != null) {
            clickNodeOrParent(storageBtn);
        }
    }

    /** 在节点树里按文案模糊匹配 */
    private AccessibilityNodeInfo findByLabels(AccessibilityNodeInfo root, String[] labels) {
        for (String label : labels) {
            List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByText(label);
            if (found != null) {
                for (AccessibilityNodeInfo n : found) {
                    if (n == null) continue;
                    CharSequence t = n.getText();
                    if (t != null && t.toString().trim().equalsIgnoreCase(label)) return n;
                    // 部分 ROM 文案在子节点，返回第一个可点祖先
                    if (t != null && t.toString().contains(label)) return n;
                }
                if (!found.isEmpty()) return found.get(0);
            }
        }
        return null;
    }

    /** 点击节点本身或往上找可点击的祖先 */
    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = node;
        int hops = 0;
        while (cur != null && hops < 6) {
            if (cur.isClickable() && cur.isEnabled()) {
                return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            cur = cur.getParent();
            hops++;
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        running = false;
    }
}
