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

    // 大文件
    private EditText bigMin;
    private LinearLayout bigList;
    private TextView bigSum;
    private final List<JunkItem> bigItems = new ArrayList<JunkItem>();
    private String bigType = "all", bigSort = "size";
    private int bigDays = 0;
    private final List<Button> typeChips = new ArrayList<Button>();

    // 重复
    private LinearLayout dupList;
    private TextView dupSum, dupPolicyLabel;
    private List<Finder.DupGroup> dupGroups = new ArrayList<Finder.DupGroup>();
    private String keepPolicy = "newest";

    // 空文件
    private LinearLayout emptyList;
    private TextView emptySum;
    private final List<JunkItem> emptyItems = new ArrayList<JunkItem>();
    private boolean emptyDirs = true;

    // 安装包
    private LinearLayout apkList;
    private TextView apkSum;
    private List<Finder.ApkInfo> apkItems = new ArrayList<Finder.ApkInfo>();

    public FilesPage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;
        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        root.addView(UI.section(act, "大文件"));
        root.addView(bigCard());
        root.addView(UI.section(act, "重复文件"));
        root.addView(dupCard());
        root.addView(UI.section(act, "空文件与空目录"));
        root.addView(emptyCard());
        root.addView(UI.section(act, "安装包"));
        root.addView(apkCard());
        root.addView(UI.spacer(act, 24));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        return scroll;
    }

    // ---------- 大文件 ----------

    private View bigCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "按类型筛选、按体积或时间排序，可只看旧文件"));

        LinearLayout typeRow = UI.row(act);
        String[][] types = {{"all","全部"},{"img","图片"},{"vid","视频"},{"aud","音频"},
                            {"doc","文档"},{"zip","压缩"},{"apk","安装包"}};
        for (String[] t : types) {
            final String key = t[0];
            Button chip = UI.chip(act, t[1], "all".equals(key));
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    bigType = key;
                    for (Button b : typeChips) UI.setChipActive(act, b, b == v);
                    renderBig();
                }
            });
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, Theme.dp(act, 26));
            lp.rightMargin = Theme.dp(act, 5);
            typeRow.addView(chip, lp);
            typeChips.add(chip);
        }
        HorizontalScrollView hs = new HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(typeRow);
        c.addView(hs, UI.lpm(act, UI.MP, UI.WC, 10));

        LinearLayout ctl = UI.row(act);
        bigMin = UI.input(act, "阈值 MB", "50");
        bigMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        Button sortBtn = UI.secondary(act, "排序");
        Button daysBtn = UI.secondary(act, "时间");
        Button scan = UI.primary(act, "扫描");
        sortBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickSort(); }
        });
        daysBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickDays(); }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanBig(); }
        });
        ctl.addView(bigMin, UI.weight(1.1f, UI.BTN_H, act));
        for (Button b : new Button[]{sortBtn, daysBtn, scan}) {
            LinearLayout.LayoutParams lp = UI.weight(1f, UI.BTN_H, act);
            lp.leftMargin = Theme.dp(act, 6);
            ctl.addView(b, lp);
        }
        c.addView(ctl, UI.lpm(act, UI.MP, UI.WC, 8));

        bigSum = UI.note(act, "");
        c.addView(bigSum, UI.lpm(act, UI.MP, UI.WC, 8));
        bigList = UI.col(act);
        c.addView(bigList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button selAll = UI.secondary(act, "全选");
        Button trash = UI.secondary(act, "移入回收站");
        Button del = UI.danger(act, "彻底删除");
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { selectAll(bigItems, bigList, true); updateSum(bigItems, bigSum); }
        });
        trash.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(bigItems, bigList, bigSum, true); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(bigItems, bigList, bigSum, false); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, selAll, trash, del), UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    private void pickSort() {
        final String[] labels = {"体积从大到小", "时间从新到旧", "时间从旧到新", "名称"};
        final String[] keys = {"size", "new", "old", "name"};
        int cur = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(bigSort)) cur = i;
        UI.pick(act, "排序方式", labels, cur, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                bigSort = keys[w]; d.dismiss(); renderBig();
            }
        });
    }

    private void pickDays() {
        final String[] labels = {"不限时间", "7 天前", "30 天前", "90 天前", "180 天前"};
        final int[] vals = {0, 7, 30, 90, 180};
        int cur = 0;
        for (int i = 0; i < vals.length; i++) if (vals[i] == bigDays) cur = i;
        UI.pick(act, "只看多久前的文件", labels, cur,
                new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                bigDays = vals[w]; d.dismiss();
                act.toast(bigDays == 0 ? "不限时间，请重新扫描" : "只看 " + bigDays + " 天前，请重新扫描");
            }
        });
    }

    private void scanBig() {
        final long min;
        try { min = Long.parseLong(bigMin.getText().toString().trim()) * 1048576L; }
        catch (Exception e) { act.toast("阈值格式错误"); return; }
        bigList.removeAllViews();
        bigSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = Finder.big(scanRoot(), min, bigDays, wl(), 300);
                ui.post(new Runnable() {
                    public void run() {
                        bigItems.clear();
                        bigItems.addAll(found);
                        renderBig();
                    }
                });
            }
        }).start();
    }

    private void renderBig() {
        bigList.removeAllViews();
        List<JunkItem> show = new ArrayList<JunkItem>();
        for (JunkItem it : bigItems) if (Finder.matchType(it.name, bigType)) show.add(it);
        sortList(show, bigSort);
        if (show.isEmpty()) {
            bigSum.setText("");
            bigList.addView(UI.empty(act, "未发现符合条件的文件"));
            return;
        }
        long total = 0;
        for (JunkItem it : show) total += it.size;
        bigSum.setText(show.size() + " 项 · 合计 " + Util.fmtSize(total));
        Runnable onChange = new Runnable() {
            public void run() { updateSum(bigItems, bigSum); }
        };
        for (JunkItem it : show) bigList.addView(UI.fileRow(act, it, onChange, whitelistAction(it)));
    }

    private void sortList(List<JunkItem> l, String mode) {
        Comparator<JunkItem> cmp;
        if ("new".equals(mode)) {
            cmp = new Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return Long.compare(b.mtime, a.mtime); }
            };
        } else if ("old".equals(mode)) {
            cmp = new Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return Long.compare(a.mtime, b.mtime); }
            };
        } else if ("name".equals(mode)) {
            cmp = new Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return a.name.compareToIgnoreCase(b.name); }
            };
        } else {
            cmp = new Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
            };
        }
        Collections.sort(l, cmp);
    }

    // ---------- 重复文件 ----------

    private View dupCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "大小分桶 + 内容哈希精确比对，同组按策略保留一份"));

        dupPolicyLabel = UI.note(act, "当前策略：保留最新");
        c.addView(dupPolicyLabel, UI.lpm(act, UI.MP, UI.WC, 8));

        Button policy = UI.secondary(act, "保留策略");
        Button scan = UI.primary(act, "扫描重复");
        policy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickPolicy(); }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanDup(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, policy, scan), UI.lpm(act, UI.MP, UI.WC, 6));

        dupSum = UI.note(act, "");
        c.addView(dupSum, UI.lpm(act, UI.MP, UI.WC, 8));
        dupList = UI.col(act);
        c.addView(dupList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button del = UI.danger(act, "删除勾选的副本");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delDup(); }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 10));
        return c;
    }

    private void pickPolicy() {
        final String[] labels = {"保留最新", "保留最旧", "保留路径最短", "保留体积最大"};
        final String[] keys = {"newest", "oldest", "shortest", "largest"};
        int cur = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(keepPolicy)) cur = i;
        UI.pick(act, "保留策略", labels, cur, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                keepPolicy = keys[w];
                dupPolicyLabel.setText("当前策略：" + labels[w]);
                d.dismiss();
                if (!dupGroups.isEmpty()) {
                    Finder.applyKeepPolicy(dupGroups, keepPolicy);
                    renderDup();
                }
            }
        });
    }

    private void scanDup() {
        dupList.removeAllViews();
        dupSum.setText("扫描中（计算哈希，稍慢）…");
        new Thread(new Runnable() {
            public void run() {
                final List<Finder.DupGroup> gs = Finder.duplicates(scanRoot(), 65536, 40, wl());
                Finder.applyKeepPolicy(gs, keepPolicy);
                ui.post(new Runnable() {
                    public void run() { dupGroups = gs; renderDup(); }
                });
            }
        }).start();
    }

    private void renderDup() {
        dupList.removeAllViews();
        if (dupGroups.isEmpty()) {
            dupSum.setText("");
            dupList.addView(UI.empty(act, "未发现重复文件"));
            return;
        }
        long waste = 0;
        for (Finder.DupGroup g : dupGroups) waste += g.size * (g.files.size() - 1);
        dupSum.setText(dupGroups.size() + " 组 · 可回收约 " + Util.fmtSize(waste));

        Runnable noop = new Runnable() { public void run() { updateDupSum(); } };
        for (Finder.DupGroup g : dupGroups) {
            LinearLayout box = UI.col(act);
            box.setBackground(Theme.inner(act, 12));
            int p = Theme.dp(act, 10);
            box.setPadding(p, p, p, p);

            LinearLayout head = UI.row(act);
            TextView nm = UI.text(act, g.name, 12, Theme.TEXT);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            head.addView(nm, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            head.addView(UI.text(act, g.files.size() + " 份 · " + Util.fmtSize(g.size), 11, Theme.ACCENT));
            box.addView(head);

            for (JunkItem it : g.files) {
                LinearLayout r = UI.fileRow(act, it, noop, null);
                if (!it.checked) {
                    r.addView(UI.badge(act, "保留", Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)));
                }
                box.addView(r);
            }
            dupList.addView(box, UI.lpm(act, UI.MP, UI.WC, 8));
        }
    }

    private void updateDupSum() {
        int n = 0;
        long sz = 0;
        for (Finder.DupGroup g : dupGroups)
            for (JunkItem it : g.files) if (it.checked) { n++; sz += it.size; }
        dupSum.setText(dupGroups.size() + " 组 · 已选 " + n + " 个副本 · " + Util.fmtSize(sz));
    }

    private void delDup() {
        final List<JunkItem> sel = new ArrayList<JunkItem>();
        for (Finder.DupGroup g : dupGroups)
            for (JunkItem it : g.files) if (it.checked) sel.add(it);
        if (sel.isEmpty()) { act.toast("未选中副本"); return; }
        long total = 0;
        for (JunkItem it : sel) total += it.size;
        final long ft = total;
        final boolean toTrash = act.store.toTrash();
        UI.confirm(act, "删除重复副本",
                "将删除 " + sel.size() + " 个副本，约 " + Util.fmtSize(ft)
                + (toTrash ? "\n先移入回收站，可恢复。" : "\n直接删除，不可恢复！"),
                new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        final CleanEngine.Result r = new CleanEngine(toTrash).cleanItems(sel);
                        ui.post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                ScanEngine.invalidate();
                                String msg = "已处理 " + r.count + " 个副本 · "
                                        + Util.fmtSize(toTrash ? r.trashed : r.freed);
                                if (!r.errors.isEmpty()) msg += " · " + r.errors.size() + " 项失败";
                                act.toast(msg);
                                act.homePage().refreshDisk();
                                scanDup();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    // ---------- 空文件 ----------

    private View emptyCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.switchRow(act, "包含空目录", "同时列出没有内容的文件夹", emptyDirs,
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { emptyDirs = on; }
        }));

        Button scan = UI.primary(act, "扫描");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanEmpty(); }
        });
        c.addView(scan, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 8));

        emptySum = UI.note(act, "");
        c.addView(emptySum, UI.lpm(act, UI.MP, UI.WC, 8));
        emptyList = UI.col(act);
        c.addView(emptyList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button selAll = UI.secondary(act, "全选");
        Button del = UI.danger(act, "清理选中");
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { selectAll(emptyItems, emptyList, true); updateSum(emptyItems, emptySum); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(emptyItems, emptyList, emptySum, false); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, selAll, del), UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    private void scanEmpty() {
        emptyList.removeAllViews();
        emptySum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = Finder.empties(scanRoot(), emptyDirs, 250, wl());
                ui.post(new Runnable() {
                    public void run() {
                        emptyItems.clear();
                        emptyItems.addAll(found);
                        if (emptyItems.isEmpty()) {
                            emptyList.removeAllViews();
                            emptySum.setText("");
                            emptyList.addView(UI.empty(act, "未发现空文件"));
                        } else {
                            rebuild(emptyItems, emptyList, emptySum);
                        }
                    }
                });
            }
        }).start();
    }

    // ---------- 安装包 ----------

    private View apkCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "标注是否已安装；已安装的 apk 可安全删除"));

        Button scan = UI.primary(act, "扫描");
        Button selIns = UI.secondary(act, "只选已安装");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanApk(); }
        });
        selIns.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (Finder.ApkInfo a : apkItems) a.checked = a.installed;
                renderApk();
            }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, scan, selIns), UI.lpm(act, UI.MP, UI.WC, 8));

        apkSum = UI.note(act, "");
        c.addView(apkSum, UI.lpm(act, UI.MP, UI.WC, 8));
        apkList = UI.col(act);
        c.addView(apkList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button del = UI.danger(act, "删除选中安装包");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delApk(); }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 10));
        return c;
    }

    private void scanApk() {
        apkList.removeAllViews();
        apkSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<Finder.ApkInfo> found = Finder.apks(act, scanRoot(), wl());
                ui.post(new Runnable() {
                    public void run() { apkItems = found; renderApk(); }
                });
            }
        }).start();
    }

    private void renderApk() {
        apkList.removeAllViews();
        if (apkItems.isEmpty()) {
            apkSum.setText("");
            apkList.addView(UI.empty(act, "未发现安装包"));
            return;
        }
        long total = 0, insTotal = 0;
        int ins = 0;
        for (Finder.ApkInfo a : apkItems) {
            total += a.size;
            if (a.installed) { ins++; insTotal += a.size; }
        }
        apkSum.setText(apkItems.size() + " 个 · " + Util.fmtSize(total)
                + " · 已装 " + ins + " 个（" + Util.fmtSize(insTotal) + " 可回收）");

        for (final Finder.ApkInfo a : apkItems) {
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));
            CheckBox cb = UI.check(act, a.checked);
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { a.checked = on; }
            });
            r.addView(cb);
            TextView nm = UI.text(act, a.label, 12, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            np.leftMargin = Theme.dp(act, 4);
            r.addView(nm, np);
            if (a.installed) r.addView(UI.badge(act, "已装", Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)));
            else if (a.pkg == null) r.addView(UI.badge(act, "未知", Theme.DIM, Theme.alpha(Theme.DIM, 0x22)));
            else r.addView(UI.badge(act, "未装", Theme.WARN, Theme.alpha(Theme.WARN, 0x22)));
            TextView sz = UI.text(act, Util.fmtSize(a.size), 11.5f, Theme.DIM);
            LinearLayout.LayoutParams sp = UI.lp(UI.WC, UI.WC);
            sp.leftMargin = Theme.dp(act, 6);
            r.addView(sz, sp);
            apkList.addView(r);
        }
    }

    private void delApk() {
        final List<JunkItem> sel = new ArrayList<JunkItem>();
        int unins = 0;
        for (Finder.ApkInfo a : apkItems) {
            if (!a.checked) continue;
            sel.add(new JunkItem(a.path, a.label, a.size));
            if (!a.installed) unins++;
        }
        if (sel.isEmpty()) { act.toast("未选中安装包"); return; }
        long total = 0;
        for (JunkItem it : sel) total += it.size;
        String warn = unins > 0 ? "\n\n⚠ 其中 " + unins + " 个尚未安装，删除后需重新下载。" : "";
        final boolean toTrash = act.store.toTrash();
        UI.confirm(act, "删除安装包",
                "将删除 " + sel.size() + " 个 apk，约 " + Util.fmtSize(total) + warn,
                new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        final CleanEngine.Result r = new CleanEngine(toTrash).cleanItems(sel);
                        ui.post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                act.toast("已处理 " + r.count + " 个 · "
                                        + Util.fmtSize(toTrash ? r.trashed : r.freed));
                                act.homePage().refreshDisk();
                                scanApk();
                            }
                        });
                    }
                }).start();
            }
        });
    }
}
