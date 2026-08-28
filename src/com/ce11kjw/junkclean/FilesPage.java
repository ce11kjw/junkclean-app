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
    private EditText bigMin, bigSearch, brSearch;
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
    private String keepPolicy;

    // 空文件
    private LinearLayout emptyList;
    private TextView emptySum;
    private final List<JunkItem> emptyItems = new ArrayList<JunkItem>();
    private boolean emptyDirs = true;

    // 安装包

    // 文件清理（目录浏览）
    private LinearLayout brList;
    private TextView brPath, brSum;
    private String brCur;
    private List<Browser.Entry> brItems = new ArrayList<Browser.Entry>();
    private boolean brDirSize = true;

    public FilesPage(MainActivity a) {
        super(a);
        keepPolicy = a.store.keepPolicy();
    }

    @Override
    public View view() {
        if (scroll != null) return scroll;
        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, Theme.S4);
        root.setPadding(p, Theme.dp(act, Theme.S5), p, p);

        LinearLayout head = UI.col(act);
        head.addView(UI.eyebrow(act, "存储"));
        TextView ht = UI.display(act, "文件", Theme.T_TITLE, Theme.TEXT);
        ht.setTypeface(Theme.display(), android.graphics.Typeface.BOLD);
        head.addView(ht, UI.lpm(act, UI.WC, UI.WC, 2));
        root.addView(head);

        root.addView(UI.section(act, "大文件"));
        root.addView(bigCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "重复文件"));
        root.addView(dupCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "空文件与空目录"));
        root.addView(emptyCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "文件清理"));
        root.addView(browseCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.spacer(act, Theme.S8));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        return scroll;
    }

    // ---------- 大文件 ----------

    private View bigCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "大文件"));
        c.addView(UI.title(act, "占用排查"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "按类型筛选，按体积或时间排序，可只看旧文件"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

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

        bigSearch = UI.search(act, "按文件名过滤");
        bigSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable e) { renderBig(); }
        });
        c.addView(bigSearch, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));

        bigSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(bigSum, UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        bigList = UI.col(act);
        c.addView(bigList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button selAll = UI.secondary(act, "反选");
        Button trash = UI.secondary(act, "移入回收站");
        Button del = UI.danger(act, "彻底删除");
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { invertAll(bigItems, bigList); updateSum(bigItems, bigSum); }
        });
        selAll.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                selectTopN(bigItems, bigList, 10);
                updateSum(bigItems, bigSum);
                act.toast("已选中体积最大的 10 项");
                return true;
            }
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
                final List<JunkItem> found = Finder.big(scanRoot(), min, bigDays, wl(), 300, act.store.fullScan());
                post(new Runnable() {
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
        String kw = bigSearch == null ? ""
                : bigSearch.getText().toString().trim().toLowerCase(java.util.Locale.US);
        List<JunkItem> show = new ArrayList<JunkItem>();
        for (JunkItem it : bigItems) {
            if (!Finder.matchType(it.name, bigType)) continue;
            if (!kw.isEmpty() && !it.name.toLowerCase(java.util.Locale.US).contains(kw)) continue;
            show.add(it);
        }
        sortList(show, bigSort);
        if (show.isEmpty()) {
            bigSum.setText("");
            bigList.addView(UI.empty(act, kw.isEmpty()
                    ? "未发现符合条件的文件" : "没有匹配「" + kw + "」的文件"));
            return;
        }
        long total = 0;
        for (JunkItem it : show) total += it.size;
        bigSum.setText(show.size() + " 项   " + Util.fmtSize(total));
        renderBatched(show, bigList, bigSum, 0);
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
        c.addView(UI.eyebrow(act, "重复文件"));
        c.addView(UI.title(act, "内容比对"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "先按大小分桶再算内容哈希，同组按策略保留一份"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        dupPolicyLabel = UI.data(act, "策略  " + policyLabel(keepPolicy), Theme.T_DATA_S, Theme.MUTED);
        c.addView(dupPolicyLabel, UI.lpm(act, UI.MP, UI.WC, 8));

        Button policy = UI.secondary(act, "保留策略");
        Button scan = UI.primary(act, "扫描重复");
        policy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickPolicy(); }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanDup(); }
        });
        dupSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(dupSum, UI.lpm(act, UI.MP, UI.WC, 8));
        dupList = UI.col(act);
        c.addView(dupList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button del = UI.danger(act, "删除副本");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delDup(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, policy, scan, del), UI.lpm(act, UI.MP, UI.WC, 10));

        // 视觉重复（照片/视频）+ AI 确认
        c.addView(UI.switchRow(act, "AI 视觉确认",
                "感知哈希找到照片/视频相似后，交给 AI 二次确认是否真重复",
                act.store.aiDupCheck(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                act.store.setAiDupCheck(on);
                if (on && !act.store.aiReady()) {
                    act.toast("请先在设置配置 AI 端点");
                }
            }
        }), UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        Button aiBtn = UI.secondary(act, "🤖  AI 确认相似组");
        aiBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { aiVerifyDups(); }
        });
        c.addView(aiBtn, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));
        return c;
    }

    /** 把「视觉相似」组交给 AI 二次确认，排除构图相似但内容不同的 */
    private void aiVerifyDups() {
        if (!act.store.aiReady()) {
            act.toast("请先在设置 → AI 清理建议 配置端点");
            return;
        }
        if (dupGroups.isEmpty()) { act.toast("请先扫描重复文件"); return; }

        // 找感知哈希组（可能重复的）
        final java.util.List<Finder.DupGroup> visual = new ArrayList<Finder.DupGroup>();
        for (Finder.DupGroup g : dupGroups) {
            // 组内第一项如果是图片/视频才送 AI
            JunkItem first = g.files.get(0);
            if (PerceptualHash.isImage(new File(first.path))
                    || PerceptualHash.isVideo(new File(first.path))) {
                visual.add(g);
            }
        }
        if (visual.isEmpty()) {
            act.toast("没有需要 AI 确认的图片/视频组");
            return;
        }

        final android.app.Dialog dlg = (android.app.Dialog) UI.progress(
                act, "AI 确认中", "正在让 AI 判断 " + visual.size() + " 组是否真重复")[0];
        new Thread(new Runnable() {
            public void run() {
                final int[] removed = {0};
                for (Finder.DupGroup g : visual) {
                    boolean dup = Ai.verifyDupGroup(act.store, g);
                    if (!dup) {
                        dupGroups.remove(g);   // AI 说不是重复，剔除
                        removed[0]++;
                    }
                }
                post(new Runnable() {
                    public void run() {
                        dlg.dismiss();
                        if (removed[0] > 0) {
                            act.toast("AI 剔除了 " + removed[0] + " 组「构图相似但不同」");
                            renderDup();
                        } else {
                            act.toast("AI 确认这些组都是重复的");
                        }
                    }
                });
            }
        }).start();
    }

    private String policyLabel(String key) {
        if ("oldest".equals(key)) return "保留最旧";
        if ("shortest".equals(key)) return "保留路径最短";
        if ("largest".equals(key)) return "保留体积最大";
        return "保留最新";
    }

    private void pickPolicy() {
        final String[] labels = {"保留最新", "保留最旧", "保留路径最短", "保留体积最大"};
        final String[] keys = {"newest", "oldest", "shortest", "largest"};
        int cur = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(keepPolicy)) cur = i;
        UI.pick(act, "保留策略", labels, cur, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                keepPolicy = keys[w];
                act.store.setKeepPolicy(keepPolicy);   // 持久化，重开仍生效
                dupPolicyLabel.setText("策略  " + labels[w]);
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
                final int vThresh = act.store.visualThreshold();
                final List<Finder.DupGroup> gs = Finder.duplicates(
                        scanRoot(), 65536, 40, wl(), vThresh);
                Finder.applyKeepPolicy(gs, keepPolicy);
                post(new Runnable() {
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
            box.setBackground(Theme.item(act, false));
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
                        post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                ScanEngine.invalidate();
                                String msg = "已处理 " + r.count + " 个副本 · "
                                        + Util.fmtSize(toTrash ? r.trashed : r.freed);
                                act.toast(msg);
                                if (!r.errors.isEmpty()) offerRetry(r.errors, null);
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
        c.addView(UI.eyebrow(act, "空文件"));
        c.addView(UI.title(act, "零字节与空目录"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.switchRow(act, "包含空目录", "同时列出没有内容的文件夹", emptyDirs,
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { emptyDirs = on; }
        }));

        emptySum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(emptySum, UI.lpm(act, UI.MP, UI.WC, 8));
        emptyList = UI.col(act);
        c.addView(emptyList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button scan = UI.primary(act, "扫描");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanEmpty(); }
        });
        Button selAll = UI.secondary(act, "反选");
        Button del = UI.danger(act, "清理选中");
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { invertAll(emptyItems, emptyList); updateSum(emptyItems, emptySum); }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removeItems(emptyItems, emptyList, emptySum, false); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, scan, selAll, del), UI.lpm(act, UI.MP, UI.WC, 10));
        return c;
    }

    private void scanEmpty() {
        emptyList.removeAllViews();
        emptySum.setText("扫描中…");
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = Finder.empties(scanRoot(), emptyDirs, 250, wl(), act.store.fullScan());
                post(new Runnable() {
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







    // ---------- 文件清理 ----------

    private View browseCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "文件清理"));
        c.addView(UI.title(act, "目录浏览"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "手动删除任意文件或文件夹，重要目录标记保护且不可删除"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        brPath = UI.text(act, "", 11.5f, Theme.ACCENT);
        brPath.setSingleLine(true);
        brPath.setEllipsize(android.text.TextUtils.TruncateAt.START);
        c.addView(brPath, UI.lpm(act, UI.MP, UI.WC, 10));

        c.addView(UI.switchRow(act, "计算文件夹体积", "关闭可加快大目录加载", brDirSize,
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                brDirSize = on;
                if (brCur != null) loadBrowse(brCur);
            }
        }));

        brSearch = UI.search(act, "在当前目录过滤");
        brSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable e) { renderBrowse(); }
        });
        c.addView(brSearch, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));

        brSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(brSum, UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        brList = UI.col(act);
        c.addView(brList, UI.lpm(act, UI.MP, UI.WC, 4));

        Button jump = UI.secondary(act, "快捷目录");
        Button up = UI.secondary(act, "上一级");
        Button prot = UI.secondary(act, "保护清单");
        Button del = UI.danger(act, "删除选中");
        prot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showProtected(); }
        });
        jump.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickShortcut(); }
        });
        up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String p = Browser.parent(brCur == null ? Util.sdRoot() : brCur);
                if (p == null) { act.toast("已在存储根目录"); return; }
                loadBrowse(p);
            }
        });
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delBrowse(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, jump, up, prot), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        c.addView(UI.btnRow(act, UI.BTN_H, del), UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        loadBrowse(Util.sdRoot());
        return c;
    }

    /** 保护路径清单：可删除/恢复。删除后该目录会被扫描，可手动加入白名单。 */
    private void showProtected() {
        final java.util.List<String> all = new java.util.ArrayList<String>();
        for (String p : Store.PROTECTED) all.add(p);

        Object[] parts = UI.glassDialog(act, "保护路径");
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        body.addView(UI.note(act, "点 × 移除某项保护。移除后该目录会被扫描到。\n长按可恢复出厂。"),
                UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        for (final String p : all) {
            LinearLayout row = UI.row(act);
            row.setBackground(Theme.item(act, false));
            int pd = Theme.dp(act, 12);
            row.setPadding(pd, Theme.dp(act, 8), pd, Theme.dp(act, 8));
            LinearLayout.LayoutParams rp = UI.lp(UI.MP, UI.WC);
            rp.topMargin = Theme.dp(act, 6);
            TextView nm = UI.text(act, p, Theme.T_BODY,
                    act.store.isRemovedFromProt(p) ? Theme.DIM : Theme.TEXT);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            row.addView(nm, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            if (act.store.isRemovedFromProt(p)) {
                TextView bd = UI.badge(act, "已移除", Theme.WARN,
                        Theme.alpha(Theme.WARN, 0x22));
                row.addView(bd);
            }
            TextView x = UI.text(act, "×", Theme.T_TITLE,
                    act.store.isRemovedFromProt(p) ? Theme.ACCENT : Theme.MUTED);
            x.setGravity(android.view.Gravity.CENTER);
            x.setPadding(Theme.dp(act, 10), 0, Theme.dp(act, 10), 0);
            x.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (act.store.isRemovedFromProt(p)) {
                        act.store.removeFromProt(p);
                    } else {
                        // 二次确认
                        UI.confirm(act, "移除保护", "确认让 JunkClean 扫描这个目录？\n\n"
                                + p, new Runnable() {
                            public void run() { act.store.removeFromProt(p); }
                        });
                    }
                    // 刷新整个对话框
                    d.dismiss();
                    ScanEngine.invalidate();
                    showProtected();
                    act.toast("已" + (act.store.isRemovedFromProt(p) ? "移除" : "恢复") + "保护：" + p);
                }
            });
            row.addView(x);
            body.addView(row, rp);
        }

        Button close = UI.primary(act, "关闭");
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        wrap.addView(UI.btnRow(act, UI.BTN_H, close), UI.lpm(act, UI.MP, UI.WC, Theme.S4));
        d.show();
    }

    private void pickShortcut() {
        final String[][] sc = Browser.shortcuts();
        String[] labels = new String[sc.length];
        for (int i = 0; i < sc.length; i++) labels[i] = sc[i][0];
        UI.pick(act, "跳转到", labels, -1, new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                if (!new File(sc[w][1]).isDirectory()) { act.toast("目录不存在"); return; }
                loadBrowse(sc[w][1]);
            }
        });
    }

    private void loadBrowse(final String path) {
        brCur = path;
        brPath.setText(Util.shortPath(path));
        brList.removeAllViews();
        brSum.setText("读取中…");
        new Thread(new Runnable() {
            public void run() {
                final List<Browser.Entry> list = Browser.list(path, brDirSize);
                post(new Runnable() {
                    public void run() {
                        brItems = list;
                        renderBrowse();
                    }
                });
            }
        }).start();
    }

    private void renderBrowse() {
        brList.removeAllViews();
        if (brItems.isEmpty()) {
            brSum.setText("");
            brList.addView(UI.empty(act, "目录为空"));
            return;
        }
        long total = 0;
        for (Browser.Entry e : brItems) total += e.size;
        brSum.setText(brItems.size() + " 项 · 合计 " + Util.fmtSize(total)
                + "（点击文件夹进入，长按加入白名单）");

        String kw = brSearch == null ? ""
                : brSearch.getText().toString().trim().toLowerCase(java.util.Locale.US);
        int shown = 0;
        for (final Browser.Entry e : brItems) {
            if (!kw.isEmpty() && !e.name.toLowerCase(java.util.Locale.US).contains(kw)) continue;
            if (++shown > 120) {
                brList.addView(UI.note(act, "… 还有 " + (brItems.size() - 120) + " 项未显示"));
                break;
            }
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));

            final CheckBox cb = UI.check(act, false);
            cb.setEnabled(!e.protectedPath);
            cb.setAlpha(e.protectedPath ? 0.3f : 1f);
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                    e.checked = on;
                    updateBrowseSum();
                }
            });
            r.addView(cb);

            TextView nm = UI.text(act, (e.dir ? "📁  " : "📄  ") + e.name, 12,
                    e.protectedPath ? Theme.DIM : Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            np.leftMargin = Theme.dp(act, 4);
            r.addView(nm, np);

            if (e.protectedPath) {
                r.addView(UI.badge(act, "保护", Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)));
            }
            String meta = e.dir
                    ? (brDirSize ? Util.fmtSize(e.size) : e.children + " 项")
                    : Util.fmtSize(e.size);
            TextView sz = UI.text(act, meta, 11, Theme.DIM);
            LinearLayout.LayoutParams sp = UI.lp(UI.WC, UI.WC);
            sp.leftMargin = Theme.dp(act, 6);
            r.addView(sz, sp);

            if (e.dir) {
                nm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { loadBrowse(e.path); }
                });
            }
            r.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    act.store.addWhitelist(e.name);
                    ScanEngine.invalidate();
                    act.toast("已加入白名单：" + e.name);
                    return true;
                }
            });
            brList.addView(r);
        }
    }

    private void updateBrowseSum() {
        int n = 0;
        long sz = 0;
        for (Browser.Entry e : brItems) if (e.checked) { n++; sz += e.size; }
        if (n == 0) {
            brSum.setText(brItems.size() + " 项（点击文件夹进入，长按加入白名单）");
        } else {
            brSum.setText("已选 " + n + " 项 · " + Util.fmtSize(sz));
        }
    }

    private void delBrowse() {
        final List<JunkItem> sel = new ArrayList<JunkItem>();
        int dirs = 0;
        for (Browser.Entry e : brItems) {
            if (!e.checked || e.protectedPath) continue;
            sel.add(new JunkItem(e.path, e.name, e.size));
            if (e.dir) dirs++;
        }
        if (sel.isEmpty()) { act.toast("未选中项目"); return; }
        long total = 0;
        for (JunkItem it : sel) total += it.size;
        final boolean toTrash = act.store.toTrash();
        String detail = "共 " + sel.size() + " 项"
                + (dirs > 0 ? "（含 " + dirs + " 个文件夹，将连同内部文件一起删除）" : "")
                + "，约 " + Util.fmtSize(total)
                + (toTrash ? "\n\n先移入回收站，可恢复。" : "\n\n直接删除，无法恢复！");
        UI.confirm(act, "删除所选", detail, new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        final CleanEngine.Result r = new CleanEngine(toTrash).cleanItems(sel);
                        post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                ScanEngine.invalidate();
                                String msg = "已处理 " + r.count + " 项 · "
                                        + Util.fmtSize(toTrash ? r.trashed : r.freed);
                                act.toast(msg);
                                if (!r.errors.isEmpty()) {
                                    offerRetry(r.errors, new Runnable() {
                                        public void run() { loadBrowse(brCur); }
                                    });
                                }
                                act.homePage().refreshDisk();
                                loadBrowse(brCur);
                            }
                        });
                    }
                }).start();
            }
        });
    }


}
