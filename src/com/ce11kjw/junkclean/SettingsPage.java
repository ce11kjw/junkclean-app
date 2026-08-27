package com.ce11kjw.junkclean;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 设置：环境 / 主题 / 清理行为 / 分类开关 / 白名单 / 统计 / 关于 */
public class SettingsPage {

    private final MainActivity act;
    private ScrollView scroll;
    private EditText wlInput, rootInput;
    private TextView rootInfo, statInfo;
    private StatsChartView chart;
    private final List<Button> themeChips = new ArrayList<Button>();
    private final List<Button> accentChips = new ArrayList<Button>();

    public SettingsPage(MainActivity a) { this.act = a; }

    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        root.addView(envCard());
        root.addView(themeCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(behaviorCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(catCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(whitelistCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(statCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(aboutCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(UI.spacer(act, 24));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        refresh();
        return scroll;
    }

    // ---------- 运行环境 ----------

    private View envCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "运行环境"));
        rootInfo = UI.note(act, "");
        c.addView(rootInfo, UI.lpm(act, UI.MP, UI.WC, 6));
        Button test = UI.secondary(act, "测试 root 权限");
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean ok = Shell.testRoot();
                act.toast(ok ? "root 授权成功，可深度清理" : "未获得 root，运行在受限模式");
                refresh();
                act.homePage().refreshRootBadge();
            }
        });
        c.addView(test, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    // ---------- 主题 ----------

    private View themeCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "外观"));
        c.addView(UI.note(act, "切换后立即重建界面"));

        LinearLayout tRow = UI.row(act);
        tRow.addView(UI.text(act, "主题", 13, Theme.TEXT),
                new LinearLayout.LayoutParams(Theme.dp(act, 54), UI.WC));
        String[][] themes = {{"dark","深色"},{"oled","OLED"},{"light","浅色"}};
        String curTheme = act.store.theme();
        for (String[] t : themes) {
            final String key = t[0];
            Button b = UI.chip(act, t[1], key.equals(curTheme));
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.store.setTheme(key);
                    act.applyThemeAndRebuild();
                }
            });
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, Theme.dp(act, 30));
            lp.rightMargin = Theme.dp(act, 5);
            tRow.addView(b, lp);
            themeChips.add(b);
        }
        c.addView(tRow, UI.lpm(act, UI.MP, UI.WC, 10));

        LinearLayout aRow = UI.row(act);
        aRow.addView(UI.text(act, "强调色", 13, Theme.TEXT),
                new LinearLayout.LayoutParams(Theme.dp(act, 54), UI.WC));
        String[][] accents = {{"emerald","青绿"},{"violet","紫罗兰"},{"blue","蓝"},{"pink","粉"}};
        String curAccent = act.store.accent();
        for (String[] a : accents) {
            final String key = a[0];
            Button b = UI.chip(act, a[1], key.equals(curAccent));
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.store.setAccent(key);
                    act.applyThemeAndRebuild();
                }
            });
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, Theme.dp(act, 30));
            lp.rightMargin = Theme.dp(act, 5);
            aRow.addView(b, lp);
            accentChips.add(b);
        }
        c.addView(aRow, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    // ---------- 清理行为 ----------

    private View behaviorCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "清理行为"));

        c.addView(UI.switchRow(act, "先移入回收站",
                "sdcard 上的文件先进回收站，可恢复；系统缓存直接删除",
                act.store.toTrash(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                act.store.setToTrash(on);
            }
        }));

        LinearLayout dRow = UI.row(act);
        LinearLayout dInfo = UI.col(act);
        dInfo.addView(UI.text(act, "回收站保留天数", 13, Theme.TEXT));
        final TextView dVal = UI.note(act, daysLabel(act.store.trashDays()));
        dInfo.addView(dVal);
        dRow.addView(dInfo, new LinearLayout.LayoutParams(0, UI.WC, 1f));
        Button dBtn = UI.secondary(act, "修改");
        dBtn.setTextSize(12);
        dBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String[] labels = {"永久保留", "3 天", "7 天", "14 天", "30 天"};
                final int[] vals = {0, 3, 7, 14, 30};
                int cur = 0;
                for (int i = 0; i < vals.length; i++) if (vals[i] == act.store.trashDays()) cur = i;
                UI.pick(act, "回收站保留天数", labels, cur,
                        new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        act.store.setTrashDays(vals[w]);
                        dVal.setText(daysLabel(vals[w]));
                        d.dismiss();
                    }
                });
            }
        });
        dRow.addView(dBtn, UI.lp(Theme.dp(act, 60), Theme.dp(act, 34)));
        c.addView(dRow, UI.lpm(act, UI.MP, UI.WC, 6));

        c.addView(UI.note(act, "自定义扫描根目录（留空使用 sdcard 根）"),
                UI.lpm(act, UI.MP, UI.WC, 10));
        rootInput = UI.input(act, Util.sdRoot(), act.store.scanRoot());
        c.addView(rootInput, UI.lpm(act, UI.MP, Theme.dp(act, 40), 4));

        LinearLayout ops = UI.row(act);
        Button saveRoot = UI.primary(act, "保存扫描目录");
        Button clearCache = UI.secondary(act, "清除扫描缓存");
        saveRoot.setTextSize(12); clearCache.setTextSize(12);
        saveRoot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.store.setScanRoot(rootInput.getText().toString().trim());
                ScanEngine.invalidate();
                act.toast("已保存，下次扫描生效");
            }
        });
        clearCache.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ScanEngine.invalidate();
                act.toast("扫描缓存已清除");
            }
        });
        ops.addView(saveRoot, UI.weight(1f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ops.addView(clearCache, m);
        c.addView(ops, UI.lpm(act, UI.MP, UI.WC, 8));

        Button autoTrash = UI.secondary(act, "立即清理过期回收站项");
        autoTrash.setTextSize(12);
        autoTrash.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int d = act.store.trashDays();
                if (d <= 0) { act.toast("当前设置为永久保留"); return; }
                long f = Trash.autoClean(d);
                act.toast(f > 0 ? "已清理过期项 · 释放 " + Util.fmtSize(f) : "没有过期项目");
            }
        });
        c.addView(autoTrash, UI.lpm(act, UI.MP, Theme.dp(act, 40), 8));
        return c;
    }

    private String daysLabel(int d) {
        return d <= 0 ? "永久保留，不自动删除" : d + " 天后自动删除";
    }

    // ---------- 分类开关 ----------

    private View catCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "扫描分类"));
        c.addView(UI.note(act, "关闭的分类在首页扫描时会被跳过"));

        String[][] cats = {
                {"cache", "应用缓存"}, {"webview", "WebView 缓存"}, {"log", "日志文件"},
                {"temp", "临时文件"}, {"thumb", "缩略图缓存"}, {"apkjunk", "冗余安装包"},
                {"emptyjunk", "空文件"}, {"residue", "应用残留"}
        };
        for (String[] cat : cats) {
            final String id = cat[0];
            c.addView(UI.switchRow(act, cat[1], null, act.store.catEnabled(id),
                    new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                    act.store.setCatEnabled(id, on);
                    ScanEngine.invalidate();
                }
            }));
        }
        return c;
    }

    // ---------- 白名单 ----------

    private View whitelistCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "白名单"));
        c.addView(UI.note(act, "每行一个文件名或包名，扫描时跳过。列表项长按可快捷加入。"));
        wlInput = UI.multiline(act, "例如：\ncom.tencent.mm\nWeiXin", "", 4);
        c.addView(wlInput, UI.lpm(act, UI.MP, UI.WC, 10));

        LinearLayout ops = UI.row(act);
        Button save = UI.primary(act, "保存");
        Button clear = UI.secondary(act, "清空");
        save.setTextSize(12); clear.setTextSize(12);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                List<String> list = new ArrayList<String>();
                for (String s : wlInput.getText().toString().split("\n"))
                    if (!s.trim().isEmpty()) list.add(s.trim());
                act.store.setWhitelist(list);
                ScanEngine.invalidate();
                act.toast("已保存 " + list.size() + " 条白名单");
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.confirm(act, "清空白名单", "确认清空所有白名单条目？", new Runnable() {
                    public void run() {
                        act.store.setWhitelist(new ArrayList<String>());
                        wlInput.setText("");
                        ScanEngine.invalidate();
                        act.toast("白名单已清空");
                    }
                });
            }
        });
        ops.addView(save, UI.weight(1.4f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ops.addView(clear, m);
        c.addView(ops, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    // ---------- 统计 ----------

    private View statCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "清理统计"));
        statInfo = UI.note(act, "");
        c.addView(statInfo, UI.lpm(act, UI.MP, UI.WC, 6));

        c.addView(UI.h2(act, "最近 7 天"), UI.lpm(act, UI.MP, UI.WC, 12));
        chart = new StatsChartView(act);
        c.addView(chart, UI.lpm(act, UI.MP, UI.WC, 6));

        LinearLayout ops = UI.row(act);
        Button copy = UI.secondary(act, "复制报告");
        Button reset = UI.secondary(act, "重置统计");
        copy.setTextSize(12); reset.setTextSize(12);
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
        ops.addView(copy, UI.weight(1f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ops.addView(reset, m);
        c.addView(ops, UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
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

    // ---------- 关于 ----------

    private View aboutCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "关于"));
        c.addView(UI.note(act,
                "JunkClean v" + MainActivity.VERSION + "\n"
                + "Android 垃圾清理工具 · 深空玻璃 UI\n"
                + "纯原生 Java，无第三方依赖\n"
                + "有 root 深度清理，无 root 自动降级\n\n"
                + "App：github.com/ce11kjw/junkclean-app\n"
                + "模块：github.com/ce11kjw/junkclean"),
                UI.lpm(act, UI.MP, UI.WC, 6));
        return c;
    }

    // ---------- 刷新 ----------

    public void refresh() {
        if (rootInfo == null) return;
        boolean root = Shell.hasRoot();
        rootInfo.setText((root ? "✓ 已获得 root（" + Shell.detectManager() + "）"
                              : "⚠ 未检测到 root")
                + "\n清理模式：" + (root ? "深度（全应用缓存 + 系统日志）"
                                       : "受限（公共目录 + 外部缓存 + 自身缓存）")
                + "\n设备：" + android.os.Build.MODEL
                + " · Android " + android.os.Build.VERSION.RELEASE);

        Store s = act.store;
        statInfo.setText("累计清理：" + s.totalCount() + " 项\n"
                + "累计释放：" + Util.fmtSize(s.totalFreed()) + "\n"
                + "上次清理：" + Util.fmtTime(s.lastClean()));
        if (chart != null) chart.setData(s.recent7());

        StringBuilder wl = new StringBuilder();
        for (String w : s.whitelist()) {
            if (wl.length() > 0) wl.append('\n');
            wl.append(w);
        }
        if (wlInput != null) wlInput.setText(wl.toString());
        if (rootInput != null) rootInput.setText(s.scanRoot());
    }
}
