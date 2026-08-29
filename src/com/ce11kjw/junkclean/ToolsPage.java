package com.ce11kjw.junkclean;

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

/** 工具箱：应用缓存 / 缩略图 / 整理中心 / 回收站 / fstrim */
public class ToolsPage extends PageBase {

    private ScrollView scroll;

    // 应用缓存
    private LinearLayout appList;
    private TextView appSum;
    private List<Finder.AppCache> appItems = new ArrayList<Finder.AppCache>();

    // 缩略图
    private LinearLayout thumbList;
    private TextView thumbSum;
    private final List<JunkItem> thumbItems = new ArrayList<JunkItem>();

    // 整理中心
    private EditText orgSrcInput, appSearch;
    private String trashSort = "new";
    private LinearLayout ruleBox;
    private TextView orgSum;

    // 回收站
    private LinearLayout trashList;
    private TextView trashSum;
    private List<Trash.Item> trashItems = new ArrayList<Trash.Item>();

    // fstrim
    private TextView trimResult;

    public ToolsPage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;
        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, Theme.S4);
        root.setPadding(p, Theme.dp(act, Theme.S5), p, p);

        LinearLayout head = UI.col(act);
        head.addView(UI.eyebrow(act, "维护"));
        TextView ht = UI.display(act, "工具箱", Theme.T_TITLE, Theme.TEXT);
        ht.setTypeface(Theme.display(), android.graphics.Typeface.BOLD);
        head.addView(ht, UI.lpm(act, UI.WC, UI.WC, 2));
        root.addView(head);

        root.addView(UI.section(act, "应用缓存"));
        root.addView(appCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "缩略图缓存"));
        root.addView(thumbCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "整理中心"));
        root.addView(organizeCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "回收站"));
        root.addView(trashCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "存储优化"));
        root.addView(trimCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.spacer(act, Theme.S8));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        return scroll;
    }

    // ---------- 应用缓存 ----------

    private View appCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "应用缓存"));
        c.addView(UI.title(act, Shell.hasRoot() ? "深度清理" : "受限清理"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, Shell.hasRoot()
                ? "可清理所有应用的内部缓存目录"
                : "无 root，仅能处理 Android/data 下的外部缓存"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        Button scan = UI.primary(act, "扫描");
        Button selAll = UI.secondary(act, "全选");
        Button none = UI.secondary(act, "全不选");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanApps(); }
        });
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setAppChecks(true); }
        });
        none.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setAppChecks(false); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, scan, selAll, none), UI.lpm(act, UI.MP, UI.WC, 8));

        appSearch = UI.search(act, "按应用名过滤");
        appSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable e) { renderApps(); }
        });
        c.addView(appSearch, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));

        appSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(appSum, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        appList = UI.col(act);
        c.addView(appList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button clean = UI.danger(act, "清理选中应用缓存");
        clean.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cleanApps(); }
        });
        // 无 root 自动清理（无障碍，Apex 思路）
        Button autoClean = UI.secondary(act, "自动清理（无root）");
        autoClean.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { autoCleanApps(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, clean, autoClean), UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    /** 无 root：借无障碍服务逐个打开应用信息页自动点「清除缓存」 */
    private void autoCleanApps() {
        final List<String> pkgs = new ArrayList<String>();
        for (Finder.AppCache a : appItems) if (a.checked) pkgs.add(a.pkg);
        if (pkgs.isEmpty()) { act.toast("未选中应用"); return; }

        if (!CacheAccessibilityService.isEnabled()) {
            UI.confirm(act, "需要无障碍权限",
                    "无 root 自动清缓存需要开启无障碍服务。\n\n"
                    + "开启后 JunkClean 会自动打开每个应用的信息页并点击「清除缓存」，"
                    + "过程中屏幕会快速跳转，请勿操作。\n\n现在去开启？",
                    new Runnable() {
                public void run() {
                    try {
                        act.startActivity(new android.content.Intent(
                                android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (Exception e) { act.toast("无法打开无障碍设置"); }
                }
            });
            return;
        }
        UI.confirm(act, "自动清理",
                "将自动清理 " + pkgs.size() + " 个应用缓存。\n过程中屏幕会自动跳转，请勿操作手机。",
                new Runnable() {
            public void run() {
                CacheAccessibilityService.startBatch(act, pkgs);
                act.toast("开始自动清理，请勿操作屏幕…");
            }
        });
    }

    private void scanApps() {
        appList.removeAllViews();
        appSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<Finder.AppCache> found = Finder.appCaches(act, wl());
                post(new Runnable() {
                    public void run() { appItems = found; renderApps(); }
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
        String kw = appSearch == null ? ""
                : appSearch.getText().toString().trim().toLowerCase(java.util.Locale.US);
        List<Finder.AppCache> show = new ArrayList<Finder.AppCache>();
        for (Finder.AppCache a : appItems) {
            if (!kw.isEmpty() && !a.label.toLowerCase(java.util.Locale.US).contains(kw)) continue;
            show.add(a);
        }
        if (show.isEmpty()) {
            appSum.setText("");
            appList.addView(UI.empty(act, "没有匹配「" + kw + "」的应用"));
            return;
        }
        long total = 0;
        for (Finder.AppCache a : show) total += a.size;
        appSum.setText(show.size() + " 个应用   " + Util.fmtSize(total));

        long max = show.get(0).size;
        for (final Finder.AppCache a : show) {
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));
            CheckBox cb = UI.check(act, a.checked);
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { a.checked = on; }
            });
            r.addView(cb);

            LinearLayout info = UI.col(act);
            TextView nm = UI.text(act, a.label, 12.5f, Theme.TEXT);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(nm);
            SegmentGauge b = new SegmentGauge(act, true);
            b.setPercent(max > 0 ? a.size * 100f / max : 0);
            info.addView(b, UI.lpm(act, UI.MP, Theme.dp(act, 6), 3));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            ip.leftMargin = Theme.dp(act, 6);
            ip.rightMargin = Theme.dp(act, 8);
            r.addView(info, ip);
            r.addView(UI.data(act, Util.fmtSize(a.size), Theme.T_DATA_S, Theme.ACCENT));

            r.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    act.store.addWhitelist(a.pkg);
                    a.checked = false;
                    v.setAlpha(0.35f);
                    ScanEngine.invalidate();
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
                        post(new Runnable() {
                            public void run() {
                                act.store.addStat(f, sel.size());
                                ScanEngine.invalidate();
                                act.toast("已清理 " + sel.size() + " 个应用 · 释放 " + Util.fmtSize(f));
                                act.homePage().refreshDisk();
                                scanApps();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    // ---------- 缩略图 ----------

    private View thumbCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "缩略图"));
        c.addView(UI.title(act, "预览缓存"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "相册与图库的预览图，删除后浏览时自动重建"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        thumbSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(thumbSum, UI.lpm(act, UI.MP, UI.WC, 8));
        thumbList = UI.col(act);
        c.addView(thumbList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button scan = UI.primary(act, "扫描");
        Button del = UI.danger(act, "清理选中");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanThumb(); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(thumbItems, thumbList, thumbSum, false); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, scan, del), UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    private void scanThumb() {
        thumbList.removeAllViews();
        thumbSum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = Finder.thumbs(wl());
                post(new Runnable() {
                    public void run() {
                        thumbItems.clear();
                        thumbItems.addAll(found);
                        if (thumbItems.isEmpty()) {
                            thumbList.removeAllViews();
                            thumbSum.setText("");
                            thumbList.addView(UI.empty(act, "未发现缩略图缓存"));
                        } else {
                            rebuild(thumbItems, thumbList, thumbSum);
                        }
                    }
                });
            }
        }).start();
    }

    // ---------- 整理中心 ----------

    private View organizeCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "整理中心"));
        c.addView(UI.title(act, "按类型归档"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "源目录统一，每条规则只定义去向"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        c.addView(UI.h2(act, "统一源目录"), UI.lpm(act, UI.MP, UI.WC, 10));
        orgSrcInput = UI.input(act, Util.sdRoot() + "/Download", act.store.orgSrc());
        Button saveSrc = UI.secondary(act, "保存");
        saveSrc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String p = orgSrcInput.getText().toString().trim();
                if (p.isEmpty()) { act.toast("源目录不能为空"); return; }
                act.store.setOrgSrc(p);
                renderRules();
                act.toast("源目录已保存");
            }
        });
        LinearLayout srcRow = UI.row(act);
        srcRow.addView(orgSrcInput, UI.weight(1f, UI.BTN_H, act));
        LinearLayout.LayoutParams sbp = UI.lp(Theme.dp(act, 58), Theme.dp(act, UI.BTN_H));
        sbp.leftMargin = Theme.dp(act, 6);
        srcRow.addView(saveSrc, sbp);
        c.addView(srcRow, UI.lpm(act, UI.MP, UI.WC, 4));

        c.addView(UI.h2(act, "整理规则"), UI.lpm(act, UI.MP, UI.WC, 12));
        ruleBox = UI.col(act);
        c.addView(ruleBox, UI.lpm(act, UI.MP, UI.WC, 6));
        renderRules();

        Button addRule = UI.secondary(act, "新增规则");
        Button editMap = UI.secondary(act, "分类映射");
        Button hist = UI.secondary(act, "整理历史");
        addRule.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addRule(); }
        });
        editMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { editExtMap(); }
        });
        hist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showHistory(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, addRule, editMap, hist), UI.lpm(act, UI.MP, UI.WC, 10));

        orgSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
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
        String src = act.store.orgSrc();
        for (int i = 0; i < lines.size(); i++) {
            final int idx = i;
            final Organize.Rule r = Organize.Rule.parse(lines.get(i), src);
            if (r == null) continue;

            LinearLayout box = UI.col(act);
            box.setBackground(Theme.item(act, false));
            int p = Theme.dp(act, 10);
            box.setPadding(p, p, p, p);

            box.addView(UI.note(act, "目标目录"));
            final EditText dst = UI.input(act, Util.sdRoot() + "/JunkClean整理", r.dst);
            box.addView(dst, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

            // 两个开关排在同一水平线
            final boolean[] flags = {r.recursive, r.integrity};
            LinearLayout swRow = UI.row(act);
            LinearLayout c1 = UI.col(act);
            c1.addView(UI.text(act, "处理子项目", 12, Theme.TEXT));
            android.widget.Switch s1 = UI.smallSwitch(act, r.recursive);
            s1.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { flags[0] = on; }
            });
            LinearLayout r1 = UI.row(act);
            r1.addView(c1, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            r1.addView(s1);

            LinearLayout c2 = UI.col(act);
            c2.addView(UI.text(act, "完整性检测", 12, Theme.TEXT));
            android.widget.Switch s2 = UI.smallSwitch(act, r.integrity);
            s2.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { flags[1] = on; }
            });
            LinearLayout r2 = UI.row(act);
            r2.addView(c2, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            r2.addView(s2);

            LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            half2.leftMargin = Theme.dp(act, 10);
            swRow.addView(r1, half);
            swRow.addView(r2, half2);
            box.addView(swRow, UI.lpm(act, UI.MP, UI.WC, 8));

            Button save = UI.secondary(act, "保存");
            Button prev = UI.secondary(act, "预览");
            Button run = UI.primary(act, "整理");
            Button del = UI.danger(act, "删");
            save.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    saveRule(idx, new Organize.Rule(act.store.orgSrc(),
                            dst.getText().toString().trim(), flags[0], flags[1]));
                    act.toast("规则已保存");
                }
            });
            prev.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    previewOrganize(new Organize.Rule(act.store.orgSrc(),
                            dst.getText().toString().trim(), flags[0], flags[1]));
                }
            });
            run.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    runOrganize(new Organize.Rule(act.store.orgSrc(),
                            dst.getText().toString().trim(), flags[0], flags[1]));
                }
            });
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { deleteRule(idx); }
            });
            box.addView(UI.btnRow(act, 32, save, prev), UI.lpm(act, UI.MP, UI.WC, 8));
        box.addView(UI.btnRow(act, 32, run, del), UI.lpm(act, UI.MP, UI.WC, 6));

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
        lines.add(Util.sdRoot() + "/JunkClean整理|1|1");
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
        UI.prompt(act, "分类映射", "每行：.ext1,.ext2=分类名", act.store.extMap(), 8,
                new UI.Callback<String>() {
            public void call(String v) {
                act.store.setExtMap(v);
                act.toast("映射已保存");
            }
        });
    }

    private void previewOrganize(final Organize.Rule r) {
        orgSum.setText("预览中…");
        new Thread(new Runnable() {
            public void run() {
                Organize org = new Organize(act.store.extMap(), wl());
                final Organize.Result res = org.preview(r);
                post(new Runnable() {
                    public void run() {
                        if (res.moves.isEmpty()) {
                            orgSum.setText("没有需要整理的文件"
                                    + (res.skipped > 0 ? "（跳过 " + res.skipped + " 个未完成下载）" : ""));
                            return;
                        }
                        orgSum.setText("将移动 " + res.moves.size() + " 个文件 · " + Util.fmtSize(res.total)
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
                "源：" + Util.shortPath(r.src) + "\n目标：" + Util.shortPath(r.dst)
                + "\n\n整理记录会保存，可随时还原。", new Runnable() {
            public void run() {
                orgSum.setText("整理中…");
                new Thread(new Runnable() {
                    public void run() {
                        Organize org = new Organize(act.store.extMap(), wl());
                        final Organize.Result res = org.run(r);
                        post(new Runnable() {
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

        UI.triple(act, "整理历史（" + hist.size() + " 条）", sb.toString(),
                "清空记录", new Runnable() {
                    public void run() {
                        Organize.clearHistory();
                        act.toast("历史已清空");
                    }
                },
                "全部还原", new Runnable() {
                    public void run() {
                        new Thread(new Runnable() {
                            public void run() {
                                final int n = Organize.undoAll();
                                post(new Runnable() {
                                    public void run() {
                                        act.toast("已还原 " + n + " 个文件");
                                        orgSum.setText("已还原 " + n + " 个文件到原位");
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    // ---------- 回收站 ----------

    private View trashCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "回收站"));
        c.addView(UI.title(act, "已删除项目"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "清理时移入的文件暂存在这里，可恢复到原位"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        Button load = UI.primary(act, "刷新");
        Button sortBtn = UI.secondary(act, "排序");
        Button restore = UI.secondary(act, "恢复选中");
        Button del = UI.danger(act, "彻底删除");
        sortBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickTrashSort(); }
        });
        load.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { loadTrash(); }
        });
        restore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { restoreTrash(); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { deleteTrash(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, load, sortBtn), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        c.addView(UI.btnRow(act, UI.BTN_H, restore, del), UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        trashSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(trashSum, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        trashList = UI.col(act);
        c.addView(trashList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button empty = UI.danger(act, "清空回收站（长按确认）");
        empty.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { act.toast("请长按 1 秒确认清空"); }
        });
        empty.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) { emptyTrash(); return true; }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, empty), UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    private void pickTrashSort() {
        final String[] labels = {"时间从新到旧", "时间从旧到新", "体积从大到小", "名称"};
        final String[] keys = {"new", "old", "size", "name"};
        int cur = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(trashSort)) cur = i;
        UI.pick(act, "回收站排序", labels, cur,
                new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                trashSort = keys[w];
                renderTrash();
            }
        });
    }

    private void sortTrash(List<Trash.Item> list) {
        java.util.Comparator<Trash.Item> cmp;
        if ("old".equals(trashSort)) {
            cmp = new java.util.Comparator<Trash.Item>() {
                public int compare(Trash.Item a, Trash.Item b) { return Long.compare(a.time, b.time); }
            };
        } else if ("size".equals(trashSort)) {
            cmp = new java.util.Comparator<Trash.Item>() {
                public int compare(Trash.Item a, Trash.Item b) { return Long.compare(b.size, a.size); }
            };
        } else if ("name".equals(trashSort)) {
            cmp = new java.util.Comparator<Trash.Item>() {
                public int compare(Trash.Item a, Trash.Item b) {
                    return new File(a.orig).getName().compareToIgnoreCase(new File(b.orig).getName());
                }
            };
        } else {
            cmp = new java.util.Comparator<Trash.Item>() {
                public int compare(Trash.Item a, Trash.Item b) { return Long.compare(b.time, a.time); }
            };
        }
        java.util.Collections.sort(list, cmp);
    }

    private void loadTrash() {
        trashList.removeAllViews();
        trashSum.setText("读取中…");
        new Thread(new Runnable() {
            public void run() {
                final List<Trash.Item> items = Trash.list(act.store.trashDays());
                post(new Runnable() {
                    public void run() { trashItems = items; renderTrash(); }
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
        sortTrash(trashItems);
        trashSum.setText(trashItems.size() + " 项   " + Util.fmtSize(Trash.totalSize(trashItems)));
        for (final Trash.Item it : trashItems) {
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));
            CheckBox cb = UI.check(act, false);
            cb.setTag(it);
            r.addView(cb);

            LinearLayout info = UI.col(act);
            TextView nm = UI.text(act, new File(it.orig).getName(), 12, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            info.addView(nm);
            info.addView(UI.note(act, Util.shortPath(new File(it.orig).getParent())
                    + " · " + Util.fmtTime(it.time * 1000)));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            ip.leftMargin = Theme.dp(act, 4);
            r.addView(info, ip);

            if (it.left >= 0) {
                r.addView(UI.badge(act, "剩 " + it.left + " 天",
                        it.left <= 1 ? Theme.DANGER : Theme.WARN,
                        Theme.alpha(it.left <= 1 ? Theme.DANGER : Theme.WARN, 0x22)));
            }
            TextView sz = UI.data(act, Util.fmtSize(it.size), Theme.T_DATA_S, Theme.DIM);
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
                post(new Runnable() {
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
                        post(new Runnable() {
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
                        post(new Runnable() {
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

    // ---------- fstrim ----------

    private View trimCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "存储优化"));
        c.addView(UI.title(act, "fstrim"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "对分区执行 TRIM，回收闪存已删除块，改善写入性能（需 root）"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        trimResult = UI.data(act, "", Theme.T_DATA_S, Theme.MUTED);
        final String[] mounts = {"/data", "/cache", "/system"};
        Button[] bs = new Button[mounts.length];
        for (int i = 0; i < mounts.length; i++) {
            final String mp = mounts[i];
            Button b = UI.secondary(act, mp);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { runTrim(mp); }
            });
            bs[i] = b;
        }
        c.addView(UI.btnRow(act, UI.BTN_H, bs), UI.lpm(act, UI.MP, UI.WC, 8));
        c.addView(trimResult, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    private void runTrim(final String mp) {
        if (!Shell.hasRoot()) { act.toast("fstrim 需要 root"); return; }
        trimResult.setText("执行中：" + mp + "…");
        new Thread(new Runnable() {
            public void run() {
                final String out = Shell.fstrim(mp);
                post(new Runnable() {
                    public void run() { trimResult.setText(out); }
                });
            }
        }).start();
    }
}
