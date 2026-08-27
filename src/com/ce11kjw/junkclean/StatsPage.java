package com.ce11kjw.junkclean;

import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
        rk.addView(UI.note(act, "扫描根目录下一级目录占用，找出空间大户"));
        Button rankBtn = UI.primary(act, "开始统计");
        rankBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { loadRank(); }
        });
        rk.addView(rankBtn, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 10));
        rankBox = UI.col(act);
        rk.addView(rankBox, UI.lpm(act, UI.MP, UI.WC, 8));
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
        rankBox.addView(UI.note(act, "统计中…"));
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> list = Finder.dirRank(scanRoot(), 12, wl());
                ui.post(new Runnable() {
                    public void run() {
                        rankBox.removeAllViews();
                        if (list.isEmpty()) { rankBox.addView(UI.empty(act, "无数据")); return; }
                        long max = list.get(0).size;
                        for (JunkItem it : list) {
                            LinearLayout r = UI.row(act);
                            r.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
                            TextView nm = UI.text(act, it.name, 11.5f, Theme.MUTED);
                            nm.setSingleLine(true);
                            nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
                            r.addView(nm, new LinearLayout.LayoutParams(Theme.dp(act, 90), UI.WC));
                            StorageBarView b = new StorageBarView(act);
                            b.setPercent(max > 0 ? it.size * 100f / max : 0);
                            LinearLayout.LayoutParams bp =
                                    new LinearLayout.LayoutParams(0, Theme.dp(act, 8), 1f);
                            bp.leftMargin = bp.rightMargin = Theme.dp(act, 8);
                            r.addView(b, bp);
                            r.addView(UI.text(act, Util.fmtSize(it.size), 11, Theme.DIM));
                            rankBox.addView(r);
                        }
                    }
                });
            }
        }).start();
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
