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
    private EditText bigMin, bigSearch;
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

    // 低质量图片
    private TextView qualitySum;
    private LinearLayout qualityList;
    private final List<JunkItem> qualityItems = new ArrayList<JunkItem>();
    private final List<QualityDetect.Verdict> qualityVerdicts =
            new ArrayList<QualityDetect.Verdict>();

    // 文件清理（目录浏览）
    private android.widget.EditText brPath;
    private LinearLayout savedDirList, brList;
    private TextView brSum;
    private List<Browser.Entry> brItems = new ArrayList<Browser.Entry>();
    private String brCur = Util.sdRoot();

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
        root.addView(UI.section(act, "低质量图片"));
        root.addView(qualityCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
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
                        // 相似拒绝：记住这组，下次扫描不再列出
                        if (!g.files.isEmpty()) {
                            act.store.addDupDeny(g.files.get(0).path);
                        }
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
        if ("ai_smart".equals(key)) return "AI 智能";
        return "保留最新";
    }

    private void pickPolicy() {
        final String[] labels = {"保留最新", "保留最旧", "保留路径最短", "保留体积最大", "AI 智能"};
        final String[] keys = {"newest", "oldest", "shortest", "largest", "ai_smart"};
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
                final java.util.Set<String> deny =
                        new java.util.HashSet<String>(act.store.dupDeny());
                final List<Finder.DupGroup> gs = Finder.duplicates(
                        scanRoot(), 65536, 40, wl(), vThresh, deny);
                
                // AI 智能策略：用视觉模型挑最优
                if ("ai_smart".equals(keepPolicy) && act.store.aiReady() && !gs.isEmpty()) {
                    final java.util.Map<String, QualityAI.Verdict> verdicts = new java.util.HashMap<>();
                    for (Finder.DupGroup g : gs) {
                        QualityAI.judge(act, g.files, act.store, verdicts);
                    }
                    // 选中每组得分最高的那一份
                    for (Finder.DupGroup g : gs) {
                        JunkItem best = null;
                        float bestScore = -1;
                        for (JunkItem it : g.files) {
                            QualityAI.Verdict v = verdicts.get(it.path);
                            if (v != null && v.score > bestScore) {
                                bestScore = v.score;
                                best = it;
                            }
                        }
                        if (best != null) {
                            // AI 给了评分 —— 选中最高分，其余标 checked
                            for (int i = 0; i < g.files.size(); i++) g.files.get(i).checked = (g.files.get(i) != best);
                        } else {
                            // AI 未评分（全失败） —— 兜底走 newest
                            Finder.applyKeepPolicy(Collections.singletonList(g), "newest");
                        }
                    }
                } else {
                    Finder.applyKeepPolicy(gs, keepPolicy);
                }
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

    /**
     * 低质量图片识别（photoo 思路）：
     * 模糊（边缘稀疏）、暗光（亮度低）、过曝（高亮像素多）三轴判定。
     * 本地像素分析，不上传任何图片。
     */
    private View qualityCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "低质量图片"));
        c.addView(UI.title(act, "模糊/暗光/过曝"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "基于像素灰度+边缘密度判别，纯本地计算"), UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        Button scan = UI.primary(act, "扫描低质量图片");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanQuality(); }
        });
        c.addView(scan, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S3));

        qualitySum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(qualitySum, UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        qualityList = UI.col(act);
        c.addView(qualityList, UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        Button del = UI.danger(act, "清理选中");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                removeItems(qualityItems, qualityList, qualitySum, false);
            }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));
        return c;
    }

    private void scanQuality() {
        qualityItems.clear();
        qualityVerdicts.clear();
        qualityList.removeAllViews();
        qualitySum.setText("扫描中...");
        new Thread(new Runnable() {
            public void run() {
                final java.util.List<JunkItem> found =
                        new java.util.ArrayList<JunkItem>();
                QualityDetect.scan(scanRoot(), wl(), found, qualityVerdicts);
                post(new Runnable() {
                    public void run() {
                        qualityItems.addAll(found);
                        if (qualityItems.isEmpty()) {
                            qualitySum.setText("没有发现低质量图片");
                            qualityList.addView(UI.empty(act, "图片质量都还不错"));
                        } else {
                            qualitySum.setText(qualityItems.size() + " 张");
                            rebuild(qualityItems, qualityList, qualitySum);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 文件清理：填目录路径 → 一键删除整个目录。
     * 不做浏览点选，用户输入目标路径，二次确认后整目录删除。
     */
    private View browseCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "文件清理"));
        c.addView(UI.title(act, "目录浏览与清理"), UI.lpm(act, UI.MP, UI.WC, 2));

        // 单一路径输入框
        LinearLayout pathRow = UI.row(act);
        brPath = UI.input(act, Util.sdRoot() + "/Download", "");
        pathRow.addView(brPath, UI.weight(1f, UI.BTN_H, act));
        Button viewBtn = UI.primary(act, "查看目录");
        viewBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { goToPath(); }
        });
        LinearLayout.LayoutParams vp = UI.lp(Theme.dp(act, 72), Theme.dp(act, UI.BTN_H));
        vp.leftMargin = Theme.dp(act, Theme.S2);
        pathRow.addView(viewBtn, vp);
        c.addView(pathRow, UI.lpm(act, UI.MP, UI.WC, Theme.S3));

        // 目录内容（勾选文件）
        brSum = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        c.addView(brSum, UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        brList = UI.col(act);
        c.addView(brList, UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        // 操作按钮
        Button selAll = UI.secondary(act, "全选");
        Button clean = UI.danger(act, "立即清理选中");
        Button save = UI.secondary(act, "保存到定时");
        selAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (Browser.Entry e : brItems) e.checked = !e.protectedPath;
                // 勾选框通过 tag 关联，重绘一次保持同步
                for (int i = 0; i < brList.getChildCount(); i++) {
                    View row = brList.getChildAt(i);
                    if (!(row instanceof LinearLayout)) continue;
                    View f = ((LinearLayout) row).getChildAt(0);
                    if (f instanceof CheckBox && f.getTag() instanceof Browser.Entry) {
                        ((CheckBox) f).setChecked(((Browser.Entry) f.getTag()).checked);
                    }
                }
                updateBrowseSum();
            }
        });
        clean.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cleanCheckedFiles(); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveCurrentDir(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, selAll, clean, save), UI.lpm(act, UI.MP, UI.WC, Theme.S3));

        // 已保存定时列表
        c.addView(UI.eyebrow(act, "定时清理（已保存）"), UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        savedDirList = UI.col(act);
        c.addView(savedDirList, UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        renderSavedCleanDirs();

        Button runSchedule = UI.primary(act, "执行定时清理");
        Button clearAll = UI.secondary(act, "清空列表");
        runSchedule.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { runSavedDirsNow(); }
        });
        clearAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { act.store.clearCleanDirs(); renderSavedCleanDirs(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, runSchedule, clearAll), UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        goToPath();
        return c;
    }

    private void goToPath() {
        String path = brPath.getText().toString().trim();
        if (path.isEmpty()) { path = Util.sdRoot(); }
        java.io.File f = new java.io.File(path);
        if (!f.isAbsolute()) f = new java.io.File(Util.sdRoot(), path);
        if (!f.isDirectory()) { act.toast("目录不存在"); return; }
        brPath.setText(f.getAbsolutePath());
        loadBrowseDir(f.getAbsolutePath());
    }

    private void loadBrowseDir(String path) {
        brCur = path;
        brItems.clear();
        brList.removeAllViews();
        brSum.setText("读取中…");
        new Thread(new Runnable() {
            public void run() {
                final java.util.List<Browser.Entry> list = Browser.list(path, false);
                post(new Runnable() {
                    public void run() {
                        brItems = list;
                        renderBrowseDir();
                    }
                });
            }
        }).start();
    }

    private void renderBrowseDir() {
        brList.removeAllViews();
        if (brItems.isEmpty()) {
            brSum.setText("目录为空");
            brList.addView(UI.empty(act, "空目录"));
            return;
        }
        long total = 0;
        for (Browser.Entry e : brItems) total += e.size;
        brSum.setText(brItems.size() + " 项 · " + Util.fmtSize(total)
                + "（点击文件夹进入，勾选后点立即清理）");
        int shown = 0;
        for (final Browser.Entry e : brItems) {
            if (shown++ > 120) { brList.addView(UI.note(act, "… 还有 " + (brItems.size() - 120) + " 项")); break; }
            LinearLayout r = UI.row(act);
            r.setPadding(0, Theme.dp(act, 5), 0, Theme.dp(act, 5));
            final CheckBox cb = UI.check(act, false);
            cb.setTag(e);
            cb.setEnabled(!e.protectedPath);
            cb.setAlpha(e.protectedPath ? 0.3f : 1f);
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { e.checked = on; updateBrowseSum(); }
            });
            r.addView(cb);
            TextView nm = UI.text(act, (e.dir ? "📁 " : "📄 ") + e.name, Theme.T_BODY_S, e.protectedPath ? Theme.DIM : Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            r.addView(nm, UI.weight(1f, UI.WC, act));
            r.addView(UI.data(act, e.dir ? "" : Util.fmtSize(e.size), Theme.T_DATA_S, Theme.DIM));
            if (e.dir) {
                nm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { loadBrowseDir(e.path); brPath.setText(e.path); }
                });
            }
            brList.addView(r);
        }
    }

    private void updateBrowseSum() {
        int n = 0; long sz = 0;
        for (Browser.Entry e : brItems) if (e.checked) { n++; sz += e.size; }
        if (n == 0) { brSum.setText(brItems.size() + " 项"); return; }
        brSum.setText("已选 " + n + " / " + brItems.size() + " 项 · " + Util.fmtSize(sz));
    }

    private void cleanCheckedFiles() {
        final java.util.List<JunkItem> sel = new java.util.ArrayList<JunkItem>();
        for (Browser.Entry e : brItems) {
            if (!e.checked || e.protectedPath) continue;
            sel.add(new JunkItem(e.path, e.name, e.size));
        }
        if (sel.isEmpty()) { act.toast("未选中项目"); return; }
        UI.confirm(act, "立即清理",
                "将删除 " + sel.size() + " 项 · " + Util.fmtSize(sizeOf(sel)),
                new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        CleanEngine.Result r = new CleanEngine(false).cleanItems(sel);
                        post(new Runnable() {
                            public void run() {
                                act.store.addStat(r.freed, r.count);
                                act.toast("已清理 " + r.count + " 项 · " + Util.fmtSize(r.freed));
                                if (!r.errors.isEmpty()) offerRetry(r.errors, null);
                                loadBrowseDir(brCur);
                                act.homePage().refreshDisk();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private boolean checkable(Browser.Entry e) { return e != null && !e.protectedPath; }

    private long sizeOf(java.util.List<JunkItem> items) {
        long s = 0; for (JunkItem it : items) s += it.size; return s;
    }

    private void saveCurrentDir() {
        String path = brPath.getText().toString().trim();
        if (path.isEmpty()) { act.toast("请先查看一个目录"); return; }
        java.io.File f = new java.io.File(path);
        if (!f.isDirectory()) { act.toast("目录不存在"); return; }
        // 尾斜杠规则
        boolean del = !path.endsWith("/") && !path.endsWith("/ ");
        act.store.saveCleanDir(f.getAbsolutePath(), del);
        renderSavedCleanDirs();
        act.toast("已保存到定时清理列表");
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


