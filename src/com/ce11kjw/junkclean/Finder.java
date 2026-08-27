package com.ce11kjw.junkclean;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 各专项查找器：大文件 / 空项 / 重复 / 缩略图 / APK / 应用缓存 */
public final class Finder {

    private Finder() {}

    public static final Map<String, String[]> TYPE_EXT = new LinkedHashMap<String, String[]>();
    static {
        TYPE_EXT.put("img", new String[]{".jpg",".jpeg",".png",".gif",".webp",".heic",".bmp",".avif",".tiff"});
        TYPE_EXT.put("vid", new String[]{".mp4",".mkv",".mov",".avi",".webm",".3gp",".m4v",".flv",".ts",".wmv"});
        TYPE_EXT.put("aud", new String[]{".mp3",".flac",".wav",".m4a",".ogg",".aac",".ape",".wma"});
        TYPE_EXT.put("doc", new String[]{".pdf",".doc",".docx",".xls",".xlsx",".ppt",".pptx",".txt",".epub",".csv"});
        TYPE_EXT.put("zip", new String[]{".zip",".7z",".rar",".tar",".gz",".xz",".bz2",".iso"});
        TYPE_EXT.put("apk", new String[]{".apk",".apks",".xapk",".apkm"});
    }

    public static boolean matchType(String name, String type) {
        if (type == null || "all".equals(type)) return true;
        String[] exts = TYPE_EXT.get(type);
        if (exts == null) return true;
        String low = name.toLowerCase(Locale.US);
        for (String e : exts) if (low.endsWith(e)) return true;
        return false;
    }

    // ---------- 大文件 ----------

    /** minBytes 阈值，days>0 时只要 N 天前的旧文件 */
    public static List<JunkItem> big(String root, long minBytes, int days, List<String> wl, int cap) {
        List<JunkItem> out = new ArrayList<JunkItem>();
        long before = days > 0 ? System.currentTimeMillis() - days * 86400000L : Long.MAX_VALUE;
        walk(new File(root), 0, 9, out, cap, minBytes, before, wl);
        Collections.sort(out, new Comparator<JunkItem>() {
            public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
        });
        return out.size() > cap ? new ArrayList<JunkItem>(out.subList(0, cap)) : out;
    }

    private static void walk(File dir, int depth, int maxDepth, List<JunkItem> out, int cap,
                             long minBytes, long before, List<String> wl) {
        if (depth > maxDepth || out.size() >= cap * 2) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            String n = f.getName();
            if (n.startsWith(".")) continue;
            if (f.isDirectory()) {
                walk(f, depth + 1, maxDepth, out, cap, minBytes, before, wl);
                continue;
            }
            if (wl != null && wl.contains(n)) continue;
            if (f.length() >= minBytes && f.lastModified() <= before) {
                JunkItem it = new JunkItem(f.getAbsolutePath(), n, f.length());
                it.checked = false;
                it.mtime = f.lastModified();
                out.add(it);
            }
        }
    }

    // ---------- 目录体积排行 ----------

    /** 一级子目录体积排行（找出谁占空间） */
    public static List<JunkItem> dirRank(String root, int top) {
        List<JunkItem> out = new ArrayList<JunkItem>();
        File[] fs = new File(root).listFiles();
        if (fs == null) return out;
        for (File f : fs) {
            if (!f.isDirectory() || f.getName().startsWith(".")) continue;
            long s = Util.dirSize(f);
            if (s > 0) {
                JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName(), s);
                it.checked = false;
                out.add(it);
            }
        }
        Collections.sort(out, new Comparator<JunkItem>() {
            public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
        });
        return out.size() > top ? new ArrayList<JunkItem>(out.subList(0, top)) : out;
    }

    // ---------- 空文件 / 空目录 ----------

    public static List<JunkItem> empties(String root, boolean includeDirs, int cap) {
        List<JunkItem> out = new ArrayList<JunkItem>();
        walkEmpty(new File(root), 0, out, includeDirs, cap);
        return out;
    }

    private static void walkEmpty(File dir, int depth, List<JunkItem> out,
                                  boolean includeDirs, int cap) {
        if (depth > 9 || out.size() >= cap) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                File[] sub = f.listFiles();
                if (includeDirs && sub != null && sub.length == 0) {
                    JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName() + "/  空目录", 0);
                    it.checked = false;
                    out.add(it);
                } else {
                    walkEmpty(f, depth + 1, out, includeDirs, cap);
                }
            } else if (f.length() == 0) {
                JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName(), 0);
                it.checked = false;
                out.add(it);
            }
        }
    }

    // ---------- 重复文件 ----------

    public static class DupGroup {
        public String name;
        public long size;
        public List<JunkItem> files = new ArrayList<JunkItem>();
    }

    /** 先按 大小 分桶，再对同桶算 quickHash 精确分组 */
    public static List<DupGroup> duplicates(String root, long minSize, int maxGroups) {
        Map<Long, List<File>> bySize = new HashMap<Long, List<File>>();
        collectBySize(new File(root), 0, bySize, minSize);

        List<DupGroup> groups = new ArrayList<DupGroup>();
        for (Map.Entry<Long, List<File>> e : bySize.entrySet()) {
            if (e.getValue().size() < 2) continue;
            Map<String, List<File>> byHash = new HashMap<String, List<File>>();
            for (File f : e.getValue()) {
                String h = Util.quickHash(f);
                List<File> l = byHash.get(h);
                if (l == null) { l = new ArrayList<File>(); byHash.put(h, l); }
                l.add(f);
            }
            for (Map.Entry<String, List<File>> g : byHash.entrySet()) {
                if (g.getValue().size() < 2) continue;
                DupGroup dg = new DupGroup();
                dg.size = e.getKey();
                dg.name = g.getValue().get(0).getName();
                for (File f : g.getValue()) {
                    JunkItem it = new JunkItem(f.getAbsolutePath(),
                            Util.shortPath(f.getAbsolutePath()), f.length());
                    it.checked = false;
                    it.mtime = f.lastModified();
                    dg.files.add(it);
                }
                groups.add(dg);
                if (groups.size() >= maxGroups) return sortGroups(groups);
            }
        }
        return sortGroups(groups);
    }

    private static List<DupGroup> sortGroups(List<DupGroup> g) {
        Collections.sort(g, new Comparator<DupGroup>() {
            public int compare(DupGroup a, DupGroup b) {
                return Long.compare(b.size * b.files.size(), a.size * a.files.size());
            }
        });
        return g;
    }

    private static void collectBySize(File dir, int depth, Map<Long, List<File>> map, long minSize) {
        if (depth > 8 || map.size() > 8000) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) { collectBySize(f, depth + 1, map, minSize); continue; }
            if (f.length() < minSize) continue;
            List<File> l = map.get(f.length());
            if (l == null) { l = new ArrayList<File>(); map.put(f.length(), l); }
            l.add(f);
        }
    }

    /** 保留策略：newest / oldest / shortest（其余标记为待删） */
    public static void applyKeepPolicy(List<DupGroup> groups, String policy) {
        for (DupGroup g : groups) {
            int keep = 0;
            for (int i = 1; i < g.files.size(); i++) {
                JunkItem a = g.files.get(keep), b = g.files.get(i);
                boolean better;
                if ("oldest".equals(policy)) better = b.mtime < a.mtime;
                else if ("shortest".equals(policy)) better = b.path.length() < a.path.length();
                else better = b.mtime > a.mtime;      // newest
                if (better) keep = i;
            }
            for (int i = 0; i < g.files.size(); i++) g.files.get(i).checked = (i != keep);
        }
    }

    // ---------- 缩略图 ----------

    public static List<JunkItem> thumbs() {
        String sd = Util.sdRoot();
        String[] dirs = {
                sd + "/DCIM/.thumbnails", sd + "/Pictures/.thumbnails", sd + "/.thumbnails",
                sd + "/MIUI/Gallery/cloud/.cache", sd + "/Android/data/com.miui.gallery/cache",
                sd + "/Android/data/com.google.android.apps.photos/cache",
                sd + "/tencent/MicroMsg/Cache", sd + "/Android/data/com.tencent.mm/cache",
                sd + "/Pictures/.face", sd + "/DCIM/.face"
        };
        List<JunkItem> out = new ArrayList<JunkItem>();
        for (String d : dirs) {
            File f = new File(d);
            if (!f.isDirectory()) continue;
            long s = Util.dirSize(f);
            if (s > 0) {
                JunkItem it = new JunkItem(d, Util.shortPath(d), s);
                out.add(it);
            }
        }
        return out;
    }

    // ---------- APK 管理 ----------

    public static class ApkInfo {
        public String path, label, pkg;
        public long size;
        public boolean installed;
        public boolean checked;
    }

    /** 扫描 sdcard 上的 apk，判断是否已安装（已安装的可安全删） */
    public static List<ApkInfo> apks(Context ctx, String root) {
        Set<String> installed = new HashSet<String>();
        PackageManager pm = ctx.getPackageManager();
        try {
            for (ApplicationInfo a : pm.getInstalledApplications(0)) installed.add(a.packageName);
        } catch (Exception ignored) {}

        List<JunkItem> files = new ArrayList<JunkItem>();
        walkApk(new File(root), 0, files);

        List<ApkInfo> out = new ArrayList<ApkInfo>();
        for (JunkItem f : files) {
            ApkInfo a = new ApkInfo();
            a.path = f.path;
            a.label = f.name;
            a.size = f.size;
            a.pkg = pkgOfApk(f.path);
            a.installed = a.pkg != null && installed.contains(a.pkg);
            a.checked = a.installed;      // 已安装的默认勾选（可删）
            out.add(a);
        }
        Collections.sort(out, new Comparator<ApkInfo>() {
            public int compare(ApkInfo x, ApkInfo y) { return Long.compare(y.size, x.size); }
        });
        return out;
    }

    private static void walkApk(File dir, int depth, List<JunkItem> out) {
        if (depth > 8 || out.size() > 200) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) { walkApk(f, depth + 1, out); continue; }
            String low = f.getName().toLowerCase(Locale.US);
            if (low.endsWith(".apk") || low.endsWith(".apks") || low.endsWith(".xapk")) {
                out.add(new JunkItem(f.getAbsolutePath(), f.getName(), f.length()));
            }
        }
    }

    /** 用 aapt/pm 读 apk 包名，失败返回 null */
    private static String pkgOfApk(String path) {
        String out = Shell.one(false, "aapt dump badging " + Shell.quote(path)
                + " 2>/dev/null | grep -m1 \"^package:\"");
        if (out.isEmpty()) return null;
        int i = out.indexOf("name='");
        if (i < 0) return null;
        int j = out.indexOf('\'', i + 6);
        return j < 0 ? null : out.substring(i + 6, j);
    }

    // ---------- 应用缓存排行 ----------

    public static class AppCache {
        public String pkg, label;
        public long size;
        public boolean checked = true;
    }

    public static List<AppCache> appCaches(Context ctx, List<String> wl) {
        PackageManager pm = ctx.getPackageManager();
        boolean root = Shell.hasRoot();
        List<AppCache> out = new ArrayList<AppCache>();
        List<ApplicationInfo> apps;
        try { apps = pm.getInstalledApplications(0); }
        catch (Exception e) { return out; }

        for (ApplicationInfo a : apps) {
            if ((a.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (wl != null && wl.contains(a.packageName)) continue;
            long sz = 0;
            if (root) {
                sz += Shell.du("/data/data/" + a.packageName + "/cache");
                sz += Shell.du("/data/data/" + a.packageName + "/code_cache");
            }
            sz += Util.dirSize(new File(Util.sdRoot() + "/Android/data/" + a.packageName + "/cache"));
            if (sz > 65536) {
                AppCache c = new AppCache();
                c.pkg = a.packageName;
                c.label = String.valueOf(pm.getApplicationLabel(a));
                c.size = sz;
                out.add(c);
            }
        }
        Collections.sort(out, new Comparator<AppCache>() {
            public int compare(AppCache x, AppCache y) { return Long.compare(y.size, x.size); }
        });
        return out;
    }
}
