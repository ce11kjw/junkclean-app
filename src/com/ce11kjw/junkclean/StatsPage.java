package com.ce11kjw.junkclean;

import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 统计页：总览 / 7 天趋势 / 目录排行 / 报告导出 */
public class StatsPage extends PageBase {

    private ScrollView scroll;
    private TextView overview, diskInfo, totalFreedText, totalCountText;
    private StatsChartView chart;
    private LinearLayout rankBox, dayList, catBox;

    public StatsPage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, Theme.S4);
        root.setPadding(p, Theme.dp(act, Theme.S5), p, p);
        LinearLayout head = UI.col(act);
        head.addView(UI.eyebrow(act, "数据"));
        TextView ht = UI.display(act, "清理统计", Theme.T_TITLE, Theme.TEXT);
        ht.setTypeface(Theme.display(), android.graphics.Typeface.BOLD);
        head.addView(ht, UI.lpm(act, UI.WC, UI.WC, 2));
        root.addView(head, UI.lpm(act, UI.MP, UI.WC, 0));

        LinearLayout ov = UI.card(act);
        ov.addView(UI.eyebrow(act, "累计成果"));
        LinearLayout big = UI.row(act);
        LinearLayout c1 = UI.col(act);
        totalFreedText = UI.display(act, "—", Theme.T_DISPLAY, Theme.ACCENT);
        c1.addView(totalFreedText);
        c1.addView(UI.eyebrow(act, "已释放"));
        big.addView(c1, new LinearLayout.LayoutParams(0, UI.WC, 1f));
        LinearLayout c2 = UI.col(act);
        c2.setGravity(android.view.Gravity.END);
        totalCountText = UI.data(act, "—", Theme.T_TITLE, Theme.TEXT);
        c2.addView(totalCountText);
        TextView cl = UI.eyebrow(act, "清理项数");
        cl.setGravity(android.view.Gravity.END);
        c2.addView(cl);
        big.addView(c2, new LinearLayout.LayoutParams(0, UI.WC, 1f));
        ov.addView(big, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        overview = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        ov.addView(overview, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        diskInfo = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        ov.addView(diskInfo, UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(ov);

        LinearLayout ch = UI.card(act);
        ch.addView(UI.eyebrow(act, "最近 7 天"));
        chart = new StatsChartView(act);
        ch.addView(chart, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        dayList = UI.col(act);
        ch.addView(dayList, UI.lpm(act, UI.MP, UI.WC, 10));
        root.addView(ch, UI.lpm(act, UI.MP, UI.WC, Theme.S4));

        LinearLayout rk = UI.card(act);
        rk.addView(UI.eyebrow(act, "空间分布"));
        rk.addView(UI.title(act, "目录体积排行"), UI.lpm(act, UI.MP, UI.WC, 2));
        rk.addView(UI.note(act, "展开大目录的下一级，点击条目查看完整路径"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        rankBox = UI.col(act);
        rk.addView(rankBox, UI.lpm(act, UI.MP, UI.WC, 8));
        Button rankBtn = UI.primary(act, "开始统计");
        Button depthBtn = UI.secondary(act, "统计深度");
        rankBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { loadRank(); }
        });
        depthBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String[] labels = {"仅一级目录（快）", "展开到二级（推荐）"};
                final int[] vals = {1, 2};
                int cur = act.store.rankDepth() == 1 ? 0 : 1;
                UI.pick(act, "统计深度", labels, cur,
                        new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        act.store.setRankDepth(vals[w]);
                        act.toast("已设为：" + labels[w] + "，请重新统计");
                    }
                });
            }
        });
        rk.addView(UI.btnRow(act, UI.BTN_H, rankBtn, depthBtn), UI.lpm(act, UI.MP, UI.WC, 10));
        restoreRank();
        root.addView(rk, UI.lpm(act, UI.MP, UI.WC, Theme.S4));

        // 分类占比：哪类垃圾最多
        LinearLayout cc = UI.card(act);
        cc.addView(UI.eyebrow(act, "分类占比"));
        cc.addView(UI.title(act, "垃圾来源"), UI.lpm(act, UI.MP, UI.WC, 2));
        cc.addView(UI.note(act, "累计清理量按分类拆分"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        catBox = UI.col(act);
        cc.addView(catBox, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        root.addView(cc, UI.lpm(act, UI.MP, UI.WC, Theme.S4));

        LinearLayout dm = UI.card(act);
        dm.addView(UI.eyebrow(act, "数据管理"));
        Button copy = UI.secondary(act, "复制报告");
        Button reset = UI.secondary(act, "重置统计");
        copy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { copyReport(); }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.confirm(act, "重置统计", "清空所有清理统计数据？", new Runnable() {
                    public void run() {
                        act.store.resetStats();
                        act.store.resetCatStats();
                        refresh();
                        act.homePage().refreshStat();
                        act.toast("统计已重置");
                    }
                });
            }
        });
        Button exp = UI.secondary(act, "导出配置");
        Button imp = UI.secondary(act, "导入配置");
        exp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String cfg = act.store.exportConfig();
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        act.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("JunkClean", cfg));
                UI.info(act, "配置已复制到剪贴板", cfg);
            }
        });
        imp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.prompt(act, "导入配置", "粘贴导出的配置文本", "", 8,
                        new UI.Callback<String>() {
                    public void call(String text) {
                        int n = act.store.importConfig(text);
                        ScanEngine.invalidate();
                        act.toast(n > 0 ? "已应用 " + n + " 项配置" : "未识别到有效配置");
                        if (n > 0) act.applyThemeAndRebuild();
                    }
                });
            }
        });
        dm.addView(UI.btnRow(act, UI.BTN_H, copy, reset), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        dm.addView(UI.btnRow(act, UI.BTN_H, exp, imp), UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        root.addView(dm, UI.lpm(act, UI.MP, UI.WC, Theme.S4));
        root.addView(UI.spacer(act, Theme.S8));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        refresh();
        return scroll;
    }

    public void refresh() {
        if (overview == null) return;
        Store s = act.store;
        Anim.countSize(totalFreedText, 0, s.totalFreed());
        Anim.countInt(totalCountText, 0, s.totalCount(), " 项");
        overview.setText("上次清理  " + Util.fmtTime(s.lastClean()));

        try {
            StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            diskInfo.setText("存储  " + Util.fmtSize(total - free) + " / "
                    + Util.fmtSize(total) + "   可用 " + Util.fmtSize(free));
        } catch (Exception e) {
            diskInfo.setText("");
        }

        renderCatBreakdown(s);

        List<Object[]> days = s.recent7();
        chart.setData(days);
        dayList.removeAllViews();
        for (Object[] d : days) {
            long freed = ((Long) d[1]).longValue();
            int cnt = ((Integer) d[2]).intValue();
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 3), 0, Theme.dp(act, 3));
            r.addView(UI.data(act, (String) d[0], Theme.T_DATA_S, Theme.MUTED),
                    new LinearLayout.LayoutParams(Theme.dp(act, 84), UI.WC));
            TextView c = UI.data(act, cnt > 0 ? cnt + " 项" : "—", Theme.T_DATA_S, Theme.DIM);
            r.addView(c, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            r.addView(UI.data(act, freed > 0 ? Util.fmtSize(freed) : "",
                    Theme.T_DATA_S, freed > 0 ? Theme.ACCENT : Theme.DIM));
            dayList.addView(r);
        }
    }

    /** 分类占比：横条 + 等宽读数，按累计释放量排序 */
    private void renderCatBreakdown(Store s) {
        if (catBox == null) return;
        catBox.removeAllViews();

        List<Object[]> rows = new ArrayList<Object[]>();
        long max = 0, sum = 0;
        for (int i = 0; i < Store.CAT_IDS.length; i++) {
            long f = s.catFreed(Store.CAT_IDS[i]);
            if (f <= 0) continue;
            rows.add(new Object[]{Store.CAT_NAMES[i], Long.valueOf(f),
                    Integer.valueOf(s.catCount(Store.CAT_IDS[i]))});
            max = Math.max(max, f);
            sum += f;
        }
        if (rows.isEmpty()) {
            catBox.addView(UI.empty(act, "还没有清理记录"));
            return;
        }
        java.util.Collections.sort(rows, new java.util.Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                return Long.compare((Long) b[1], (Long) a[1]);
            }
        });

        for (Object[] r : rows) {
            long freed = ((Long) r[1]).longValue();
            LinearLayout row = UI.row(act);
            row.setPadding(0, Theme.dp(act, Theme.S1), 0, Theme.dp(act, Theme.S1));
            TextView nm = UI.text(act, (String) r[0], Theme.T_BODY_S, Theme.MUTED);
            nm.setSingleLine(true);
            row.addView(nm, new LinearLayout.LayoutParams(Theme.dp(act, 88), UI.WC));

            SegmentGauge g = new SegmentGauge(act, true);
            g.setPercentImmediate(max > 0 ? freed * 100f / max : 0);
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(0, Theme.dp(act, 10), 1f);
            gp.leftMargin = gp.rightMargin = Theme.dp(act, Theme.S2);
            row.addView(g, gp);

            row.addView(UI.data(act, Util.fmtSize(freed), Theme.T_DATA_S, Theme.ACCENT));
            catBox.addView(row);
        }
        catBox.addView(UI.data(act, "合计  " + Util.fmtSize(sum),
                Theme.T_DATA_S, Theme.DIM), UI.lpm(act, UI.MP, UI.WC, Theme.S2));
    }

    private void loadRank() {
        rankBox.removeAllViews();
        rankBox.addView(UI.note(act, "统计中，大目录可能较慢…"));
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> list = Finder.dirRank(scanRoot(), 24, wl(),
                        act.store.fullScan(), act.store.rankDepth());
                // 写入缓存，重启应用后仍能看到上次结果
                List<String> lines = new ArrayList<String>();
                for (JunkItem it : list) lines.add(it.path + "\t" + it.size + "\t" + it.name);
                act.store.setRankCache(lines);
                post(new Runnable() {
                    public void run() { renderRank(list, System.currentTimeMillis()); }
                });
            }
        }).start();
    }

    /** 从缓存恢复上次排行结果 */
    private void restoreRank() {
        List<String> lines = act.store.rankCache();
        if (lines.isEmpty()) {
            rankBox.removeAllViews();
            rankBox.addView(UI.empty(act, "尚未统计\n点击下方按钮开始"));
            return;
        }
        List<JunkItem> list = new ArrayList<JunkItem>();
        for (String l : lines) {
            String[] p = l.split("\t");
            if (p.length < 2) continue;
            long size;
            try { size = Long.parseLong(p[1]); } catch (Exception e) { continue; }
            list.add(new JunkItem(p[0], p.length > 2 ? p[2] : p[0], size));
        }
        renderRank(list, act.store.rankTime());
    }

    private void renderRank(List<JunkItem> list, long time) {
        rankBox.removeAllViews();
        if (list.isEmpty()) {
            rankBox.addView(UI.empty(act, "无数据"));
            return;
        }
        if (time > 0) {
            rankBox.addView(UI.note(act, "统计于 " + Util.fmtTime(time)
                    + " · 共 " + list.size() + " 项"));
        }
        long max = list.get(0).size;
        for (final JunkItem it : list) {
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
            TextView nm = UI.text(act, it.name, Theme.T_BODY_S, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            r.addView(nm, new LinearLayout.LayoutParams(Theme.dp(act, 104), UI.WC));
            SegmentGauge b = new SegmentGauge(act, true);
            b.setPercent(max > 0 ? it.size * 100f / max : 0);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, Theme.dp(act, 8), 1f);
            bp.leftMargin = bp.rightMargin = Theme.dp(act, 8);
            r.addView(b, bp);
            r.addView(UI.data(act, Util.fmtSize(it.size), Theme.T_DATA_S, Theme.DIM));
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    UI.info(act, it.name, "完整路径：\n" + it.path
                            + "\n\n占用：" + Util.fmtSize(it.size)
                            + "\n\n可到「文件」页的文件清理中打开该目录进行处理。");
                }
            });
            rankBox.addView(r);
            Anim.enter(r, 24L * rankBox.getChildCount());
        }
    }

    private void copyReport() {
        StringBuilder sb = new StringBuilder();
        Store s = act.store;
        sb.append("JunkClean 清理报告\n");
        sb.append("累计清理：").append(s.totalCount()).append(" 项\n");
        sb.append("累计释放：").append(Util.fmtSize(s.totalFreed())).append('\n');
        sb.append("上次清理：").append(Util.fmtTime(s.lastClean())).append("\n\n最近 7 天：\n");
        for (Object[] d : s.recent7()) {
            sb.append(d[0]).append("  ").append(d[2]).append(" 项  ")
              .append(Util.fmtSize((Long) d[1])).append('\n');
        }
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                act.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("JunkClean", sb.toString()));
        act.toast("报告已复制到剪贴板");
    }
}
