package com.ce11kjw.junkclean;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 页面公共逻辑：列表渲染、勾选、删除、白名单 */
public abstract class PageBase {

    protected final MainActivity act;
    protected final Handler ui = new Handler(Looper.getMainLooper());

    protected PageBase(MainActivity a) { this.act = a; }

    /**
     * 后台任务回调前的存活检查。主题或壁纸切换会重建整个界面，
     * 此时旧页面的线程仍在跑，直接操作 View 会更新到已废弃的实例上。
     */
    protected boolean alive() {
        return !act.isFinishing() && !act.isDestroyed();
    }

    /** 只在页面仍存活时切回主线程执行 */
    protected void post(final Runnable r) {
        ui.post(new Runnable() {
            public void run() { if (alive()) r.run(); }
        });
    }

    public abstract View view();

    protected String scanRoot() {
        String r = act.store.scanRoot();
        return r == null || r.trim().isEmpty() ? Util.sdRoot() : r.trim();
    }

    protected List<String> wl() { return act.store.whitelist(); }

    /** 长按加入白名单 */
    protected Runnable whitelistAction(final JunkItem it) {
        return new Runnable() {
            public void run() {
                String key = new File(it.path).getName();
                act.store.addWhitelist(key);
                it.checked = false;
                ScanEngine.invalidate();
                act.toast("已加入白名单：" + key);
            }
        };
    }

    protected void selectAll(List<JunkItem> pool, LinearLayout box, boolean on) {
        for (JunkItem it : pool) it.checked = on;
        for (int i = 0; i < box.getChildCount(); i++) {
            View v = box.getChildAt(i);
            if (!(v instanceof LinearLayout)) continue;
            View f = ((LinearLayout) v).getChildAt(0);
            if (f instanceof CheckBox) ((CheckBox) f).setChecked(on);
        }
    }

    protected void updateSum(List<JunkItem> pool, TextView sum) {
        long sel = 0;
        int n = 0;
        for (JunkItem it : pool) if (it.checked) { sel += it.size; n++; }
        sum.setText(n == 0 ? pool.size() + " 项"
                : "已选 " + n + " / " + pool.size() + " 项 · " + Util.fmtSize(sel));
    }

    /** 删除选中项；toTrash 决定是否走回收站 */
    protected void removeItems(final List<JunkItem> pool, final LinearLayout box,
                               final TextView sum, final boolean toTrash) {
        final List<JunkItem> sel = new ArrayList<JunkItem>();
        for (JunkItem it : pool) if (it.checked) sel.add(it);
        if (sel.isEmpty()) { act.toast("未选中项目"); return; }
        long total = 0;
        for (JunkItem it : sel) total += it.size;
        final long ft = total;
        UI.confirm(act, toTrash ? "移入回收站" : "彻底删除",
                (toTrash ? "将移入回收站 " : "将永久删除 ") + sel.size() + " 项，约 "
                + Util.fmtSize(ft) + (toTrash ? "\n可从回收站恢复。" : "\n无法恢复！"),
                new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        final CleanEngine.Result r = new CleanEngine(toTrash).cleanItems(sel);
                        post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                ScanEngine.invalidate();
                                pool.removeAll(sel);
                                rebuild(pool, box, sum);
                                String msg = (toTrash ? "已移入回收站 " : "已删除 ") + r.count + " 项 · "
                                        + Util.fmtSize(toTrash ? r.trashed : r.freed)
                                        + (toTrash ? "（清空后才释放）" : "");
                                act.toast(msg);
                                if (!r.errors.isEmpty()) showErrors(r.errors);
                                act.homePage().refreshDisk();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    /** 展示删除失败清单，说明原因与可行做法 */
    protected void showErrors(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下项目未能删除：\n\n");
        int n = Math.min(errors.size(), 30);
        for (int i = 0; i < n; i++) sb.append("· ").append(errors.get(i)).append('\n');
        if (errors.size() > n) sb.append("\n… 共 ").append(errors.size()).append(" 项");
        sb.append("\n\n常见原因：\n");
        sb.append("· Android/data 与 obb 在 Android 11+ 受系统限制，需 root\n");
        sb.append("· 文件正被其他应用占用\n");
        sb.append("· 位于只读分区或缺少写权限");
        if (!Shell.hasRoot()) sb.append("\n\n授予 root 后可解除大部分限制。");
        UI.info(act, "删除失败 " + errors.size() + " 项", sb.toString());
    }

    /**
     * 有 root 时提供重试选项，否则只展示原因。
     * Android/data 受限是最常见的失败原因，root 能解决绝大部分。
     */
    protected void offerRetry(final List<String> errors, final Runnable after) {
        if (!Shell.hasRoot()) { showErrors(errors); return; }
        StringBuilder sb = new StringBuilder();
        sb.append(errors.size()).append(" 项未能删除：\n\n");
        int n = Math.min(errors.size(), 20);
        for (int i = 0; i < n; i++) sb.append("· ").append(errors.get(i)).append('\n');
        if (errors.size() > n) sb.append("\n… 共 ").append(errors.size()).append(" 项");
        sb.append("\n\n已检测到 root，可用 root 权限强制删除。");
        UI.confirm(act, "删除失败", sb.toString(), new Runnable() {
            public void run() { retryWithRoot(errors, after); }
        });
    }

    /** 失败项用 root 重试。路径从错误文案里取「：」之后的部分 */
    protected void retryWithRoot(final List<String> errors, final Runnable after) {
        if (!Shell.hasRoot()) { act.toast("需要 root 才能重试"); return; }
        new Thread(new Runnable() {
            public void run() {
                StringBuilder batch = new StringBuilder();
                for (String e : errors) {
                    int i = e.indexOf('：');
                    String p = i >= 0 ? e.substring(i + 1).trim() : e.trim();
                    if (p.startsWith("…")) p = Util.sdRoot() + p.substring(1);
                    if (!CleanEngine.isSafe(p)) continue;
                    batch.append("rm -rf ").append(Shell.quote(p)).append('\n');
                }
                if (batch.length() > 0) Shell.exec(true, batch.toString());
                post(new Runnable() {
                    public void run() {
                        act.toast("已用 root 重试 " + errors.size() + " 项");
                        ScanEngine.invalidate();
                        if (after != null) after.run();
                    }
                });
            }
        }).start();
    }

    /** 反选：已勾的取消，未勾的勾上 */
    protected void invertAll(List<JunkItem> pool, LinearLayout box) {
        for (JunkItem it : pool) it.checked = !it.checked;
        syncChecks(pool, box);
    }

    /** 只勾体积最大的 N 项，其余取消 */
    protected void selectTopN(List<JunkItem> pool, LinearLayout box, int n) {
        List<JunkItem> sorted = new ArrayList<JunkItem>(pool);
        java.util.Collections.sort(sorted, new java.util.Comparator<JunkItem>() {
            public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
        });
        for (JunkItem it : pool) it.checked = false;
        for (int i = 0; i < Math.min(n, sorted.size()); i++) sorted.get(i).checked = true;
        syncChecks(pool, box);
    }

    /** 把数据层的勾选状态刷到已渲染的行上 */
    private void syncChecks(List<JunkItem> pool, LinearLayout box) {
        int idx = 0;
        for (int i = 0; i < box.getChildCount() && idx < pool.size(); i++) {
            View v = box.getChildAt(i);
            if (!(v instanceof LinearLayout)) continue;
            View f = ((LinearLayout) v).getChildAt(0);
            if (f instanceof CheckBox) {
                ((CheckBox) f).setChecked(pool.get(idx).checked);
                idx++;
            }
        }
    }

    /**
     * 分批渲染。列表可能上百项，一次 addView 会明显卡顿；
     * 首屏只建 24 行，其余分帧追加。
     */
    protected void renderBatched(final List<JunkItem> pool, final LinearLayout box,
                                 final TextView sum, final int from) {
        final int BATCH = 24;
        int end = Math.min(from + BATCH, pool.size());
        Runnable onChange = new Runnable() {
            public void run() { updateSum(pool, sum); }
        };
        for (int i = from; i < end; i++) {
            JunkItem it = pool.get(i);
            box.addView(UI.fileRow(act, it, onChange, whitelistAction(it)));
        }
        if (end < pool.size()) {
            box.post(new Runnable() {
                public void run() {
                    if (alive()) renderBatched(pool, box, sum, end);
                }
            });
        }
    }

    protected void rebuild(final List<JunkItem> pool, final LinearLayout box, final TextView sum) {
        box.removeAllViews();
        if (pool.isEmpty()) {
            box.addView(UI.empty(act, "列表已清空"));
            sum.setText("");
            return;
        }
        renderBatched(pool, box, sum, 0);
        updateSum(pool, sum);
    }
}
