package com.ce11kjw.junkclean;

import android.os.Environment;
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

/** 首页：存储读数 + 扫描 + 分类 + AI 建议 + 清理 */
public class HomePage extends PageBase {

    private ScrollView scroll;
    private LinearLayout catBox, scanAction;
    private SegmentGauge gauge;
    private TextView rootBadge, pctText, freeText, diskText, compareText;
    private TextView statBadge, scanProgress, scanState, rescanBtn, aiText;
    private Button cleanBtn, allBtn, aiBtn;
    private List<JunkCategory> cats = new ArrayList<JunkCategory>();
    private boolean scanning;
    private volatile boolean cancelFlag;
    private long freeBefore;
    private String lastAdvice = "";

    public HomePage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, Theme.S4);
        root.setPadding(p, Theme.dp(act, Theme.S5), p, p);

        root.addView(buildHeader());
        root.addView(buildStorageCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S5));
        root.addView(buildActionArea(), UI.lpm(act, UI.MP, UI.WC, Theme.S5));

        catBox = UI.col(act);
        root.addView(catBox, UI.lpm(act, UI.MP, UI.WC, Theme.S3));

        root.addView(buildAiCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S4));
        root.addView(buildCleanRow(), UI.lpm(act, UI.MP, UI.WC, Theme.S4));
        root.addView(UI.spacer(act, Theme.S8));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));

        refreshDisk();
        refreshStat();
        refreshRootBadge();
        Anim.stagger(root, 40, 45);
        return scroll;
    }

    // ---------- 页头 ----------

    private View buildHeader() {
        LinearLayout col = UI.col(act);
        col.addView(UI.eyebrow(act, "存储维护"));

        LinearLayout row = UI.row(act);
        TextView t = UI.display(act, "JunkClean", Theme.T_TITLE, Theme.TEXT);
        t.setTypeface(Theme.display(), android.graphics.Typeface.BOLD);
        row.addView(t, new LinearLayout.LayoutParams(0, UI.WC, 1f));
        rootBadge = UI.badge(act, "…", Theme.MUTED, Theme.alpha(Theme.MUTED, 0x22));
        row.addView(rootBadge);
        col.addView(row, UI.lpm(act, UI.MP, UI.WC, 2));
        return col;
    }

    // ---------- 存储卡（签名元素） ----------

    private View buildStorageCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "内部存储"));

        gauge = new SegmentGauge(act);
        c.addView(gauge, UI.lpm(act, UI.MP, UI.WC, Theme.S3));

        LinearLayout readout = UI.row(act);
        LinearLayout usedCol = UI.col(act);
        pctText = UI.display(act, "—", Theme.T_DISPLAY, Theme.TEXT);
        usedCol.addView(pctText);
        usedCol.addView(UI.eyebrow(act, "已用"));
        readout.addView(usedCol, new LinearLayout.LayoutParams(0, UI.WC, 1f));

        LinearLayout freeCol = UI.col(act);
        freeCol.setGravity(Gravity.END);
        freeText = UI.data(act, "—", Theme.T_TITLE, Theme.ACCENT);
        freeCol.addView(freeText);
        TextView lbl = UI.eyebrow(act, "可用空间");
        lbl.setGravity(Gravity.END);
        freeCol.addView(lbl);
        readout.addView(freeCol, new LinearLayout.LayoutParams(0, UI.WC, 1f));
        c.addView(readout, UI.lpm(act, UI.MP, UI.WC, Theme.S4));

        diskText = UI.data(act, "读取中…", Theme.T_DATA_S, Theme.DIM);
        c.addView(diskText, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        compareText = UI.data(act, "", Theme.T_DATA_S, Theme.ACCENT);
        c.addView(compareText, UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        return c;
    }

    // ---------- 扫描区 ----------

    private View buildActionArea() {
        LinearLayout col = UI.col(act);

        scanAction = UI.actionButton(act, "开始扫描", "→", new Runnable() {
            public void run() {
                if (scanning) cancelScan();
                else startScan(false);
            }
        });
        col.addView(scanAction, UI.lp(UI.MP, Theme.dp(act, UI.BTN_H_MAIN)));

        rescanBtn = UI.text(act, "60 秒内复用上次结果 · 点此强制重扫",
                Theme.T_MICRO + 1f, Theme.DIM);
        rescanBtn.setGravity(Gravity.CENTER);
        rescanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startScan(true); }
        });
        col.addView(rescanBtn, UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        scanState = UI.data(act, "", Theme.T_DATA_S, Theme.ACCENT);
        scanState.setGravity(Gravity.CENTER);
        col.addView(scanState, UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        scanProgress = UI.data(act, "", Theme.T_DATA_S, Theme.ACCENT);
        scanProgress.setGravity(Gravity.CENTER);
        col.addView(scanProgress, UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        statBadge = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        statBadge.setGravity(Gravity.CENTER);
        col.addView(statBadge, UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        return col;
    }

    private void setActionLabel(String label) {
        if (scanAction == null) return;
        View v = scanAction.getChildAt(0);
        if (v instanceof TextView) ((TextView) v).setText(label);
    }

    /** 扫描可中断：全盘模式动辄几分钟，不给取消不合理 */
    private void cancelScan() {
        cancelFlag = true;
        scanState.setText("正在取消…");
    }

    // ---------- AI 卡 ----------

    private View buildAiCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "智能分析"));
        c.addView(UI.title(act, "AI 清理建议"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "把扫描结果交给 AI，给出该清哪些、留哪些的判断"),
                UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        aiText = UI.text(act, "", Theme.T_BODY_S, Theme.MUTED);
        aiText.setLineSpacing(0, 1.5f);
        c.addView(aiText, UI.lpm(act, UI.MP, UI.WC, Theme.S3));

        aiBtn = UI.primary(act, "分析扫描结果");
        Button apply = UI.secondary(act, "采纳建议");
        aiBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { askAi(); }
        });
        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { applyAiAdvice(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, aiBtn, apply), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        return c;
    }

    // ---------- 清理按钮 ----------

    private View buildCleanRow() {
        cleanBtn = UI.danger(act, "清理 (0 B)");
        setCleanEnabled(false);
        cleanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmClean(); }
        });
        allBtn = UI.secondary(act, "一键全清安全项");
        allBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cleanAllSafe(); }
        });
        return UI.btnRow(act, UI.BTN_H_MAIN, cleanBtn, allBtn);
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
        if (gauge == null) return;
        try {
            StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            long used = total - free;
            float pct = total > 0 ? used * 100f / total : 0f;

            gauge.setPercent(pct);
            Anim.countPercent(pctText, 0f, pct);
            Anim.countSize(freeText, 0, free);
            diskText.setText("已用 " + Util.fmtSize(used) + "  /  " + Util.fmtSize(total));
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
        if (statBadge == null) return;
        if (scanProgress != null) scanProgress.setText("");
        Store s = act.store;
        if (s.totalCount() > 0) {
            String today = Util.fmtDate(System.currentTimeMillis());
            statBadge.setText("今日 " + s.dayCount(today) + " 项   累计 "
                    + s.totalCount() + " 项 / " + Util.fmtSize(s.totalFreed()));
        } else {
            statBadge.setText("");
        }
    }

    // ---------- 扫描 ----------

    private void startScan(final boolean force) {
        if (scanning) return;
        scanning = true;
        cancelFlag = false;
        setActionLabel("取消扫描");
        catBox.removeAllViews();
        compareText.setText("");
        setCleanEnabled(false);
        cleanBtn.setText("清理 (0 B)");
        freeBefore = currentFree();

        final ScanEngine eng = new ScanEngine(act, act.store);
        new Thread(new Runnable() {
            public void run() {
                final List<JunkCategory> result = eng.scan(force, new ScanEngine.Progress() {
                    public void onCategory(final String name, final int i, final int n,
                                           final int items, final long bytes) {
                        post(new Runnable() {
                            public void run() {
                                scanState.setText("[" + i + "/" + n + "]  " + name);
                                if (scanProgress != null) {
                                    scanProgress.setText("已发现 " + items + " 项   " + Util.fmtSize(bytes));
                                }
                            }
                        });
                    }
                    public boolean cancelled() { return cancelFlag; }
                });
                post(new Runnable() {
                    public void run() {
                        cats = result;
                        scanning = false;
                        setActionLabel(cancelFlag ? "开始扫描" : "重新扫描");
                        scanState.setText(cancelFlag ? "已取消" : "");
                        if (scanProgress != null) scanProgress.setText("");
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
            View guide = buildEmptyGuide();
            catBox.addView(guide);
            Anim.enter(guide, 60L);
            return;
        }
        int shown = 0;
        for (JunkCategory c : cats) {
            if (c.items.isEmpty()) continue;
            View card = buildCatCard(c);
            catBox.addView(card, UI.lpm(act, UI.MP, UI.WC, shown == 0 ? 0 : Theme.S3));
            Anim.enter(card, 60L * shown);
            shown++;
        }
        if (shown == 0) catBox.addView(UI.empty(act, "未发现可清理的垃圾"));
    }

    /** 空状态：给出下一步可做的事，而不是只说「没找到」 */
    private View buildEmptyGuide() {
        LinearLayout card = UI.card(act);
        card.addView(UI.eyebrow(act, "扫描结果"));
        card.addView(UI.title(act, "没有发现垃圾"), UI.lpm(act, UI.MP, UI.WC, 2));

        boolean root = Shell.hasRoot();
        boolean full = act.store.fullScan();
        StringBuilder tip = new StringBuilder();
        if (!root) {
            tip.append("当前为受限模式，只能看到公共目录与外部缓存。\n授予 root 后可扫描应用内部缓存与系统日志。");
        } else if (!full) {
            tip.append("已启用 root 但未开启全盘扫描。\n开启后会额外检查 /data、/cache 等系统分区。");
        } else {
            tip.append("已使用全盘模式扫描，设备确实很干净。\n可到「文件」页手动排查大文件与重复文件。");
        }
        card.addView(UI.note(act, tip.toString()), UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        Button a = root && !full ? UI.primary(act, "开启全盘扫描")
                                 : UI.primary(act, root ? "查看大文件" : "测试 root 权限");
        final boolean toFull = root && !full;
        final boolean toRoot = !root;
        a.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (toFull) {
                    act.store.setFullScan(true);
                    ScanEngine.invalidate();
                    act.toast("已开启全盘扫描");
                    startScan(true);
                } else if (toRoot) {
                    boolean ok = Shell.testRoot();
                    act.toast(ok ? "root 授权成功" : "未获得 root");
                    refreshRootBadge();
                } else {
                    act.switchTab(2);
                }
            }
        });
        card.addView(UI.btnRow(act, UI.BTN_H, a), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        return card;
    }

    private View buildCatCard(final JunkCategory c) {
        final LinearLayout card = UI.card(act);

        LinearLayout head = UI.row(act);
        // Lucide 图标（回退：无 drawable 时显示文本）
        android.widget.ImageView iconIv = IconView.of(act, c.icon, 18, Theme.TEXT);
        if (iconIv.getDrawable() != null) {
            head.addView(iconIv);
        } else {
            TextView icon = UI.text(act, c.icon, 18, Theme.TEXT);
            head.addView(icon);
        }

        LinearLayout info = UI.col(act);
        LinearLayout nameRow = UI.row(act);
        nameRow.addView(UI.title(act, c.name));
        if (c.careful) {
            TextView bd = UI.badge(act, "谨慎", Theme.DANGER, Theme.alpha(Theme.DANGER, 0x22));
            LinearLayout.LayoutParams pp = UI.lp(UI.WC, UI.WC);
            pp.leftMargin = Theme.dp(act, Theme.S2);
            nameRow.addView(bd, pp);
        }
        if (c.needRoot && !Shell.hasRoot()) {
            TextView bd = UI.badge(act, "需 root", Theme.WARN, Theme.alpha(Theme.WARN, 0x22));
            LinearLayout.LayoutParams pp = UI.lp(UI.WC, UI.WC);
            pp.leftMargin = Theme.dp(act, Theme.S2);
            nameRow.addView(bd, pp);
        }
        info.addView(nameRow);
        info.addView(UI.data(act, c.items.size() + " 项", Theme.T_DATA_S, Theme.DIM));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, UI.WC, 1f);
        ip.leftMargin = Theme.dp(act, Theme.S3);
        head.addView(info, ip);

        TextView size = UI.data(act, Util.fmtSize(c.total()), Theme.T_DATA, Theme.ACCENT);
        head.addView(size);

        final CheckBox all = UI.check(act, !c.careful);
        LinearLayout.LayoutParams cp = UI.lp(UI.WC, UI.WC);
        cp.leftMargin = Theme.dp(act, Theme.S2);
        head.addView(all, cp);
        card.addView(head);

        final LinearLayout detail = UI.col(act);
        detail.setVisibility(View.GONE);
        for (final JunkItem it : c.items) {
            it.checked = !c.careful;
            Runnable onChange = new Runnable() {
                public void run() { updateCleanBtn(); }
            };
            Runnable onLong = new Runnable() {
                public void run() {
                    String key = new File(it.path).getName();
                    act.store.addWhitelist(key);
                    it.checked = false;
                    ScanEngine.invalidate();
                    act.toast("已加入白名单：" + key);
                    updateCleanBtn();
                }
            };
            detail.addView(UI.fileRow(act, it, onChange, onLong));
        }
        card.addView(detail);

        final TextView tip = UI.note(act, "长按条目加入白名单");
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

        Anim.pressableItem(head);
        head.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean open = detail.getVisibility() == View.GONE;
                detail.setVisibility(open ? View.VISIBLE : View.GONE);
                tip.setVisibility(open ? View.VISIBLE : View.GONE);
                if (open) Anim.stagger(detail, 0, 18);
            }
        });
        return card;
    }

    private void setCleanEnabled(boolean on) {
        cleanBtn.setEnabled(on);
        cleanBtn.setAlpha(on ? 1f : 0.45f);
    }

    private void updateCleanBtn() {
        long sel = 0;
        int n = 0;
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) if (it.checked) { sel += it.size; n++; }
        }
        cleanBtn.setText("清理 " + Util.fmtSize(sel));
        setCleanEnabled(n > 0);
    }

    // ---------- 清理 ----------

    private void confirmClean() {
        long sel = 0;
        int n = 0;
        boolean careful = false;
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) {
                if (!it.checked) continue;
                sel += it.size;
                n++;
                if (c.careful) careful = true;
            }
        }
        String msg = "将清理 " + n + " 项，约 " + Util.fmtSize(sel)
                + (act.store.toTrash() ? "\n\nsdcard 文件会先移入回收站，可恢复。"
                                       : "\n\n直接删除，不可恢复。");
        if (careful) msg += "\n\n包含「谨慎」分类项目，请确认。";
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
                post(new Runnable() {
                    public void run() {
                        act.store.addStat(r.freed, r.count);
                        for (java.util.Map.Entry<String, long[]> e : r.catFreed.entrySet()) {
                            act.store.addCatStat(e.getKey(), e.getValue()[0], (int) e.getValue()[1]);
                        }
                        ScanEngine.invalidate();
                        long after = currentFree();
                        long delta = after - freeBefore;
                        compareText.setText("可用 " + Util.fmtSize(freeBefore) + " → "
                                + Util.fmtSize(after)
                                + (delta > 0 ? "   +" + Util.fmtSize(delta) : ""));
                        String msg = "已处理 " + r.count + " 项";
                        if (r.freed > 0) msg += " · 释放 " + Util.fmtSize(r.freed);
                        if (r.toTrash > 0) msg += " · " + r.toTrash + " 项入回收站";
                        act.toast(msg);
                        if (!r.errors.isEmpty()) offerRetry(r.errors, null);
                        catBox.removeAllViews();
                        cats.clear();
                        cleanBtn.setText("清理 0 B");
                        setActionLabel("开始扫描");
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
                "将清理 " + fn + " 项，约 " + Util.fmtSize(fs) + "\n跳过所有「谨慎」分类。",
                new Runnable() { public void run() { doClean(); } });
    }

    // ---------- AI ----------

    private void askAi() {
        if (!act.store.aiReady()) {
            aiText.setText("尚未配置 AI。到「设置 → AI 清理建议」填写端点与密钥。");
            return;
        }
        if (cats.isEmpty()) { act.toast("请先扫描"); return; }
        aiBtn.setEnabled(false);
        aiText.setText("分析中…");
        new Thread(new Runnable() {
            public void run() {
                long total = 0;
                long free = currentFree();
                try {
                    StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
                    total = fs.getBlockCountLong() * fs.getBlockSizeLong();
                } catch (Exception ignored) {}
                final String r = Ai.advise(act.store, Ai.summarize(cats, free, total));
                post(new Runnable() {
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

    private void applyAiAdvice() {
        if (lastAdvice.isEmpty()) { act.toast("请先让 AI 分析"); return; }
        List<String> hit = Ai.matchCategories(lastAdvice, cats);
        if (hit.isEmpty()) { act.toast("建议中未匹配到具体分类"); return; }
        int n = 0;
        for (JunkCategory c : cats) {
            boolean on = hit.contains(c.id) && !c.careful;
            for (JunkItem it : c.items) {
                it.checked = on;
                if (on) n++;
            }
        }
        renderCats();
        updateCleanBtn();
        act.toast("已按建议勾选 " + n + " 项，谨慎分类仍需手动确认");
    }
}
