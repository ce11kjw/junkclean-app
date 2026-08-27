package com.ce11kjw.junkclean;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 工具箱：8 张大卡片 */
public class ToolsPage {

    final MainActivity act;
    final Handler ui = new Handler(Looper.getMainLooper());
    private ScrollView scroll;

    // 大文件
    private EditText bigMin;
    private LinearLayout bigList;
    private TextView bigSum;
    private final List<JunkItem> bigItems = new ArrayList<JunkItem>();
    private String bigType = "all";
    private String bigSort = "size";
    private int bigDays = 0;
    private final List<Button> typeChips = new ArrayList<Button>();

    // 空文件
    private LinearLayout emptyList;
    private TextView emptySum;
    private final List<JunkItem> emptyItems = new ArrayList<JunkItem>();
    private boolean emptyDirs = true;

    // 重复
    private LinearLayout dupList;
    private TextView dupSum;
    private List<Finder.DupGroup> dupGroups = new ArrayList<Finder.DupGroup>();
    private String keepPolicy = "newest";

    // 应用缓存
    private LinearLayout appList;
    private TextView appSum;
    private List<Finder.AppCache> appItems = new ArrayList<Finder.AppCache>();

    // APK
    private LinearLayout apkList;
    private TextView apkSum;
    private List<Finder.ApkInfo> apkItems = new ArrayList<Finder.ApkInfo>();

    // 缩略图
    private LinearLayout thumbList;
    private TextView thumbSum;
    private final List<JunkItem> thumbItems = new ArrayList<JunkItem>();

    // 回收站
    private LinearLayout trashList;
    private TextView trashSum;
    private List<Trash.Item> trashItems = new ArrayList<Trash.Item>();

    // 整理
    private LinearLayout ruleBox;
    private TextView orgSum;

    // fstrim
    private TextView trimResult;

    public ToolsPage(MainActivity a) { this.act = a; }

    public View view() {
        if (scroll != null) return scroll;
        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        root.addView(bigCard());
        root.addView(dupCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(emptyCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(appCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(apkCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(thumbCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(organizeCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(trashCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(trimCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(UI.spacer(act, 24));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        return scroll;
    }

    // ================= 大文件 =================

    private View bigCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "📁  大文件清理"));
        c.addView(UI.note(act, "按类型筛选 / 按体积或时间排序 / 可只看旧文件"));

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
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, Theme.dp(act, 30));
            lp.rightMargin = Theme.dp(act, 5);
            typeRow.addView(chip, lp);
            typeChips.add(chip);
        }
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(typeRow);
        c.addView(hs, UI.lpm(act, UI.MP, UI.WC, 10));

        LinearLayout ctl = UI.row(act);
        bigMin = UI.input(act, "阈值 MB", "50");
        bigMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        Button sortBtn = UI.secondary(act, "排序");
        Button daysBtn = UI.secondary(act, "时间");
        Button scan = UI.primary(act, "扫描");
        sortBtn.setTextSize(12); daysBtn.setTextSize(12);
        sortBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickSort(); }
        });
        daysBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickDays(); }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanBig(); }
        });
        ctl.addView(bigMin, UI.weight(1.2f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ctl.addView(sortBtn, m);
        LinearLayout.LayoutParams m2 = UI.weight(1f, 40, act);
        m2.leftMargin = Theme.dp(act, 6);
        ctl.addView(daysBtn, m2);
        LinearLayout.LayoutParams m3 = UI.weight(1f, 40, act);
        m3.leftMargin = Theme.dp(act, 6);
        ctl.addView(scan, m3);
        c.addView(ctl, UI.lpm(act, UI.MP, UI.WC, 10));

        bigSum = UI.note(act, "");
        c.addView(bigSum, UI.lpm(act, UI.MP, UI.WC, 8));
        bigList = UI.col(act);
        c.addView(bigList, UI.lpm(act, UI.MP, UI.WC, 4));

        LinearLayout ops = UI.row(act);
        Button selAll = UI.secondary(act, "全选");
        Button trash = UI.secondary(act, "移入回收站");
        Button del = UI.danger(act, "彻底删除");
        selAll.setTextSize(12); trash.setTextSize(12); del.setTextSize(12);
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { selectAll(bigItems, bigList, true); }
        });
        trash.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(bigItems, bigList, bigSum, true); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(bigItems, bigList, bigSum, false); }
        });
        ops.addView(selAll, UI.weight(1f, 42, act));
        LinearLayout.LayoutParams o1 = UI.weight(1.4f, 42, act);
        o1.leftMargin = Theme.dp(act, 6);
        ops.addView(trash, o1);
        LinearLayout.LayoutParams o2 = UI.weight(1.2f, 42, act);
        o2.leftMargin = Theme.dp(act, 6);
        ops.addView(del, o2);
        c.addView(ops, UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    private void pickSort() {
        final String[] labels = {"体积从大到小", "时间从新到旧", "时间从旧到新", "名称"};
        final String[] keys = {"size", "new", "old", "name"};
        int cur = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(bigSort)) cur = i;
        UI.pick(act, "排序方式", labels, cur, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                bigSort = keys[w];
                d.dismiss();
                renderBig();
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
                bigDays = vals[w];
                d.dismiss();
                act.toast(bigDays == 0 ? "不限时间" : "只看 " + bigDays + " 天前，请重新扫描");
            }
        });
    }

    private void scanBig() {
        final long min;
        try {
            min = Long.parseLong(bigMin.getText().toString().trim()) * 1048576L;
        } catch (Exception e) { act.toast("阈值格式错误"); return; }
        bigList.removeAllViews();
        bigSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = Finder.big(scanRoot(), min, bigDays,
                        act.store.whitelist(), 300);
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
        Runnable onChange = new Runnable() { public void run() { updateSum(bigItems, bigSum); } };
        for (final JunkItem it : show) {
            bigList.addView(UI.fileRow(act, it, onChange, whitelistAction(it)));
        }
    }

    private void sortList(List<JunkItem> l, String mode) {
        java.util.Comparator<JunkItem> cmp;
        if ("new".equals(mode)) {
            cmp = new java.util.Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return Long.compare(b.mtime, a.mtime); }
            };
        } else if ("old".equals(mode)) {
            cmp = new java.util.Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return Long.compare(a.mtime, b.mtime); }
            };
        } else if ("name".equals(mode)) {
            cmp = new java.util.Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return a.name.compareToIgnoreCase(b.name); }
            };
        } else {
            cmp = new java.util.Comparator<JunkItem>() {
                public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
            };
        }
        java.util.Collections.sort(l, cmp);
    }

    // ================= 重复文件 =================

    private View dupCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🔁  重复文件"));
        c.addView(UI.note(act, "大小分桶 + 内容哈希精确比对，可选保留策略"));

        LinearLayout ctl = UI.row(act);
        Button policy = UI.secondary(act, "保留策略");
        Button scan = UI.primary(act, "扫描重复");
        policy.setTextSize(12); scan.setTextSize(12);
        policy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickPolicy(); }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanDup(); }
        });
        ctl.addView(policy, UI.weight(1f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ctl.addView(scan, m);
        c.addView(ctl, UI.lpm(act, UI.MP, UI.WC, 10));

        dupSum = UI.note(act, "");
        c.addView(dupSum, UI.lpm(act, UI.MP, UI.WC, 8));
        dupList = UI.col(act);
        c.addView(dupList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button del = UI.danger(act, "删除勾选的重复副本");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delDup(); }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void pickPolicy() {
        final String[] labels = {"保留最新", "保留最旧", "保留路径最短"};
        final String[] keys = {"newest", "oldest", "shortest"};
        int cur = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(keepPolicy)) cur = i;
        UI.pick(act, "保留策略", labels, cur, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                keepPolicy = keys[w];
                d.dismiss();
                if (!dupGroups.isEmpty()) {
                    Finder.applyKeepPolicy(dupGroups, keepPolicy);
                    renderDup();
                }
                act.toast("策略：" + labels[w]);
            }
        });
    }

    private void scanDup() {
        dupList.removeAllViews();
        dupSum.setText("扫描中（计算哈希，稍慢）…");
        new Thread(new Runnable() {
            public void run() {
                final List<Finder.DupGroup> gs = Finder.duplicates(scanRoot(), 65536, 40);
                Finder.applyKeepPolicy(gs, keepPolicy);
                ui.post(new Runnable() {
                    public void run() {
                        dupGroups = gs;
                        renderDup();
                    }
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
        dupSum.setText(dupGroups.size() + " 组重复 · 可回收约 " + Util.fmtSize(waste));

        Runnable onChange = new Runnable() { public void run() {} };
        for (Finder.DupGroup g : dupGroups) {
            LinearLayout box = UI.col(act);
            box.setPadding(0, Theme.dp(act, 6), 0, Theme.dp(act, 6));
            LinearLayout head = UI.row(act);
            TextView nm = UI.text(act, g.name, 12.5f, Theme.TEXT);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            head.addView(nm, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            head.addView(UI.text(act, g.files.size() + " 份 · " + Util.fmtSize(g.size), 11, Theme.ACCENT));
            box.addView(head);
            for (JunkItem it : g.files) {
                LinearLayout r = UI.fileRow(act, it, onChange, null);
                if (!it.checked) {
                    r.addView(UI.badge(act, "保留", Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)));
                }
                box.addView(r);
            }
            box.addView(UI.divider(act));
            dupList.addView(box);
        }
    }

    private void delDup() {
        final List<JunkItem> sel = new ArrayList<JunkItem>();
        for (Finder.DupGroup g : dupGroups)
            for (JunkItem it : g.files) if (it.checked) sel.add(it);
        if (sel.isEmpty()) { act.toast("未选中副本"); return; }
        long total = 0;
        for (JunkItem it : sel) total += it.size;
        final long ft = total;
        UI.confirm(act, "删除重复副本",
                "将删除 " + sel.size() + " 个副本，约 " + Util.fmtSize(ft)
                + (act.store.toTrash() ? "\n先移入回收站，可恢复。" : "\n直接删除，不可恢复！"),
                new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        final CleanEngine.Result r = new CleanEngine(act.store.toTrash()).cleanItems(sel);
                        ui.post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                act.toast("已删除 " + r.count + " 个副本 · 释放 " + Util.fmtSize(r.freed));
                                scanDup();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    // ================= 空文件 =================

    private View emptyCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🫙  空文件与空目录"));
        c.addView(UI.switchRow(act, "包含空目录", "同时列出没有内容的文件夹", emptyDirs,
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { emptyDirs = on; }
        }));

        Button scan = UI.primary(act, "扫描");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanEmpty(); }
        });
        c.addView(scan, UI.lpm(act, UI.MP, Theme.dp(act, 42), 8));

        emptySum = UI.note(act, "");
        c.addView(emptySum, UI.lpm(act, UI.MP, UI.WC, 8));
        emptyList = UI.col(act);
        c.addView(emptyList, UI.lpm(act, UI.MP, UI.WC, 4));

        LinearLayout ops = UI.row(act);
        Button selAll = UI.secondary(act, "全选");
        Button del = UI.danger(act, "清理选中");
        selAll.setTextSize(12); del.setTextSize(12);
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { selectAll(emptyItems, emptyList, true); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(emptyItems, emptyList, emptySum, false); }
        });
        ops.addView(selAll, UI.weight(1f, 42, act));
        LinearLayout.LayoutParams m = UI.weight(1.6f, 42, act);
        m.leftMargin = Theme.dp(act, 6);
        ops.addView(del, m);
        c.addView(ops, UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    private void scanEmpty() {
        emptyList.removeAllViews();
        emptySum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = Finder.empties(scanRoot(), emptyDirs, 250);
                ui.post(new Runnable() {
                    public void run() {
                        emptyItems.clear();
                        emptyItems.addAll(found);
                        emptyList.removeAllViews();
                        if (emptyItems.isEmpty()) {
                            emptySum.setText("");
                            emptyList.addView(UI.empty(act, "未发现空文件"));
                            return;
                        }
                        emptySum.setText(emptyItems.size() + " 项");
                        Runnable onChange = new Runnable() { public void run() {} };
                        for (JunkItem it : emptyItems)
                            emptyList.addView(UI.fileRow(act, it, onChange, whitelistAction(it)));
                    }
                });
            }
        }).start();
    }

    // ================= 应用缓存 =================

    private View appCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "📱  应用缓存排行"));
        c.addView(UI.note(act, Shell.hasRoot()
                ? "root 模式：可清理所有应用的内部缓存"
                : "无 root：仅可清理 Android/data 下的外部缓存"));

        LinearLayout ctl = UI.row(act);
        Button scan = UI.primary(act, "扫描");
        Button selAll = UI.secondary(act, "全选");
        Button none = UI.secondary(act, "全不选");
        scan.setTextSize(12); selAll.setTextSize(12); none.setTextSize(12);
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanApps(); }
        });
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setAppChecks(true); }
        });
        none.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setAppChecks(false); }
        });
        ctl.addView(scan, UI.weight(1f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ctl.addView(selAll, m);
        LinearLayout.LayoutParams m2 = UI.weight(1f, 40, act);
        m2.leftMargin = Theme.dp(act, 6);
        ctl.addView(none, m2);
        c.addView(ctl, UI.lpm(act, UI.MP, UI.WC, 10));

        appSum = UI.note(act, "");
        c.addView(appSum, UI.lpm(act, UI.MP, UI.WC, 8));
        appList = UI.col(act);
        c.addView(appList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button clean = UI.danger(act, "清理选中应用缓存");
        clean.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cleanApps(); }
        });
        c.addView(clean, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void scanApps() {
        appList.removeAllViews();
        appSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<Finder.AppCache> found = Finder.appCaches(act, act.store.whitelist());
                ui.post(new Runnable() {
                    public void run() {
                        appItems = found;
                        renderApps();
                    }
                });
            }
        }).start();
    }

    private void renderApps() {
        appList.removeAllViews();
        if (appItems.isEmpty()) {
            appSum.setText("");
            appList.addView(UI.empty(act, "未发现明显的应用缓存"));
            return;
        }
        long total = 0;
        for (Finder.AppCache a : appItems) total += a.size;
        appSum.setText(appItems.size() + " 个应用 · 合计 " + Util.fmtSize(total));

        long max = appItems.get(0).size;
        for (final Finder.AppCache a : appItems) {
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));
            final CheckBox cb = UI.check(act, a.checked);
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { a.checked = on; }
            });
            r.addView(cb);

            LinearLayout info = UI.col(act);
            TextView nm = UI.text(act, a.label, 12.5f, Theme.TEXT);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(nm);
            StorageBarView b = new StorageBarView(act);
            b.setPercent(max > 0 ? a.size * 100f / max : 0);
            info.addView(b, UI.lpm(act, UI.MP, Theme.dp(act, 6), 3));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            ip.leftMargin = Theme.dp(act, 6);
            ip.rightMargin = Theme.dp(act, 8);
            r.addView(info, ip);
            r.addView(UI.text(act, Util.fmtSize(a.size), 11.5f, Theme.ACCENT));

            r.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    act.store.addWhitelist(a.pkg);
                    a.checked = false;
                    v.setAlpha(0.35f);
                    act.toast("已加入白名单：" + a.pkg);
                    return true;
                }
            });
            appList.addView(r);
        }
    }

    private void setAppChecks(boolean on) {
        for (Finder.AppCache a : appItems) a.checked = on;
        renderApps();
    }

    private void cleanApps() {
        final List<Finder.AppCache> sel = new ArrayList<Finder.AppCache>();
        for (Finder.AppCache a : appItems) if (a.checked) sel.add(a);
        if (sel.isEmpty()) { act.toast("未选中应用"); return; }
        UI.confirm(act, "清理应用缓存",
                "将清理 " + sel.size() + " 个应用的缓存目录\n（不影响账号与聊天记录）",
                new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        CleanEngine eng = new CleanEngine(false);
                        long freed = 0;
                        for (Finder.AppCache a : sel) freed += eng.cleanAppCache(a.pkg);
                        final long f = freed;
                        ui.post(new Runnable() {
                            public void run() {
                                act.store.addStat(f, sel.size());
                                ScanEngine.invalidate();
                                act.toast("已清理 " + sel.size() + " 个应用 · 释放 " + Util.fmtSize(f));
                                scanApps();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    // ================= 冗余安装包 =================

    private View apkCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "📥  安装包管理"));
        c.addView(UI.note(act, "扫描 sdcard 上的 apk，标注是否已安装（已安装的可安全删除）"));

        LinearLayout ctl = UI.row(act);
        Button scan = UI.primary(act, "扫描 APK");
        Button selIns = UI.secondary(act, "只选已安装");
        scan.setTextSize(12); selIns.setTextSize(12);
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanApk(); }
        });
        selIns.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (Finder.ApkInfo a : apkItems) a.checked = a.installed;
                renderApk();
            }
        });
        ctl.addView(scan, UI.weight(1f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1.2f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ctl.addView(selIns, m);
        c.addView(ctl, UI.lpm(act, UI.MP, UI.WC, 10));

        apkSum = UI.note(act, "");
        c.addView(apkSum, UI.lpm(act, UI.MP, UI.WC, 8));
        apkList = UI.col(act);
        c.addView(apkList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button del = UI.danger(act, "删除选中安装包");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delApk(); }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void scanApk() {
        apkList.removeAllViews();
        apkSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<Finder.ApkInfo> found = Finder.apks(act, scanRoot());
                ui.post(new Runnable() {
                    public void run() {
                        apkItems = found;
                        renderApk();
                    }
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
        apkSum.setText(apkItems.size() + " 个 · 合计 " + Util.fmtSize(total)
                + " · 已安装 " + ins + " 个（" + Util.fmtSize(insTotal) + " 可回收）");

        for (final Finder.ApkInfo a : apkItems) {
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));
            final CheckBox cb = UI.check(act, a.checked);
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
            if (a.installed) {
                r.addView(UI.badge(act, "已装", Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)));
            } else if (a.pkg == null) {
                r.addView(UI.badge(act, "未知", Theme.DIM, Theme.alpha(Theme.DIM, 0x22)));
            } else {
                r.addView(UI.badge(act, "未装", Theme.WARN, Theme.alpha(Theme.WARN, 0x22)));
            }
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
            JunkItem it = new JunkItem(a.path, a.label, a.size);
            sel.add(it);
            if (!a.installed) unins++;
        }
        if (sel.isEmpty()) { act.toast("未选中安装包"); return; }
        long total = 0;
        for (JunkItem it : sel) total += it.size;
        String warn = unins > 0 ? "\n\n⚠ 其中 " + unins + " 个尚未安装，删除后需重新下载。" : "";
        UI.confirm(act, "删除安装包",
                "将删除 " + sel.size() + " 个 apk，约 " + Util.fmtSize(total) + warn,
                new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        final CleanEngine.Result r = new CleanEngine(act.store.toTrash()).cleanItems(sel);
                        ui.post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                act.toast("已删除 " + r.count + " 个 · 释放 " + Util.fmtSize(r.freed));
                                scanApk();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    // ================= 缩略图 =================

    private View thumbCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🖼  缩略图缓存"));
        c.addView(UI.note(act, "相册与图库的预览缓存，删除后浏览时会自动重建"));

        Button scan = UI.primary(act, "扫描");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanThumb(); }
        });
        c.addView(scan, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));

        thumbSum = UI.note(act, "");
        c.addView(thumbSum, UI.lpm(act, UI.MP, UI.WC, 8));
        thumbList = UI.col(act);
        c.addView(thumbList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button del = UI.danger(act, "清理选中");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(thumbItems, thumbList, thumbSum, false); }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void scanThumb() {
        thumbList.removeAllViews();
        thumbSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = Finder.thumbs();
                ui.post(new Runnable() {
                    public void run() {
                        thumbItems.clear();
                        thumbItems.addAll(found);
                        thumbList.removeAllViews();
                        if (thumbItems.isEmpty()) {
                            thumbSum.setText("");
                            thumbList.addView(UI.empty(act, "未发现缩略图缓存"));
                            return;
                        }
                        updateSum(thumbItems, thumbSum);
                        Runnable onChange = new Runnable() {
                            public void run() { updateSum(thumbItems, thumbSum); }
                        };
                        for (JunkItem it : thumbItems)
                            thumbList.addView(UI.fileRow(act, it, onChange, null));
                    }
                });
            }
        }).start();
    }

    // ================= 整理中心 =================

    private View organizeCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🗂  整理中心"));
        c.addView(UI.note(act, "按扩展名把散落文件归档到分类目录，支持干跑预览与整理还原"));

        ruleBox = UI.col(act);
        c.addView(ruleBox, UI.lpm(act, UI.MP, UI.WC, 10));
        renderRules();

        LinearLayout ops = UI.row(act);
        Button addRule = UI.secondary(act, "新增规则");
        Button editMap = UI.secondary(act, "分类映射");
        Button hist = UI.secondary(act, "整理历史");
        addRule.setTextSize(12); editMap.setTextSize(12); hist.setTextSize(12);
        addRule.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addRule(); }
        });
        editMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { editExtMap(); }
        });
        hist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showHistory(); }
        });
        ops.addView(addRule, UI.weight(1f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ops.addView(editMap, m);
        LinearLayout.LayoutParams m2 = UI.weight(1f, 40, act);
        m2.leftMargin = Theme.dp(act, 6);
        ops.addView(hist, m2);
        c.addView(ops, UI.lpm(act, UI.MP, UI.WC, 10));

        orgSum = UI.note(act, "");
        c.addView(orgSum, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    private void renderRules() {
        ruleBox.removeAllViews();
        List<String> lines = act.store.rules();
        if (lines.isEmpty()) {
            ruleBox.addView(UI.empty(act, "暂无整理规则"));
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            final int idx = i;
            final Organize.Rule r = Organize.Rule.parse(lines.get(i));
            if (r == null) continue;

            LinearLayout box = UI.col(act);
            box.setBackground(Theme.card(act, 12));
            int p = Theme.dp(act, 10);
            box.setPadding(p, p, p, p);

            final EditText src = UI.input(act, "源目录", r.src);
            final EditText dst = UI.input(act, "目标目录", r.dst);
            box.addView(UI.note(act, "源目录"));
            box.addView(src, UI.lpm(act, UI.MP, Theme.dp(act, 40), 2));
            box.addView(UI.note(act, "目标目录"), UI.lpm(act, UI.MP, UI.WC, 6));
            box.addView(dst, UI.lpm(act, UI.MP, Theme.dp(act, 40), 2));

            final boolean[] flags = {r.recursive, r.integrity};
            box.addView(UI.switchRow(act, "处理子目录", "递归遍历源目录下所有层级", r.recursive,
                    new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { flags[0] = on; }
            }));
            box.addView(UI.switchRow(act, "完整性检测", "跳过 .part / .tmp 等未完成的下载", r.integrity,
                    new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { flags[1] = on; }
            }));

            LinearLayout btns = UI.row(act);
            Button save = UI.secondary(act, "保存");
            Button prev = UI.secondary(act, "预览");
            Button run = UI.primary(act, "整理");
            Button del = UI.danger(act, "删");
            save.setTextSize(11.5f); prev.setTextSize(11.5f);
            run.setTextSize(11.5f); del.setTextSize(11.5f);

            save.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    saveRule(idx, new Organize.Rule(src.getText().toString().trim(),
                            dst.getText().toString().trim(), flags[0], flags[1]));
                    act.toast("规则已保存");
                }
            });
            prev.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    previewOrganize(new Organize.Rule(src.getText().toString().trim(),
                            dst.getText().toString().trim(), flags[0], flags[1]));
                }
            });
            run.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    runOrganize(new Organize.Rule(src.getText().toString().trim(),
                            dst.getText().toString().trim(), flags[0], flags[1]));
                }
            });
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { deleteRule(idx); }
            });

            btns.addView(save, UI.weight(1f, 36, act));
            LinearLayout.LayoutParams b1 = UI.weight(1f, 36, act);
            b1.leftMargin = Theme.dp(act, 5);
            btns.addView(prev, b1);
            LinearLayout.LayoutParams b2 = UI.weight(1f, 36, act);
            b2.leftMargin = Theme.dp(act, 5);
            btns.addView(run, b2);
            LinearLayout.LayoutParams b3 = UI.weight(0.6f, 36, act);
            b3.leftMargin = Theme.dp(act, 5);
            btns.addView(del, b3);
            box.addView(btns, UI.lpm(act, UI.MP, UI.WC, 8));

            ruleBox.addView(box, UI.lpm(act, UI.MP, UI.WC, i == 0 ? 0 : 8));
        }
    }

    private void saveRule(int idx, Organize.Rule r) {
        List<String> lines = new ArrayList<String>(act.store.rules());
        if (idx < lines.size()) lines.set(idx, r.serialize());
        else lines.add(r.serialize());
        act.store.setRules(lines);
    }

    private void addRule() {
        List<String> lines = new ArrayList<String>(act.store.rules());
        lines.add(Util.sdRoot() + "/Download|" + Util.sdRoot() + "/JunkClean整理|1|1");
        act.store.setRules(lines);
        renderRules();
    }

    private void deleteRule(final int idx) {
        UI.confirm(act, "删除规则", "确认删除这条整理规则？", new Runnable() {
            public void run() {
                List<String> lines = new ArrayList<String>(act.store.rules());
                if (idx < lines.size()) lines.remove(idx);
                act.store.setRules(lines);
                renderRules();
                act.toast("规则已删除");
            }
        });
    }

    private void editExtMap() {
        final EditText e = UI.multiline(act, "每行：.ext1,.ext2=分类名", act.store.extMap(), 8);
        new android.app.AlertDialog.Builder(act)
                .setTitle("分类映射")
                .setView(e)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        act.store.setExtMap(e.getText().toString());
                        act.toast("映射已保存");
                    }
                }).show();
    }

    private void previewOrganize(final Organize.Rule r) {
        orgSum.setText("预览中…");
        new Thread(new Runnable() {
            public void run() {
                Organize org = new Organize(act.store.extMap(), act.store.whitelist());
                final Organize.Result res = org.preview(r);
                ui.post(new Runnable() {
                    public void run() {
                        if (res.moves.isEmpty()) {
                            orgSum.setText("没有需要整理的文件"
                                    + (res.skipped > 0 ? "（跳过 " + res.skipped + " 个未完成下载）" : ""));
                            return;
                        }
                        orgSum.setText("将移动 " + res.moves.size() + " 个文件 · "
                                + Util.fmtSize(res.total)
                                + (res.skipped > 0 ? " · 跳过 " + res.skipped : ""));
                        StringBuilder sb = new StringBuilder();
                        int n = Math.min(res.moves.size(), 40);
                        for (int i = 0; i < n; i++) {
                            Organize.Move mv = res.moves.get(i);
                            sb.append(new File(mv.from).getName()).append("\n  → ")
                              .append(Util.shortPath(new File(mv.to).getParent())).append("\n\n");
                        }
                        if (res.moves.size() > n) sb.append("… 还有 ").append(res.moves.size() - n).append(" 个");
                        UI.info(act, "整理预览（干跑）", sb.toString());
                    }
                });
            }
        }).start();
    }

    private void runOrganize(final Organize.Rule r) {
        UI.confirm(act, "执行整理",
                "将按分类映射移动文件到：\n" + Util.shortPath(r.dst)
                + "\n\n整理记录会保存，可随时还原。", new Runnable() {
            public void run() {
                orgSum.setText("整理中…");
                new Thread(new Runnable() {
                    public void run() {
                        Organize org = new Organize(act.store.extMap(), act.store.whitelist());
                        final Organize.Result res = org.run(r);
                        ui.post(new Runnable() {
                            public void run() {
                                orgSum.setText("已整理 " + res.moves.size() + " 个文件 · "
                                        + Util.fmtSize(res.total));
                                act.toast("整理完成：" + res.moves.size() + " 个文件");
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private void showHistory() {
        final List<String[]> hist = Organize.history(200);
        if (hist.isEmpty()) { act.toast("暂无整理历史"); return; }
        StringBuilder sb = new StringBuilder();
        int n = Math.min(hist.size(), 30);
        for (int i = 0; i < n; i++) {
            String[] h = hist.get(i);
            sb.append(new File(h[0]).getName()).append('\n')
              .append("  现在：").append(Util.shortPath(new File(h[0]).getParent())).append('\n')
              .append("  原位：").append(Util.shortPath(new File(h[1]).getParent())).append("\n\n");
        }
        if (hist.size() > n) sb.append("… 共 ").append(hist.size()).append(" 条记录");

        new android.app.AlertDialog.Builder(act)
                .setTitle("整理历史（" + hist.size() + " 条）")
                .setMessage(sb.toString())
                .setNeutralButton("清空记录", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        Organize.clearHistory();
                        act.toast("历史已清空");
                    }
                })
                .setNegativeButton("关闭", null)
                .setPositiveButton("全部还原", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        new Thread(new Runnable() {
                            public void run() {
                                final int n = Organize.undoAll();
                                ui.post(new Runnable() {
                                    public void run() {
                                        act.toast("已还原 " + n + " 个文件");
                                        orgSum.setText("已还原 " + n + " 个文件到原位");
                                    }
                                });
                            }
                        }).start();
                    }
                }).show();
    }

    // ================= 回收站 =================

    private View trashCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🗑  回收站"));
        c.addView(UI.note(act, "清理时移入的文件暂存于此，可恢复到原位"));

        LinearLayout ctl = UI.row(act);
        Button load = UI.primary(act, "刷新");
        Button restore = UI.secondary(act, "恢复选中");
        Button del = UI.danger(act, "彻底删除");
        load.setTextSize(12); restore.setTextSize(12); del.setTextSize(12);
        load.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { loadTrash(); }
        });
        restore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { restoreTrash(); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { deleteTrash(); }
        });
        ctl.addView(load, UI.weight(1f, 40, act));
        LinearLayout.LayoutParams m = UI.weight(1.2f, 40, act);
        m.leftMargin = Theme.dp(act, 6);
        ctl.addView(restore, m);
        LinearLayout.LayoutParams m2 = UI.weight(1.2f, 40, act);
        m2.leftMargin = Theme.dp(act, 6);
        ctl.addView(del, m2);
        c.addView(ctl, UI.lpm(act, UI.MP, UI.WC, 10));

        trashSum = UI.note(act, "");
        c.addView(trashSum, UI.lpm(act, UI.MP, UI.WC, 8));
        trashList = UI.col(act);
        c.addView(trashList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button empty = UI.danger(act, "清空回收站（长按确认）");
        empty.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { act.toast("请长按 1 秒确认清空"); }
        });
        empty.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) { emptyTrash(); return true; }
        });
        c.addView(empty, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void loadTrash() {
        trashList.removeAllViews();
        trashSum.setText("读取中…");
        new Thread(new Runnable() {
            public void run() {
                final List<Trash.Item> items = Trash.list(act.store.trashDays());
                ui.post(new Runnable() {
                    public void run() {
                        trashItems = items;
                        renderTrash();
                    }
                });
            }
        }).start();
    }

    private void renderTrash() {
        trashList.removeAllViews();
        if (trashItems.isEmpty()) {
            trashSum.setText("");
            trashList.addView(UI.empty(act, "回收站为空"));
            return;
        }
        trashSum.setText(trashItems.size() + " 项 · 占用 " + Util.fmtSize(Trash.totalSize(trashItems)));
        for (final Trash.Item it : trashItems) {
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));
            final CheckBox cb = UI.check(act, false);
            cb.setTag(it);
            r.addView(cb);

            LinearLayout info = UI.col(act);
            TextView nm = UI.text(act, new File(it.orig).getName(), 12, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            info.addView(nm);
            String meta = Util.shortPath(new File(it.orig).getParent())
                    + " · " + Util.fmtTime(it.time * 1000);
            info.addView(UI.note(act, meta));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            ip.leftMargin = Theme.dp(act, 4);
            r.addView(info, ip);

            if (it.left >= 0) {
                r.addView(UI.badge(act, "剩 " + it.left + " 天",
                        it.left <= 1 ? Theme.DANGER : Theme.WARN,
                        Theme.alpha(it.left <= 1 ? Theme.DANGER : Theme.WARN, 0x22)));
            }
            TextView sz = UI.text(act, Util.fmtSize(it.size), 11.5f, Theme.DIM);
            LinearLayout.LayoutParams sp = UI.lp(UI.WC, UI.WC);
            sp.leftMargin = Theme.dp(act, 6);
            r.addView(sz, sp);
            trashList.addView(r);
        }
    }

    private List<Trash.Item> checkedTrash() {
        List<Trash.Item> sel = new ArrayList<Trash.Item>();
        for (int i = 0; i < trashList.getChildCount(); i++) {
            View v = trashList.getChildAt(i);
            if (!(v instanceof LinearLayout)) continue;
            View f = ((LinearLayout) v).getChildAt(0);
            if (f instanceof CheckBox && ((CheckBox) f).isChecked()) {
                Object tag = f.getTag();
                if (tag instanceof Trash.Item) sel.add((Trash.Item) tag);
            }
        }
        return sel;
    }

    private void restoreTrash() {
        final List<Trash.Item> sel = checkedTrash();
        if (sel.isEmpty()) { act.toast("未选中项目"); return; }
        new Thread(new Runnable() {
            public void run() {
                int n = 0;
                for (Trash.Item it : sel) if (Trash.restore(it)) n++;
                final int fn = n;
                ui.post(new Runnable() {
                    public void run() {
                        act.toast("已恢复 " + fn + " 项到原位");
                        loadTrash();
                    }
                });
            }
        }).start();
    }

    private void deleteTrash() {
        final List<Trash.Item> sel = checkedTrash();
        if (sel.isEmpty()) { act.toast("未选中项目"); return; }
        UI.confirm(act, "彻底删除", "将永久删除 " + sel.size() + " 项，无法恢复。", new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        long freed = 0;
                        for (Trash.Item it : sel) freed += Trash.delete(it);
                        final long f = freed;
                        ui.post(new Runnable() {
                            public void run() {
                                act.toast("已删除 · 释放 " + Util.fmtSize(f));
                                loadTrash();
                                act.homePage().refreshDisk();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private void emptyTrash() {
        UI.confirm(act, "清空回收站", "将永久删除回收站内所有文件，无法恢复。", new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        final long f = Trash.empty();
                        ui.post(new Runnable() {
                            public void run() {
                                act.toast("回收站已清空 · 释放 " + Util.fmtSize(f));
                                loadTrash();
                                act.homePage().refreshDisk();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    // ================= fstrim =================

    private View trimCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "⚡  fstrim 优化"));
        c.addView(UI.note(act, "对分区执行 TRIM，回收闪存已删除块，可改善写入性能（需 root）"));

        trimResult = UI.note(act, "");
        LinearLayout ops = UI.row(act);
        final String[] mounts = {"/data", "/cache", "/system"};
        for (final String mp : mounts) {
            Button b = UI.secondary(act, mp);
            b.setTextSize(12);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { runTrim(mp); }
            });
            LinearLayout.LayoutParams lp = UI.weight(1f, 40, act);
            if (ops.getChildCount() > 0) lp.leftMargin = Theme.dp(act, 6);
            ops.addView(b, lp);
        }
        c.addView(ops, UI.lpm(act, UI.MP, UI.WC, 10));
        c.addView(trimResult, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    private void runTrim(final String mp) {
        if (!Shell.hasRoot()) { act.toast("fstrim 需要 root"); return; }
        trimResult.setText("执行中：" + mp + "…");
        new Thread(new Runnable() {
            public void run() {
                final String out = Shell.fstrim(mp);
                ui.post(new Runnable() {
                    public void run() { trimResult.setText(out); }
                });
            }
        }).start();
    }

    // ================= 公共 =================

    private String scanRoot() {
        String r = act.store.scanRoot();
        return r == null || r.trim().isEmpty() ? Util.sdRoot() : r.trim();
    }

    private Runnable whitelistAction(final JunkItem it) {
        return new Runnable() {
            public void run() {
                String key = new File(it.path).getName();
                act.store.addWhitelist(key);
                it.checked = false;
                act.toast("已加入白名单：" + key);
            }
        };
    }

    private void selectAll(List<JunkItem> pool, LinearLayout box, boolean on) {
        for (JunkItem it : pool) it.checked = on;
        for (int i = 0; i < box.getChildCount(); i++) {
            View v = box.getChildAt(i);
            if (!(v instanceof LinearLayout)) continue;
            View f = ((LinearLayout) v).getChildAt(0);
            if (f instanceof CheckBox) ((CheckBox) f).setChecked(on);
        }
    }

    private void updateSum(List<JunkItem> pool, TextView sum) {
        long sel = 0;
        int n = 0;
        for (JunkItem it : pool) if (it.checked) { sel += it.size; n++; }
        if (n == 0) sum.setText(pool.size() + " 项");
        else sum.setText("已选 " + n + " / " + pool.size() + " 项 · " + Util.fmtSize(sel));
    }

    /** 删除选中项（toTrash=true 走回收站） */
    private void removeItems(final List<JunkItem> pool, final LinearLayout box,
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
                                box.removeAllViews();
                                Runnable onChange = new Runnable() {
                                    public void run() { updateSum(pool, sum); }
                                };
                                if (pool.isEmpty()) {
                                    box.addView(UI.empty(act, "列表已清空"));
                                    sum.setText("");
                                } else {
                                    for (JunkItem it : pool)
                                        box.addView(UI.fileRow(act, it, onChange, whitelistAction(it)));
                                    updateSum(pool, sum);
                                }
                                String msg = (toTrash ? "已移入回收站 " : "已删除 ") + r.count
                                        + " 项 · " + Util.fmtSize(toTrash ? r.trashed : r.freed)
                                        + (toTrash ? "（清空回收站后才真正释放）" : "");
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
}
