package com.ce11kjw.junkclean;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 工具箱：大文件 / 空文件 / 重复文件 / 应用缓存 / 缩略图 */
public class ToolsPage {

    private final MainActivity act;
    private ScrollView scroll;
    private final Handler ui = new Handler(Looper.getMainLooper());

    // 大文件
    private EditText bigMin;
    private LinearLayout bigList;
    private final List<JunkItem> bigItems = new ArrayList<JunkItem>();
    private String bigType = "all";
    private final List<Button> typeChips = new ArrayList<Button>();

    // 空文件
    private LinearLayout emptyList;
    private final List<JunkItem> emptyItems = new ArrayList<JunkItem>();

    // 重复文件
    private LinearLayout dupList;

    // 应用缓存
    private LinearLayout appList;
    private final List<Object[]> appItems = new ArrayList<Object[]>(); // {pkg, label, size}

    private static final Map<String, String[]> TYPE_EXT = new HashMap<String, String[]>();
    static {
        TYPE_EXT.put("img", new String[]{".jpg",".jpeg",".png",".gif",".webp",".heic",".bmp",".avif"});
        TYPE_EXT.put("vid", new String[]{".mp4",".mkv",".mov",".avi",".webm",".3gp",".m4v",".flv",".ts"});
        TYPE_EXT.put("doc", new String[]{".pdf",".doc",".docx",".xls",".xlsx",".ppt",".pptx",".txt",".epub"});
        TYPE_EXT.put("zip", new String[]{".zip",".7z",".rar",".tar",".gz",".xz",".apk",".iso"});
    }

    public ToolsPage(MainActivity a) { this.act = a; }

    public View view() {
        if (scroll != null) return scroll;
        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        root.addView(bigCard());
        root.addView(emptyCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(dupCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(appCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(thumbCard(), UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(UI.spacer(act, 20));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        return scroll;
    }

    // ---------- 大文件 ----------

    private View bigCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "📁  大文件清理"));
        c.addView(UI.note(act, "扫描 sdcard 上超过阈值的文件，按类型筛选"));

        LinearLayout typeRow = UI.row(act);
        String[][] types = {{"all","全部"},{"img","图片"},{"vid","视频"},{"doc","文档"},{"zip","压缩包"}};
        for (String[] t : types) {
            final String key = t[0];
            Button chip = UI.chip(act, t[1], key.equals("all"));
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    bigType = key;
                    for (int i = 0; i < typeChips.size(); i++)
                        UI.setChipActive(act, typeChips.get(i), typeChips.get(i) == v);
                    renderBig();
                }
            });
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, UI.WC);
            lp.rightMargin = Theme.dp(act, 5);
            typeRow.addView(chip, lp);
            typeChips.add(chip);
        }
        c.addView(typeRow, UI.lpm(act, UI.MP, UI.WC, 10));

        LinearLayout ctl = UI.row(act);
        bigMin = UI.input(act, "阈值 MB", "50");
        bigMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        Button scan = UI.primary(act, "扫描");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanBig(); }
        });
        LinearLayout.LayoutParams e1 = new LinearLayout.LayoutParams(0, Theme.dp(act, 42), 1f);
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, Theme.dp(act, 42), 1f);
        b1.leftMargin = Theme.dp(act, 8);
        ctl.addView(bigMin, e1);
        ctl.addView(scan, b1);
        c.addView(ctl, UI.lpm(act, UI.MP, UI.WC, 10));

        bigList = UI.col(act);
        c.addView(bigList, UI.lpm(act, UI.MP, UI.WC, 8));

        Button del = UI.danger(act, "删除选中");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delSelected(bigItems, bigList); }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void scanBig() {
        final long min;
        try { min = Long.parseLong(bigMin.getText().toString().trim()) * 1048576L; }
        catch (Exception e) { act.toast("阈值格式错误"); return; }
        bigList.removeAllViews();
        bigList.addView(UI.note(act, "扫描中…"));
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = new ArrayList<JunkItem>();
                walkBig(new File(Util.sdRoot()), min, found, 0);
                Collections.sort(found, new Comparator<JunkItem>() {
                    public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
                });
                ui.post(new Runnable() {
                    public void run() {
                        bigItems.clear();
                        bigItems.addAll(found.size() > 200 ? found.subList(0, 200) : found);
                        renderBig();
                    }
                });
            }
        }).start();
    }

    private void walkBig(File dir, long min, List<JunkItem> out, int depth) {
        if (depth > 8 || out.size() > 400) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) { walkBig(f, min, out, depth + 1); continue; }
            if (f.length() >= min) {
                JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName(), f.length());
                it.checked = false;
                out.add(it);
            }
        }
    }

    private void renderBig() {
        bigList.removeAllViews();
        List<JunkItem> show = new ArrayList<JunkItem>();
        for (JunkItem it : bigItems) if (matchType(it.name)) show.add(it);
        if (show.isEmpty()) {
            TextView e = UI.note(act, "✦  未发现符合条件的文件");
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, Theme.dp(act, 16), 0, Theme.dp(act, 16));
            bigList.addView(e);
            return;
        }
        for (final JunkItem it : show) bigList.addView(itemRow(it));
    }

    private boolean matchType(String name) {
        if ("all".equals(bigType)) return true;
        String low = name.toLowerCase(Locale.US);
        String[] exts = TYPE_EXT.get(bigType);
        if (exts == null) return true;
        for (String e : exts) if (low.endsWith(e)) return true;
        return false;
    }

    // ---------- 空文件 ----------

    private View emptyCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🫙  空文件清理"));
        c.addView(UI.note(act, "查找 0 字节文件与空目录"));

        Button scan = UI.primary(act, "扫描");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanEmpty(); }
        });
        c.addView(scan, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));

        emptyList = UI.col(act);
        c.addView(emptyList, UI.lpm(act, UI.MP, UI.WC, 8));

        Button del = UI.danger(act, "清理选中");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { delSelected(emptyItems, emptyList); }
        });
        c.addView(del, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void scanEmpty() {
        emptyList.removeAllViews();
        emptyList.addView(UI.note(act, "扫描中…"));
        new Thread(new Runnable() {
            public void run() {
                final List<JunkItem> found = new ArrayList<JunkItem>();
                walkEmpty(new File(Util.sdRoot()), found, 0);
                ui.post(new Runnable() {
                    public void run() {
                        emptyItems.clear();
                        emptyItems.addAll(found.size() > 200 ? found.subList(0, 200) : found);
                        emptyList.removeAllViews();
                        if (emptyItems.isEmpty()) {
                            TextView e = UI.note(act, "✦  未发现空文件");
                            e.setGravity(Gravity.CENTER);
                            emptyList.addView(e);
                        } else {
                            for (JunkItem it : emptyItems) emptyList.addView(itemRow(it));
                        }
                    }
                });
            }
        }).start();
    }

    private void walkEmpty(File dir, List<JunkItem> out, int depth) {
        if (depth > 8 || out.size() > 300) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                File[] sub = f.listFiles();
                if (sub != null && sub.length == 0) {
                    JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName() + "/（空目录）", 0);
                    it.checked = false;
                    out.add(it);
                } else {
                    walkEmpty(f, out, depth + 1);
                }
            } else if (f.length() == 0) {
                JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName(), 0);
                it.checked = false;
                out.add(it);
            }
        }
    }

    // ---------- 重复文件 ----------

    private View dupCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🔁  重复文件"));
        c.addView(UI.note(act, "按大小 + 名称初筛，同组保留第一个"));

        Button scan = UI.primary(act, "扫描重复");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanDup(); }
        });
        c.addView(scan, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        dupList = UI.col(act);
        c.addView(dupList, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    private void scanDup() {
        dupList.removeAllViews();
        dupList.addView(UI.note(act, "扫描中…"));
        new Thread(new Runnable() {
            public void run() {
                final Map<String, List<File>> groups = new HashMap<String, List<File>>();
                collectDup(new File(Util.sdRoot()), groups, 0);
                ui.post(new Runnable() {
                    public void run() {
                        dupList.removeAllViews();
                        int n = 0;
                        for (Map.Entry<String, List<File>> e : groups.entrySet()) {
                            if (e.getValue().size() < 2) continue;
                            if (++n > 20) break;
                            List<File> g = e.getValue();
                            LinearLayout box = UI.col(act);
                            box.setPadding(0, Theme.dp(act, 6), 0, Theme.dp(act, 6));
                            box.addView(UI.text(act, g.get(0).getName() + " × " + g.size()
                                    + " 份 · " + Util.fmtSize(g.get(0).length()), 12, Theme.MUTED));
                            for (int i = 1; i < g.size(); i++) {
                                final JunkItem it = new JunkItem(g.get(i).getAbsolutePath(),
                                        g.get(i).getAbsolutePath().replace(Util.sdRoot(), "…"),
                                        g.get(i).length());
                                it.checked = false;
                                box.addView(itemRow(it));
                                bigItems.add(it);   // 复用删除逻辑
                            }
                            dupList.addView(box);
                        }
                        if (n == 0) {
                            TextView e = UI.note(act, "✦  未发现重复文件");
                            e.setGravity(Gravity.CENTER);
                            dupList.addView(e);
                        }
                    }
                });
            }
        }).start();
    }

    private void collectDup(File dir, Map<String, List<File>> map, int depth) {
        if (depth > 6 || map.size() > 3000) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) { collectDup(f, map, depth + 1); continue; }
            if (f.length() < 4096) continue;
            String key = f.getName() + ":" + f.length();
            List<File> l = map.get(key);
            if (l == null) { l = new ArrayList<File>(); map.put(key, l); }
            l.add(f);
        }
    }

    // ---------- 应用缓存 ----------

    private View appCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "📱  应用缓存"));
        c.addView(UI.note(act, Shell.hasRoot()
                ? "root 模式：可清理所有应用缓存"
                : "无 root：仅可清理可访问的外部缓存"));

        Button scan = UI.primary(act, "扫描应用");
        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { scanApps(); }
        });
        c.addView(scan, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        appList = UI.col(act);
        c.addView(appList, UI.lpm(act, UI.MP, UI.WC, 8));

        Button clean = UI.danger(act, "清理选中应用缓存");
        clean.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cleanApps(); }
        });
        c.addView(clean, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        return c;
    }

    private void scanApps() {
        appList.removeAllViews();
        appList.addView(UI.note(act, "扫描中…"));
        new Thread(new Runnable() {
            public void run() {
                final List<Object[]> found = new ArrayList<Object[]>();
                PackageManager pm = act.getPackageManager();
                List<ApplicationInfo> apps;
                try { apps = pm.getInstalledApplications(0); }
                catch (Exception e) { apps = new ArrayList<ApplicationInfo>(); }
                boolean root = Shell.hasRoot();
                for (ApplicationInfo a : apps) {
                    if ((a.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    long sz = 0;
                    if (root) {
                        String s = Shell.one(true, "du -sk /data/data/" + a.packageName
                                + "/cache 2>/dev/null | cut -f1");
                        try { sz = Long.parseLong(s.trim()) * 1024; } catch (Exception ignored) {}
                    }
                    File ext = new File(Util.sdRoot() + "/Android/data/" + a.packageName + "/cache");
                    sz += Util.dirSize(ext);
                    if (sz > 65536) {
                        String label = String.valueOf(pm.getApplicationLabel(a));
                        found.add(new Object[]{a.packageName, label, sz});
                    }
                }
                Collections.sort(found, new Comparator<Object[]>() {
                    public int compare(Object[] x, Object[] y) {
                        return Long.compare((Long) y[2], (Long) x[2]);
                    }
                });
                ui.post(new Runnable() {
                    public void run() {
                        appItems.clear();
                        appItems.addAll(found);
                        appList.removeAllViews();
                        if (appItems.isEmpty()) {
                            TextView e = UI.note(act, "✦  未发现明显的应用缓存");
                            e.setGravity(Gravity.CENTER);
                            appList.addView(e);
                            return;
                        }
                        for (Object[] o : appItems) {
                            LinearLayout r = UI.row(act);
                            r.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
                            CheckBox cb = UI.check(act, true);
                            cb.setTag(o[0]);
                            r.addView(cb);
                            LinearLayout info = UI.col(act);
                            info.addView(UI.text(act, (String) o[1], 12.5f, Theme.TEXT));
                            info.addView(UI.note(act, (String) o[0]));
                            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, UI.WC, 1f);
                            ip.leftMargin = Theme.dp(act, 6);
                            r.addView(info, ip);
                            r.addView(UI.text(act, Util.fmtSize((Long) o[2]), 11.5f, Theme.ACCENT));
                            appList.addView(r);
                        }
                    }
                });
            }
        }).start();
    }

    private void cleanApps() {
        final List<String> pkgs = new ArrayList<String>();
        for (int i = 0; i < appList.getChildCount(); i++) {
            View v = appList.getChildAt(i);
            if (!(v instanceof LinearLayout)) continue;
            View first = ((LinearLayout) v).getChildAt(0);
            if (first instanceof CheckBox && ((CheckBox) first).isChecked()) {
                Object tag = first.getTag();
                if (tag != null) pkgs.add(String.valueOf(tag));
            }
        }
        if (pkgs.isEmpty()) { act.toast("未选中应用"); return; }
        new Thread(new Runnable() {
            public void run() {
                CleanEngine eng = new CleanEngine();
                long freed = 0;
                for (String p : pkgs) {
                    freed += eng.cleanAppCache(p);
                    File ext = new File(Util.sdRoot() + "/Android/data/" + p + "/cache");
                    long s = Util.dirSize(ext);
                    if (Util.rmrf(ext)) freed += s;
                }
                final long f = freed;
                ui.post(new Runnable() {
                    public void run() {
                        act.store.addStat(f, pkgs.size());
                        act.toast("已清理 " + pkgs.size() + " 个应用 · 释放 " + Util.fmtSize(f));
                        scanApps();
                    }
                });
            }
        }).start();
    }

    // ---------- 缩略图 ----------

    private View thumbCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.title(act, "🖼  缩略图缓存"));
        c.addView(UI.note(act, "相册/图库生成的预览缓存，删除后会自动重建"));
        final TextView res = UI.note(act, "");
        Button go = UI.primary(act, "扫描并清理");
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        String sd = Util.sdRoot();
                        String[] dirs = {sd + "/DCIM/.thumbnails", sd + "/Pictures/.thumbnails",
                                sd + "/.thumbnails"};
                        long freed = 0;
                        int n = 0;
                        for (String d : dirs) {
                            File f = new File(d);
                            if (!f.isDirectory()) continue;
                            long s = Util.dirSize(f);
                            if (s > 0 && Util.rmrf(f)) { freed += s; n++; }
                        }
                        final long ff = freed; final int nn = n;
                        ui.post(new Runnable() {
                            public void run() {
                                res.setText(nn == 0 ? "未发现缩略图缓存"
                                        : "已清理 " + nn + " 个目录 · 释放 " + Util.fmtSize(ff));
                                if (ff > 0) act.store.addStat(ff, nn);
                            }
                        });
                    }
                }).start();
            }
        });
        c.addView(go, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        c.addView(res, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    // ---------- 公共 ----------

    private View itemRow(final JunkItem it) {
        LinearLayout r = UI.row(act);
        r.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
        final CheckBox cb = UI.check(act, it.checked);
        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) { it.checked = on; }
        });
        r.addView(cb);
        TextView nm = UI.text(act, it.name, 12, Theme.MUTED);
        nm.setSingleLine(true);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        r.addView(nm, new LinearLayout.LayoutParams(0, UI.WC, 1f));
        r.addView(UI.text(act, Util.fmtSize(it.size), 11.5f, Theme.DIM));
        return r;
    }

    private void delSelected(final List<JunkItem> pool, final LinearLayout box) {
        final List<JunkItem> sel = new ArrayList<JunkItem>();
        for (JunkItem it : pool) if (it.checked) sel.add(it);
        if (sel.isEmpty()) { act.toast("未选中项目"); return; }
        new Thread(new Runnable() {
            public void run() {
                long freed = 0;
                int n = 0;
                for (JunkItem it : sel) {
                    File f = new File(it.path);
                    long s = it.size;
                    boolean ok = Util.rmrf(f);
                    if (!ok && Shell.hasRoot()) {
                        Shell.exec(true, "rm -rf '" + it.path.replace("'", "") + "'");
                        ok = !f.exists();
                    }
                    if (ok) { freed += s; n++; pool.remove(it); }
                }
                final long ff = freed; final int nn = n;
                ui.post(new Runnable() {
                    public void run() {
                        act.store.addStat(ff, nn);
                        act.toast("已删除 " + nn + " 项 · 释放 " + Util.fmtSize(ff));
                        box.removeAllViews();
                        for (JunkItem it : pool) box.addView(itemRow(it));
                    }
                });
            }
        }).start();
    }
}
