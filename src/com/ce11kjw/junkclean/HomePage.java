package com.ce11kjw.junkclean;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 首页：存储条 + 扫描 + 分类列表 + 清理 */
public class HomePage {

    private final MainActivity act;
    private ScrollView scroll;
    private LinearLayout catBox;
    private StorageBarView bar;
    private TextView diskText, rootBadge, statBadge, scanState;
    private Button scanBtn, cleanBtn, allBtn;
    private List<JunkCategory> cats = new ArrayList<JunkCategory>();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean scanning;

    public HomePage(MainActivity a) { this.act = a; }

    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        // 顶部标题行
        LinearLayout head = UI.row(act);
        TextView t = UI.text(act, "JunkClean", 22, Theme.TEXT);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(t);
        rootBadge = UI.badge(act, Shell.hasRoot() ? "root" : "无 root",
                Shell.hasRoot() ? Theme.ACCENT : Theme.WARN,
                Shell.hasRoot() ? 0x222DD4A7 : 0x22FBBF24);
        LinearLayout.LayoutParams bp = UI.lp(UI.WC, UI.WC);
        bp.leftMargin = Theme.dp(act, 8);
        head.addView(rootBadge, bp);
        root.addView(head);

        // 存储卡片
        LinearLayout diskCard = UI.card(act);
        diskCard.addView(UI.h2(act, "存储空间"));
        bar = new StorageBarView(act);
        diskCard.addView(bar, UI.lpm(act, UI.MP, UI.WC, 10));
        diskText = UI.note(act, "读取中…");
        diskCard.addView(diskText, UI.lpm(act, UI.MP, UI.WC, 8));
        root.addView(diskCard, UI.lpm(act, UI.MP, UI.WC, 12));

        // 扫描按钮
        scanBtn = UI.primary(act, "🔍  开始扫描");
        scanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startScan(); }
        });
        LinearLayout.LayoutParams sp = UI.lpm(act, UI.MP, Theme.dp(act, 52), 14);
        root.addView(scanBtn, sp);

        statBadge = UI.note(act, "");
        statBadge.setGravity(Gravity.CENTER);
        root.addView(statBadge, UI.lpm(act, UI.MP, UI.WC, 6));

        scanState = UI.note(act, "");
        scanState.setGravity(Gravity.CENTER);
        root.addView(scanState, UI.lpm(act, UI.MP, UI.WC, 2));

        // 分类容器
        catBox = UI.col(act);
        root.addView(catBox, UI.lpm(act, UI.MP, UI.WC, 8));

        // 底部操作
        LinearLayout btns = UI.row(act);
        cleanBtn = UI.danger(act, "清理 (0 B)");
        cleanBtn.setEnabled(false);
        cleanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doClean(); }
        });
        allBtn = UI.secondary(act, "一键全清安全项");
        allBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cleanAllSafe(); }
        });
        LinearLayout.LayoutParams h1 = new LinearLayout.LayoutParams(0, Theme.dp(act, 46), 1f);
        LinearLayout.LayoutParams h2 = new LinearLayout.LayoutParams(0, Theme.dp(act, 46), 1f);
        h2.leftMargin = Theme.dp(act, 8);
        btns.addView(cleanBtn, h1);
        btns.addView(allBtn, h2);
        root.addView(btns, UI.lpm(act, UI.MP, UI.WC, 14));
        root.addView(UI.spacer(act, 20));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));

        refreshDisk();
        refreshStat();
        return scroll;
    }

    // ---------- 磁盘 ----------

    void refreshDisk() {
        try {
            StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            long used = total - free;
            float pct = total > 0 ? used * 100f / total : 0f;
            bar.setPercent(pct);
            diskText.setText(String.format(java.util.Locale.US,
                    "已用 %s / %s · 可用 %s · %.1f%%",
                    Util.fmtSize(used), Util.fmtSize(total), Util.fmtSize(free), pct));
        } catch (Exception e) {
            diskText.setText("无法读取存储信息");
        }
    }

    void refreshStat() {
        Store s = act.store;
        if (s.totalCount() > 0) {
            statBadge.setText("累计清理 " + s.totalCount() + " 项 · 释放 " + Util.fmtSize(s.totalFreed()));
        } else {
            statBadge.setText("");
        }
    }

    // ---------- 扫描 ----------

    private void startScan() {
        if (scanning) return;
        scanning = true;
        scanBtn.setEnabled(false);
        scanBtn.setText("扫描中…");
        catBox.removeAllViews();
        cleanBtn.setEnabled(false);
        cleanBtn.setText("清理 (0 B)");

        final ScanEngine eng = new ScanEngine(act, act.store.whitelist());
        new Thread(new Runnable() {
            public void run() {
                final List<JunkCategory> result = eng.scan(new ScanEngine.Progress() {
                    public void onCategory(final String name) {
                        ui.post(new Runnable() {
                            public void run() { scanState.setText("正在扫描：" + name); }
                        });
                    }
                    public void onDone(List<JunkCategory> c) {}
                });
                ui.post(new Runnable() {
                    public void run() {
                        cats = result;
                        scanning = false;
                        scanBtn.setEnabled(true);
                        scanBtn.setText("🔍  重新扫描");
                        scanState.setText("");
                        renderCats();
                        updateCleanBtn();
                    }
                });
            }
        }).start();
    }

    private void renderCats() {
        catBox.removeAllViews();
        long total = 0;
        for (JunkCategory c : cats) total += c.total();
        if (total == 0) {
            TextView e = UI.note(act, "✦\n未发现可清理的垃圾");
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, Theme.dp(act, 24), 0, Theme.dp(act, 24));
            catBox.addView(e);
            return;
        }

        for (final JunkCategory c : cats) {
            if (c.items.isEmpty()) continue;
            catBox.addView(buildCatCard(c), UI.lpm(act, UI.MP, UI.WC, 10));
        }
    }

    private View buildCatCard(final JunkCategory c) {
        final LinearLayout card = UI.card(act);

        LinearLayout head = UI.row(act);
        TextView icon = UI.text(act, c.icon, 20, Theme.TEXT);
        head.addView(icon);

        LinearLayout info = UI.col(act);
        LinearLayout nameRow = UI.row(act);
        nameRow.addView(UI.title(act, c.name));
        if (c.careful) {
            TextView bd = UI.badge(act, "谨慎", Theme.DANGER, 0x22FB7185);
            LinearLayout.LayoutParams p = UI.lp(UI.WC, UI.WC);
            p.leftMargin = Theme.dp(act, 6);
            nameRow.addView(bd, p);
        }
        info.addView(nameRow);
        info.addView(UI.note(act, c.desc + " · " + c.items.size() + " 项"));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, UI.WC, 1f);
        ip.leftMargin = Theme.dp(act, 10);
        head.addView(info, ip);

        TextView size = UI.text(act, Util.fmtSize(c.total()), 13.5f, Theme.ACCENT);
        size.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(size);

        final CheckBox all = UI.check(act, !c.careful);
        head.addView(all);
        card.addView(head);

        // 明细（默认收起）
        final LinearLayout detail = UI.col(act);
        detail.setVisibility(View.GONE);
        for (final JunkItem it : c.items) {
            it.checked = !c.careful;
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
            final CheckBox cb = UI.check(act, it.checked);
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                    it.checked = on;
                    updateCleanBtn();
                }
            });
            r.addView(cb);
            TextView nm = UI.text(act, it.name, 12, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            r.addView(nm, np);
            r.addView(UI.text(act, Util.fmtSize(it.size), 11.5f, Theme.DIM));

            // 长按加白名单
            r.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    String key = new java.io.File(it.path).getName();
                    act.store.addWhitelist(key);
                    it.checked = false;
                    cb.setChecked(false);
                    v.setAlpha(0.35f);
                    act.toast("已加入白名单: " + key);
                    updateCleanBtn();
                    return true;
                }
            });
            detail.addView(r);
        }
        card.addView(detail);

        all.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                for (int i = 0; i < detail.getChildCount(); i++) {
                    View row = detail.getChildAt(i);
                    if (row instanceof LinearLayout) {
                        View first = ((LinearLayout) row).getChildAt(0);
                        if (first instanceof CheckBox) ((CheckBox) first).setChecked(on);
                    }
                }
                for (JunkItem it : c.items) it.checked = on;
                updateCleanBtn();
            }
        });

        head.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                detail.setVisibility(detail.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });
        return card;
    }

    private void updateCleanBtn() {
        long sel = 0;
        int n = 0;
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) if (it.checked) { sel += it.size; n++; }
        }
        cleanBtn.setText("清理 (" + Util.fmtSize(sel) + ")");
        cleanBtn.setEnabled(n > 0);
    }

    // ---------- 清理 ----------

    private void doClean() {
        cleanBtn.setEnabled(false);
        cleanBtn.setText("清理中…");
        new Thread(new Runnable() {
            public void run() {
                final CleanEngine.Result r = new CleanEngine().clean(cats);
                ui.post(new Runnable() {
                    public void run() {
                        act.store.addStat(r.freed, r.count);
                        act.toast("已清理 " + r.count + " 项 · 释放 " + Util.fmtSize(r.freed)
                                + (r.errors.isEmpty() ? "" : "（" + r.errors.size() + " 项失败）"));
                        catBox.removeAllViews();
                        cats.clear();
                        cleanBtn.setText("清理 (0 B)");
                        cleanBtn.setEnabled(false);
                        scanBtn.setText("🔍  开始扫描");
                        refreshDisk();
                        refreshStat();
                    }
                });
            }
        }).start();
    }

    private void cleanAllSafe() {
        if (cats.isEmpty()) { act.toast("请先扫描"); return; }
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) it.checked = !c.careful;
        }
        updateCleanBtn();
        doClean();
    }
}
