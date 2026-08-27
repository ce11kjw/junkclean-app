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
    private TextView overview, diskInfo;
    private StatsChartView chart;
    private LinearLayout rankBox, dayList;

    public StatsPage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        root.addView(UI.section(act, "总览"));
        LinearLayout ov = UI.card(act);
        overview = UI.note(act, "");
        ov.addView(overview);
        diskInfo = UI.note(act, "");
        ov.addView(diskInfo, UI.lpm(act, UI.MP, UI.WC, 8));
        root.addView(ov);

        root.addView(UI.section(act, "最近 7 天"));
        LinearLayout ch = UI.card(act);
        chart = new StatsChartView(act);
        ch.addView(chart);
        dayList = UI.col(act);
        ch.addView(dayList, UI.lpm(act, UI.MP, UI.WC, 10));
        root.addView(ch);

        root.addView(UI.section(act, "目录体积排行"));
        LinearLayout rk = UI.card(act);
        rk.addView(UI.note(act, "统计目录占用并展开大目录的下一级，点击条目可看完整路径"));
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
        root.addView(rk);

        root.addView(UI.section(act, "数据管理"));
        LinearLayout dm = UI.card(act);
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
                        refresh();
                        act.homePage().refreshStat();
                        act.toast("统计已重置");
                    }
                });
            }
        });
        dm.addView(UI.btnRow(act, UI.BTN_H, copy, reset));
        root.addView(dm);
        root.addView(UI.spacer(act, 24));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        refresh();
        return scroll;
    }

    public void refresh() {
        if (overview == null) return;
        Store s = act.store;
        overview.setText("累计清理：" + s.totalCount() + " 项\n"
                + "累计释放：" + Util.fmtSize(s.totalFreed()) + "\n"
                + "上次清理：" + Util.fmtTime(s.lastClean()));

        try {
            StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            diskInfo.setText("存储：已用 " + Util.fmtSize(total - free)
                    + " / " + Util.fmtSize(total) + " · 可用 " + Util.fmtSize(free));
        } catch (Exception e) {
            diskInfo.setText("");
        }

        List<Object[]> days = s.recent7();
        chart.setData(days);
        dayList.removeAllViews();
        for (Object[] d : days) {
            long freed = ((Long) d[1]).longValue();
            int cnt = ((Integer) d[2]).intValue();
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 3), 0, Theme.dp(act, 3));
            r.addView(UI.text(act, (String) d[0], 11.5f, Theme.MUTED),
                    new LinearLayout.LayoutParams(Theme.dp(act, 84), UI.WC));
            TextView c = UI.text(act, cnt > 0 ? cnt + " 项" : "—", 11.5f, Theme.DIM);
            r.addView(c, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            r.addView(UI.text(act, freed > 0 ? Util.fmtSize(freed) : "",
                    11.5f, freed > 0 ? Theme.ACCENT : Theme.DIM));
            dayList.addView(r);
        }
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
            TextView nm = UI.text(act, it.name, 11.5f, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            r.addView(nm, new LinearLayout.LayoutParams(Theme.dp(act, 104), UI.WC));
            StorageBarView b = new StorageBarView(act);
            b.setPercent(max > 0 ? it.size * 100f / max : 0);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, Theme.dp(act, 8), 1f);
            bp.leftMargin = bp.rightMargin = Theme.dp(act, 8);
            r.addView(b, bp);
            r.addView(UI.text(act, Util.fmtSize(it.size), 11, Theme.DIM));
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    UI.info(act, it.name, "完整路径：\n" + it.path
                            + "\n\n占用：" + Util.fmtSize(it.size)
                            + "\n\n可到「文件」页的文件清理中打开该目录进行处理。");
                }
            });
            rankBox.addView(r);
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
