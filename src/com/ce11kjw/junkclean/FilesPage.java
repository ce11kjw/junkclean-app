package com.ce11kjw.junkclean;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 文件页：大文件 / 重复文件 / 空文件 / 安装包 */
public class FilesPage extends PageBase {

    private ScrollView scroll;

    // 定时清理目录
    private android.widget.EditText brPath;
    private LinearLayout savedDirList;


    public FilesPage(MainActivity a) {
        super(a);
    }

    @Override
    public View view() {
        if (scroll != null) return scroll;
        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, Theme.S4);
        root.setPadding(p, Theme.dp(act, Theme.S5), p, p);

        LinearLayout head = UI.col(act);
        head.addView(UI.eyebrow(act, "存储"));
        TextView ht = UI.display(act, "定时清理", Theme.T_TITLE, Theme.TEXT);
        ht.setTypeface(Theme.display(), android.graphics.Typeface.BOLD);
        head.addView(ht, UI.lpm(act, UI.WC, UI.WC, 2));
        root.addView(head);

        root.addView(UI.section(act, "定时清理目录"));
        root.addView(scheduleCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.spacer(act, Theme.S8));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        return scroll;
    }

    private View scheduleCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "定时清理"));
        c.addView(UI.title(act, "填目录 → 保存 → 到点自动清"), UI.lpm(act, UI.MP, UI.WC, 2));

        // 定时开关
        c.addView(UI.switchRow(act, "启用定时清理",
                "到点自动清理下面保存的目录，平时不用管",
                act.store.scheduleEnabled(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                act.store.setScheduleEnabled(on);
                ScheduleManager.apply(act, act.store);
                act.toast(on ? "已开启定时清理" : "已关闭");
            }
        }), UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        // 间隔选择
        LinearLayout intRow = UI.row(act);
        intRow.addView(UI.text(act, "清理间隔", Theme.T_BODY, Theme.MUTED),
                UI.weight(1f, UI.WC, act));
        final TextView intVal = UI.data(act, fmtInterval(act.store.scheduleIntervalMin()),
                Theme.T_DATA_S, Theme.ACCENT);
        Button intBtn = UI.chip(act, "更改", false);
        intBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickInterval(intVal); }
        });
        intRow.addView(intVal);
        LinearLayout.LayoutParams ibp = UI.lp(UI.WC, Theme.dp(act, 28));
        ibp.leftMargin = Theme.dp(act, Theme.S2);
        intRow.addView(intBtn, ibp);
        c.addView(intRow, UI.lpm(act, UI.MP, UI.WC, Theme.S3));

        // 添加目录
        LinearLayout addRow = UI.row(act);
        brPath = UI.input(act, Util.sdRoot() + "/Download/temp", "");
        brPath.setTypeface(Theme.data());
        brPath.setTextSize(Theme.T_DATA_S);
        addRow.addView(brPath, UI.weight(1f, UI.BTN_H, act));
        Button add = UI.primary(act, "＋ 添加");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveCurrentDir(); }
        });
        LinearLayout.LayoutParams ap = UI.lp(Theme.dp(act, 72), Theme.dp(act, UI.BTN_H));
        ap.leftMargin = Theme.dp(act, Theme.S2);
        addRow.addView(add, ap);
        c.addView(addRow, UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        c.addView(UI.note(act, "路径末尾带 / = 只清里面文件保留目录；不带 / = 删整个目录"),
                UI.lpm(act, UI.MP, UI.WC, Theme.S3));

        // 已保存列表
        c.addView(UI.eyebrow(act, "已保存目录"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        savedDirList = UI.col(act);
        c.addView(savedDirList, UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        renderSavedCleanDirs();

        // 立即清理全部
        Button runNow = UI.danger(act, "立即清理全部");
        runNow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { runSavedDirsNow(); }
        });
        c.addView(runNow, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));
        return c;
    }

    private String fmtInterval(int min) {
        if (min < 60) return min + " 分钟";
        if (min % 1440 == 0) return (min / 1440) + " 天";
        if (min % 60 == 0) return (min / 60) + " 小时";
        return min + " 分钟";
    }

    private void pickInterval(final TextView label) {
        final int[] opts = {30, 60, 120, 360, 720, 1440};
        final String[] names = {"30 分钟", "1 小时", "2 小时", "6 小时", "12 小时", "1 天"};
        int cur = 0;
        for (int i = 0; i < opts.length; i++) if (opts[i] == act.store.scheduleIntervalMin()) cur = i;
        new android.app.AlertDialog.Builder(act)
                .setTitle("清理间隔")
                .setSingleChoiceItems(names, cur, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                act.store.setScheduleIntervalMin(opts[w]);
                ScheduleManager.apply(act, act.store);
                label.setText(fmtInterval(opts[w]));
                d.dismiss();
            }
        }).show();
    }

    private void saveCurrentDir() {
        String path = brPath.getText().toString().trim();
        if (path.isEmpty()) { act.toast("请输入目录路径"); return; }
        boolean del = !path.endsWith("/") && !path.endsWith("/ ");
        String cleanPath = path.replaceAll("/+$", "");
        java.io.File f = new java.io.File(cleanPath);
        if (!f.isAbsolute()) f = new java.io.File(Util.sdRoot(), cleanPath);
        if (!f.isDirectory()) { act.toast("目录不存在：" + f.getAbsolutePath()); return; }
        act.store.saveCleanDir(f.getAbsolutePath(), del);
        brPath.setText("");
        renderSavedCleanDirs();
        act.toast("已添加，" + (del ? "到点删整个目录" : "到点只清内容"));
    }

    private void renderSavedCleanDirs() {
        savedDirList.removeAllViews();
        List<String> dirs = act.store.savedCleanDirs();
        if (dirs.isEmpty()) {
            savedDirList.addView(UI.note(act, "暂无保存的目录，查看后点「保存到定时」"));
            return;
        }
        for (int i = 0; i < dirs.size(); i++) {
            final int idx = i;
            String[] parts = dirs.get(i).split("\\|", 2);
            String path = parts.length > 0 ? parts[0] : "";
            boolean del = parts.length > 1 && "1".equals(parts[1]);
            LinearLayout row = UI.row(act);
            row.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
            TextView nm = UI.data(act, Util.shortPath(path) + (del ? "（整个）" : "（内容）"),
                    Theme.T_DATA_S, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            row.addView(nm, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            Button delBtn = UI.danger(act, "×");
            delBtn.setTextSize(13);
            LinearLayout.LayoutParams dp = UI.lp(Theme.dp(act, 32), Theme.dp(act, 28));
            delBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { act.store.removeCleanDir(idx); renderSavedCleanDirs(); }
            });
            row.addView(delBtn, dp);
            savedDirList.addView(row);
        }
    }

    /** 立即执行已保存目录清理 */
    private void runSavedDirsNow() {
        final List<String> dirs = act.store.savedCleanDirs();
        if (dirs.isEmpty()) { act.toast("没有已保存的目录"); return; }
        act.toast("开始清理 " + dirs.size() + " 个目录…");
        new Thread(new Runnable() {
            public void run() {
                long freed = 0; int cnt = 0;
                CleanEngine eng = new CleanEngine(false);
                for (int i = 0; i < dirs.size(); i++) {
                    String line = dirs.get(i);
                    String[] parts = line.split("\\|", 2);
                    String path = parts.length > 0 ? parts[0] : "";
                    if (path.isEmpty()) continue;
                    java.io.File dir = new java.io.File(path);
                    if (!dir.exists()) continue;
                    boolean delItself = parts.length > 1 && "1".equals(parts[1]);
                    if (delItself) {
                        CleanEngine.Result r = eng.cleanItems(java.util.Collections.singletonList(
                                new JunkItem(path, dir.getName(), 0)));
                        freed += r.freed; cnt += r.count;
                        act.store.removeCleanDir(i);
                    } else {
                        java.io.File[] kids = dir.listFiles();
                        if (kids != null) {
                            java.util.List<JunkItem> items = new java.util.ArrayList<JunkItem>();
                            for (java.io.File k : kids)
                                items.add(new JunkItem(k.getAbsolutePath(), k.getName(), 0));
                            CleanEngine.Result r = eng.cleanItems(items);
                            freed += r.freed; cnt += r.count;
                        }
                    }
                }
                final long f = freed; final int n = cnt;
                post(new Runnable() {
                    public void run() {
                        renderSavedCleanDirs();
                        act.store.addStat(f, n);
                        ScanEngine.invalidate();
                        act.toast("清理完成 · " + n + " 项 · " + Util.fmtSize(f));
                        act.homePage().refreshDisk();
                    }
                });
            }
        }).start();
    }

}


