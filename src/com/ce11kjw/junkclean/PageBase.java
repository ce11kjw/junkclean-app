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
                        ui.post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                ScanEngine.invalidate();
                                pool.removeAll(sel);
                                rebuild(pool, box, sum);
                                String msg = (toTrash ? "已移入回收站 " : "已删除 ") + r.count + " 项 · "
                                        + Util.fmtSize(toTrash ? r.trashed : r.freed)
                                        + (toTrash ? "（清空后才释放）" : "");
                                if (!r.errors.isEmpty()) msg += " · " + r.errors.size() + " 项失败";
                                act.toast(msg);
                                act.homePage().refreshDisk();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    protected void rebuild(final List<JunkItem> pool, final LinearLayout box, final TextView sum) {
        box.removeAllViews();
        if (pool.isEmpty()) {
            box.addView(UI.empty(act, "列表已清空"));
            sum.setText("");
            return;
        }
        Runnable onChange = new Runnable() {
            public void run() { updateSum(pool, sum); }
        };
        for (JunkItem it : pool) box.addView(UI.fileRow(act, it, onChange, whitelistAction(it)));
        updateSum(pool, sum);
    }
}
