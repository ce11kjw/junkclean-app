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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 首页：存储条 + 扫描 + 分类卡 + 清理 + 前后对比 + 目录排行 */
public class HomePage extends PageBase {

    private ScrollView scroll;
    private LinearLayout catBox;
    private StorageBarView bar;
    private TextView diskText, rootBadge, statBadge, scanState, compareText;
    private Button scanBtn, cleanBtn, allBtn, aiBtn;
    private TextView rescanBtn, aiText;
    private List<JunkCategory> cats = new ArrayList<JunkCategory>();
    private boolean scanning;
    private long freeBefore;

    public HomePage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        // 标题行
        LinearLayout head = UI.row(act);
        TextView t = UI.text(act, "JunkClean", 21, Theme.TEXT);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        head.addView(t);
        rootBadge = UI.badge(act, "…", Theme.MUTED, Theme.alpha(Theme.MUTED, 0x22));
        LinearLayout.LayoutParams bp = UI.lp(UI.WC, UI.WC);
        bp.leftMargin = Theme.dp(act, 8);
        head.addView(rootBadge, bp);
        root.addView(head);

        // 存储卡
        LinearLayout diskCard = UI.card(act);
        diskCard.addView(UI.h2(act, "存储空间"));
        bar = new StorageBarView(act);
        diskCard.addView(bar, UI.lpm(act, UI.MP, UI.WC, 10));
        diskText = UI.note(act, "读取中…");
        diskCard.addView(diskText, UI.lpm(act, UI.MP, UI.WC, 8));
        compareText = UI.text(act, "", 11.5f, Theme.ACCENT);
        diskCard.addView(compareText, UI.lpm(act, UI.MP, UI.WC, 4));
        root.addView(diskCard, UI.lpm(act, UI.MP, UI.WC, 12));

        // 扫描按钮
        scanBtn = UI.primary(act, "🔍  开始扫描");
        scanBtn.setTextSize(14.5f);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startScan(false); }
        });
        root.addView(scanBtn, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H_MAIN), 14));

        // 强制重扫
        rescanBtn = UI.text(act, "60 秒内复用上次结果 · 点此强制重扫", 11, Theme.DIM);
        rescanBtn.setGravity(Gravity.CENTER);
        rescanBtn.setPadding(0, Theme.dp(act, 6), 0, 0);
        rescanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startScan(true); }
        });
        root.addView(rescanBtn, UI.lpm(act, UI.MP, UI.WC, 2));

        statBadge = UI.note(act, "");
        statBadge.setGravity(Gravity.CENTER);
        root.addView(statBadge, UI.lpm(act, UI.MP, UI.WC, 6));

        scanState = UI.text(act, "", 11.5f, Theme.ACCENT);
        scanState.setGravity(Gravity.CENTER);
        root.addView(scanState, UI.lpm(act, UI.MP, UI.WC, 2));

        catBox = UI.col(act);
        root.addView(catBox, UI.lpm(act, UI.MP, UI.WC, 8));

        // AI 建议：标题独占一行，两个按钮在底部同一水平线
        LinearLayout aiCard = UI.card(act);
        aiCard.addView(UI.title(act, "🤖  AI 清理建议"));
        aiCard.addView(UI.note(act, "把扫描结果交给 AI 分析，给出该清哪些、留哪些的建议"));
        aiText = UI.text(act, "", 12, Theme.MUTED);
        aiText.setLineSpacing(0, 1.45f);
        aiCard.addView(aiText, UI.lpm(act, UI.MP, UI.WC, 8));

        aiBtn = UI.primary(act, "分析扫描结果");
        Button aiApply = UI.secondary(act, "采纳建议");
        aiBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { askAi(); }
        });
        aiApply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { applyAiAdvice(); }
        });
        aiCard.addView(UI.btnRow(act, UI.BTN_H, aiBtn, aiApply), UI.lpm(act, UI.MP, UI.WC, 10));
        root.addView(aiCard, UI.lpm(act, UI.MP, UI.WC, 12));

        // 底部操作
        cleanBtn = UI.danger(act, "清理 (0 B)");
        cleanBtn.setEnabled(false);
        cleanBtn.setAlpha(0.5f);
        cleanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmClean(); }
        });
        allBtn = UI.secondary(act, "一键全清安全项");
        allBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cleanAllSafe(); }
        });
        root.addView(UI.btnRow(act, UI.BTN_H_MAIN, cleanBtn, allBtn), UI.lpm(act, UI.MP, UI.WC, 14));

        root.addView(UI.spacer(act, 24));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));

        refreshDisk();
        refreshStat();
        refreshRootBadge();
        return scroll;
    }

    // ---------- 状态刷新 ----------

    void refreshRootBadge() {
        if (rootBadge == null) return;
        boolean r = Shell.hasRoot();
        rootBadge.setText(r ? Shell.detectManager() : "无 root");
        rootBadge.setTextColor(r ? Theme.ACCENT : Theme.WARN);
        rootBadge.setBackground(Theme.badge(act,
                Theme.alpha(r ? Theme.ACCENT : Theme.WARN, 0x22)));
    }

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

    long currentFree() {
        try {
            StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
        } catch (Exception e) { return 0; }
    }

    void refreshStat() {
        Store s = act.store;
        String today = Util.fmtDate(System.currentTimeMillis());
        int td = s.dayCount(today);
        if (s.totalCount() > 0) {
            statBadge.setText("今日 " + td + " 项 · 累计 " + s.totalCount()
                    + " 项 / " + Util.fmtSize(s.totalFreed()));
        } else {
            statBadge.setText("");
        }
    }

    // ---------- 扫描 ----------

    private void startScan(final boolean force) {
        if (scanning) return;
        scanning = true;
        scanBtn.setEnabled(false);
        scanBtn.setText("扫描中…");
        catBox.removeAllViews();
        compareText.setText("");
        setCleanEnabled(false);
        cleanBtn.setText("清理 (0 B)");
        freeBefore = currentFree();

        final ScanEngine eng = new ScanEngine(act, act.store);
        new Thread(new Runnable() {
            public void run() {
                final List<JunkCategory> result = eng.scan(force, new ScanEngine.Progress() {
                    public void onCategory(final String name, final int i, final int n) {
                        ui.post(new Runnable() {
                            public void run() {
                                scanState.setText("正在扫描 " + i + "/" + n + "：" + name);
                            }
                        });
                    }
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
        int shown = 0;
        for (JunkCategory c : cats) total += c.total();
        if (total == 0) {
            catBox.addView(UI.empty(act, "未发现可清理的垃圾\n设备很干净"));
            return;
        }
        for (JunkCategory c : cats) {
            if (c.items.isEmpty()) continue;
            catBox.addView(buildCatCard(c), UI.lpm(act, UI.MP, UI.WC, 10));
            shown++;
        }
        if (shown == 0) catBox.addView(UI.empty(act, "未发现可清理的垃圾"));
    }

    private View buildCatCard(final JunkCategory c) {
        final LinearLayout card = UI.card(act);

        LinearLayout head = UI.row(act);
        head.addView(UI.text(act, c.icon, 19, Theme.TEXT));

        LinearLayout info = UI.col(act);
        LinearLayout nameRow = UI.row(act);
        nameRow.addView(UI.title(act, c.name));
        if (c.careful) {
            TextView bd = UI.badge(act, "谨慎", Theme.DANGER, Theme.alpha(Theme.DANGER, 0x22));
            LinearLayout.LayoutParams pp = UI.lp(UI.WC, UI.WC);
            pp.leftMargin = Theme.dp(act, 6);
            nameRow.addView(bd, pp);
        }
        if (c.needRoot && !Shell.hasRoot()) {
            TextView bd = UI.badge(act, "需 root", Theme.WARN, Theme.alpha(Theme.WARN, 0x22));
            LinearLayout.LayoutParams pp = UI.lp(UI.WC, UI.WC);
            pp.leftMargin = Theme.dp(act, 6);
            nameRow.addView(bd, pp);
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
        LinearLayout.LayoutParams cp = UI.lp(UI.WC, UI.WC);
        cp.leftMargin = Theme.dp(act, 6);
        head.addView(all, cp);
        card.addView(head);

        final LinearLayout detail = UI.col(act);
        detail.setVisibility(View.GONE);
        for (final JunkItem it : c.items) {
            it.checked = !c.careful;
            final LinearLayout[] holder = new LinearLayout[1];
            Runnable onChange = new Runnable() { public void run() { updateCleanBtn(); } };
            Runnable onLong = new Runnable() {
                public void run() {
                    String key = new File(it.path).getName();
                    act.store.addWhitelist(key);
                    it.checked = false;
                    if (holder[0] != null) {
                        holder[0].setAlpha(0.35f);
                        View f = holder[0].getChildAt(0);
                        if (f instanceof CheckBox) ((CheckBox) f).setChecked(false);
                    }
                    act.toast("已加入白名单：" + key);
                    updateCleanBtn();
                }
            };
            LinearLayout r = UI.fileRow(act, it, onChange, onLong);
            holder[0] = r;
            detail.addView(r);
        }
        card.addView(detail);

        TextView tip = UI.note(act, "点击卡片展开明细 · 长按条目加入白名单");
        tip.setVisibility(View.GONE);
        card.addView(tip);

        all.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                for (int i = 0; i < detail.getChildCount(); i++) {
                    View row = detail.getChildAt(i);
                    if (row instanceof LinearLayout) {
                        View f = ((LinearLayout) row).getChildAt(0);
                        if (f instanceof CheckBox) ((CheckBox) f).setChecked(on);
                    }
                }
                for (JunkItem it : c.items) it.checked = on;
                updateCleanBtn();
            }
        });

        final TextView tipRef = tip;
        head.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean open = detail.getVisibility() == View.GONE;
                detail.setVisibility(open ? View.VISIBLE : View.GONE);
                tipRef.setVisibility(open ? View.VISIBLE : View.GONE);
            }
        });
        return card;
    }

    private void setCleanEnabled(boolean on) {
        cleanBtn.setEnabled(on);
        cleanBtn.setAlpha(on ? 1f : 0.5f);
    }

    private void updateCleanBtn() {
        long sel = 0;
        int n = 0;
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) if (it.checked) { sel += it.size; n++; }
        }
        cleanBtn.setText("清理 (" + Util.fmtSize(sel) + ")");
        setCleanEnabled(n > 0);
    }

    // ---------- 清理 ----------

    private void confirmClean() {
        long sel = 0;
        int n = 0;
        boolean hasCareful = false;
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) if (it.checked) { sel += it.size; n++; }
            if (c.careful) for (JunkItem it : c.items) if (it.checked) hasCareful = true;
        }
        String msg = "将清理 " + n + " 项，约 " + Util.fmtSize(sel)
                + (act.store.toTrash() ? "\n\nsdcard 文件会先移入回收站，可恢复。" : "\n\n直接删除，不可恢复！");
        if (hasCareful) msg += "\n\n⚠ 包含「谨慎」分类项目，请确认。";
        UI.confirm(act, "确认清理", msg, new Runnable() {
            public void run() { doClean(); }
        });
    }

    private void doClean() {
        setCleanEnabled(false);
        cleanBtn.setText("清理中…");
        new Thread(new Runnable() {
            public void run() {
                final CleanEngine.Result r = new CleanEngine(act.store.toTrash()).clean(cats);
                ui.post(new Runnable() {
                    public void run() {
                        act.store.addStat(r.freed, r.count);
                        ScanEngine.invalidate();
                        long after = currentFree();
                        long delta = after - freeBefore;
                        compareText.setText("清理完成 · 可用空间 "
                                + Util.fmtSize(freeBefore) + " → " + Util.fmtSize(after)
                                + (delta > 0 ? "（+" + Util.fmtSize(delta) + "）" : ""));
                        String msg = "已处理 " + r.count + " 项";
                        if (r.freed > 0) msg += " · 释放 " + Util.fmtSize(r.freed);
                        if (r.toTrash > 0) msg += " · " + r.toTrash + " 项入回收站（"
                                + Util.fmtSize(r.trashed) + "，清空后才释放）";
                        if (!r.errors.isEmpty()) msg += " · " + r.errors.size() + " 项失败";
                        act.toast(msg);
                        catBox.removeAllViews();
                        cats.clear();
                        cleanBtn.setText("清理 (0 B)");
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
        int n = 0;
        long sel = 0;
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) {
                it.checked = !c.careful;
                if (it.checked) { n++; sel += it.size; }
            }
        }
        if (n == 0) { act.toast("没有安全项可清理"); return; }
        updateCleanBtn();
        final int fn = n;
        final long fs = sel;
        UI.confirm(act, "一键全清安全项",
                "将清理 " + fn + " 项安全项目，约 " + Util.fmtSize(fs) + "\n（跳过所有「谨慎」分类）",
                new Runnable() { public void run() { doClean(); } });
    }

    // ---------- AI ----------

    private String lastAdvice = "";

    private void askAi() {
        if (!act.store.aiReady()) {
            aiText.setText("尚未配置 AI，请到「设置 → AI 清理建议」填写端点与 Key。");
            return;
        }
        if (cats.isEmpty()) { act.toast("请先扫描"); return; }
        aiBtn.setEnabled(false);
        aiText.setText("AI 分析中…");
        new Thread(new Runnable() {
            public void run() {
                long total = 0, free = currentFree();
                try {
                    StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
                    total = fs.getBlockCountLong() * fs.getBlockSizeLong();
                } catch (Exception ignored) {}
                final String summary = Ai.summarize(cats, free, total);
                final String r = Ai.advise(act.store, summary);
                ui.post(new Runnable() {
                    public void run() {
                        aiBtn.setEnabled(true);
                        if (r.startsWith("ERR:")) {
                            aiText.setText("请求失败：" + r.substring(4));
                            lastAdvice = "";
                        } else {
                            aiText.setText(r);
                            lastAdvice = r;
                        }
                    }
                });
            }
        }).start();
    }

    /** 按 AI 提到的分类名勾选对应分类，未提到的取消勾选 */
    private void applyAiAdvice() {
        if (lastAdvice.isEmpty()) { act.toast("请先让 AI 分析"); return; }
        List<String> hit = Ai.matchCategories(lastAdvice, cats);
        if (hit.isEmpty()) { act.toast("建议中未匹配到具体分类"); return; }
        int n = 0;
        for (JunkCategory c : cats) {
            boolean on = hit.contains(c.id) && !c.careful;
            for (JunkItem it : c.items) { it.checked = on; if (on) n++; }
        }
        renderCats();
        updateCleanBtn();
        act.toast("已按建议勾选 " + n + " 项（谨慎分类仍需手动确认）");
    }

}
