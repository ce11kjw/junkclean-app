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

    /** 白名单匹配：文件名完全相等，或路径包含该条目 */
    public static boolean inWhitelist(List<String> wl, String nameOrPath) {
        if (wl == null || wl.isEmpty() || nameOrPath == null) return false;
        for (String w : wl) {
            if (w.isEmpty()) continue;
            if (nameOrPath.equals(w)) return true;
            if (nameOrPath.contains("/" + w + "/") || nameOrPath.endsWith("/" + w)) return true;
        }
        return false;
    }

    public static boolean matchType(String name, String type) {
        if (type == null || "all".equals(type)) return true;
        String[] exts = TYPE_EXT.get(type);
        if (exts == null) return true;
        String low = name.toLowerCase(Locale.US);
        for (String e : exts) if (low.endsWith(e)) return true;
        return false;
    }

    /** 全盘扫描时的系统分区（需 root） */
    public static final String[] SYSTEM_ROOTS = {
            "/data/data", "/data/local/tmp", "/data/media/0", "/data/app",
            "/cache", "/data/log", "/data/tombstones", "/data/anr",
            "/data/system/dropbox", "/data/vendor", "/data/misc"
    };

    /** 返回本次要遍历的根目录：普通模式只有 sdcard，全盘模式追加系统分区 */
    public static List<String> roots(String sdRoot, boolean full) {
        List<String> out = new ArrayList<String>();
        out.add(sdRoot);
        if (full && Shell.hasRoot()) {
            for (String p : SYSTEM_ROOTS) {
                if (p.startsWith(sdRoot)) continue;
                if (new File(p).isDirectory()) out.add(p);
            }
        }
        return out;
    }

    // ---------- 大文件 ----------

    /** minBytes 阈值，days>0 时只要 N 天前的旧文件 */
    public static List<JunkItem> big(String root, long minBytes, int days, List<String> wl, int cap) {
        return big(root, minBytes, days, wl, cap, false);
    }

    public static List<JunkItem> big(String root, long minBytes, int days, List<String> wl,
                                     int cap, boolean full) {
        List<JunkItem> out = new ArrayList<JunkItem>();
        long before = days > 0 ? System.currentTimeMillis() - days * 86400000L : Long.MAX_VALUE;
        for (String r : roots(root, full)) {
            walk(new File(r), 0, 9, out, cap, minBytes, before, wl);
        }
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
                // 跳过 Android 目录：obb/data 是应用与游戏数据包，误删代价大
                if (depth == 0 && n.equals("Android")) continue;
                walk(f, depth + 1, maxDepth, out, cap, minBytes, before, wl);
                continue;
            }
            if (inWhitelist(wl, n) || inWhitelist(wl, f.getAbsolutePath())) continue;
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
    public static List<JunkItem> dirRank(String root, int top, List<String> wl) {
        return dirRank(root, top, wl, false, 2);
    }

    public static List<JunkItem> dirRank(String root, int top, List<String> wl, boolean full) {
        return dirRank(root, top, wl, full, 2);
    }

    /**
     * 目录体积排行。depth=1 只列一级；depth=2 会把体积占比高的一级目录展开到二级，
     * 这样能看到「Android/data 里具体是哪个应用大」而不是只有一个 Android 条目。
     */
    public static List<JunkItem> dirRank(String root, int top, List<String> wl,
                                         boolean full, int depth) {
        // 两组分别排序、各取前 N，再合并：用户一级目录填满 top，
        // 系统分区只在 full 模式且还有余位时补足。这样用户目录永远占据榜首，
        // 不再被 /data/* 这种几十 GB 的系统分区挤掉。
        List<JunkItem> firstLevel = new ArrayList<JunkItem>();
        File[] fs = new File(root).listFiles();
        if (fs != null) {
            for (File f : fs) {
                if (!f.isDirectory() || f.getName().startsWith(".")) continue;
                if (inWhitelist(wl, f.getName())) continue;
                long s = Util.dirSize(f);
                if (s > 0) firstLevel.add(mk(f.getAbsolutePath(), f.getName(), s));
            }
        }
        Collections.sort(firstLevel, BY_SIZE);
        List<JunkItem> userTop = firstLevel.size() > top
                ? firstLevel.subList(0, top) : firstLevel;

        List<JunkItem> sysTop = new ArrayList<JunkItem>();
        if (full && Shell.hasRoot()) {
            for (String p : SYSTEM_ROOTS) {
                if (!new File(p).isDirectory()) continue;
                long s = Shell.du(p);
                if (s > 0) sysTop.add(mk(p, "[系统] " + p, s));
            }
            Collections.sort(sysTop, BY_SIZE);
            // 系统目录最多占 top/2 位，避免喧宾夺主
            int sysN = Math.min(sysTop.size(), top / 2);
            sysTop = sysTop.subList(0, sysN);
        }

        // 合并：用户目录 + 系统分区，统一按大小降序。
        // 不再做二级展开 —— 之前把 300MB 的子目录和 50GB 的父目录混排，
        // 中间 1GB/2GB 的目录被 top 截断挤掉，用户看到「50GB 第一、300MB 第二」。
        // 排行只看一级：父目录代表整个文件夹大小，子目录去文件浏览里看。
        List<JunkItem> out = new ArrayList<JunkItem>(userTop);
        for (JunkItem s : sysTop) {
            s.checked = false;
            out.add(s);
        }
        // 纯大小降序，不截断到 top 之前（top 只是分页上限）
        Collections.sort(out, BY_SIZE);
        return out.size() > top ? new ArrayList<JunkItem>(out.subList(0, top)) : out;
    }

    private static JunkItem mk(String path, String name, long size) {
        JunkItem it = new JunkItem(path, name, size);
        it.checked = false;
        return it;
    }

    private static final Comparator<JunkItem> BY_SIZE = new Comparator<JunkItem>() {
        public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
    };

    private static List<JunkItem> sortBySize(List<JunkItem> out, int top) {
        Collections.sort(out, new Comparator<JunkItem>() {
            public int compare(JunkItem a, JunkItem b) { return Long.compare(b.size, a.size); }
        });
        return out.size() > top ? new ArrayList<JunkItem>(out.subList(0, top)) : out;
    }

    // ---------- 空文件 / 空目录 ----------

    public static List<JunkItem> empties(String root, boolean includeDirs, int cap,
                                         List<String> wl) {
        return empties(root, includeDirs, cap, wl, false);
    }

    public static List<JunkItem> empties(String root, boolean includeDirs, int cap,
                                         List<String> wl, boolean full) {
        List<JunkItem> out = new ArrayList<JunkItem>();
        for (String r : roots(root, full)) {
            walkEmpty(new File(r), 0, out, includeDirs, cap, wl);
        }
        return out;
    }

    private static void walkEmpty(File dir, int depth, List<JunkItem> out,
                                  boolean includeDirs, int cap, List<String> wl) {
        if (depth > 9 || out.size() >= cap) return;
        if (dir.getName().equals(".junkclean_trash")) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".") || inWhitelist(wl, f.getName())) continue;
            if (f.isDirectory()) {
                File[] sub = f.listFiles();
                if (includeDirs && sub != null && sub.length == 0) {
                    JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName() + "/  空目录", 0);
                    it.checked = false;
                    out.add(it);
                } else {
                    walkEmpty(f, depth + 1, out, includeDirs, cap, wl);
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
    public static List<DupGroup> duplicates(String root, long minSize, int maxGroups,
                                            List<String> wl) {
        return duplicates(root, minSize, maxGroups, wl, false);
    }

    public static List<DupGroup> duplicates(String root, long minSize, int maxGroups,
                                            List<String> wl, boolean full) {
        Map<Long, List<File>> bySize = new HashMap<Long, List<File>>();
        for (String r : roots(root, full)) {
            collectBySize(new File(r), 0, bySize, minSize, wl);
        }

        List<DupGroup> groups = new ArrayList<DupGroup>();
        for (Map.Entry<Long, List<File>> e : bySize.entrySet()) {
            if (e.getValue().size() < 2) continue;
            // 同桶文件并发算哈希：单文件哈希是 IO 密集型，串行时 CPU 空等
            final Map<String, List<File>> byHash =
                    java.util.Collections.synchronizedMap(new HashMap<String, List<File>>());
            hashPool(e.getValue(), byHash);
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

    /** 并发计算一组文件的哈希并按哈希分桶 */
    private static void hashPool(List<File> files, final Map<String, List<File>> out) {
        if (files.size() <= 2) {
            for (File f : files) putHash(out, Util.quickHash(f), f);
            return;
        }
        // 固定 4 个 worker 从共享游标取任务，而不是每个文件一个线程。
        // 同尺寸文件可能上百个，一文件一线程会把设备线程数打满。
        final java.util.concurrent.atomic.AtomicInteger cursor =
                new java.util.concurrent.atomic.AtomicInteger();
        final List<File> work = files;
        int workers = Math.min(4, work.size());
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(workers);
        for (int w = 0; w < workers; w++) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        int idx;
                        while ((idx = cursor.getAndIncrement()) < work.size()) {
                            File one = work.get(idx);
                            try { putHash(out, Util.quickHash(one), one); }
                            catch (Throwable ignored) {}
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        try {
            latch.await(90, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }

    private static void putHash(Map<String, List<File>> map, String h, File f) {
        synchronized (map) {
            List<File> l = map.get(h);
            if (l == null) { l = new ArrayList<File>(); map.put(h, l); }
            l.add(f);
        }
    }

    private static List<DupGroup> sortGroups(List<DupGroup> g) {
        Collections.sort(g, new Comparator<DupGroup>() {
            public int compare(DupGroup a, DupGroup b) {
                return Long.compare(b.size * b.files.size(), a.size * a.files.size());
            }
        });
        return g;
    }

    private static void collectBySize(File dir, int depth, Map<Long, List<File>> map,
                                      long minSize, List<String> wl) {
        if (depth > 8 || map.size() > 8000) return;
        if (dir.getName().equals(".junkclean_trash")) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) { collectBySize(f, depth + 1, map, minSize, wl); continue; }
            if (f.length() < minSize || inWhitelist(wl, f.getName())) continue;
            List<File> l = map.get(f.length());
            if (l == null) { l = new ArrayList<File>(); map.put(f.length(), l); }
            l.add(f);
        }
    }

    /** 保留策略：newest / oldest / shortest / largest（其余标记为待删） */
    public static void applyKeepPolicy(List<DupGroup> groups, String policy) {
        for (DupGroup g : groups) {
            int keep = 0;
            for (int i = 1; i < g.files.size(); i++) {
                JunkItem a = g.files.get(keep), b = g.files.get(i);
                boolean better;
                if ("oldest".equals(policy)) better = b.mtime < a.mtime;
                else if ("shortest".equals(policy)) better = b.path.length() < a.path.length();
                else if ("largest".equals(policy)) {
                    // 同组文件大小相同时退化为比较修改时间，保证策略稳定
                    better = b.size > a.size || (b.size == a.size && b.mtime > a.mtime);
                } else better = b.mtime > a.mtime;      // newest
                if (better) keep = i;
            }
            for (int i = 0; i < g.files.size(); i++) g.files.get(i).checked = (i != keep);
        }
    }

    // ---------- 缩略图 ----------

    public static List<JunkItem> thumbs(List<String> wl) {
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
            if (inWhitelist(wl, f.getName()) || inWhitelist(wl, d)) continue;
            long s = Util.dirSize(f);
            if (s > 0) out.add(new JunkItem(d, Util.shortPath(d), s));
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
    public static List<ApkInfo> apks(Context ctx, String root, List<String> wl) {
        return apks(ctx, root, wl, false);
    }

    public static List<ApkInfo> apks(Context ctx, String root, List<String> wl, boolean full) {
        Set<String> installed = new HashSet<String>();
        PackageManager pm = ctx.getPackageManager();
        try {
            for (ApplicationInfo a : pm.getInstalledApplications(0)) installed.add(a.packageName);
        } catch (Exception ignored) {}

        List<JunkItem> files = new ArrayList<JunkItem>();
        for (String r : roots(root, full)) walkApk(new File(r), 0, files);

        List<ApkInfo> out = new ArrayList<ApkInfo>();
        for (JunkItem f : files) {
            if (inWhitelist(wl, f.name)) continue;
            ApkInfo a = new ApkInfo();
            a.path = f.path;
            a.label = f.name;
            a.size = f.size;
            a.pkg = pkgOfApk(ctx, f.path);
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
        if (dir.getName().equals(".junkclean_trash")) return;
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

    /** 用 PackageManager 读 apk 包名（纯 Java API，设备上无需 aapt） */
    private static String pkgOfApk(Context ctx, String path) {
        try {
            android.content.pm.PackageInfo pi = ctx.getPackageManager()
                    .getPackageArchiveInfo(path, 0);
            if (pi != null && pi.packageName != null) return pi.packageName;
        } catch (Exception ignored) {}
        return null;
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
            if (inWhitelist(wl, a.packageName)) continue;
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

    // ============================================================
    // 感知哈希重复检测：照片/视频的「构图相似」匹配
    // ============================================================

    public static final int DEFAULT_VISUAL_THRESHOLD = 12;

    /** 仅扫描图片和视频 */
    public static List<DupGroup> visualDuplicates(String root, List<String> wl,
                                                final int threshold) {
        final List<File> media = new ArrayList<File>();
        collectMedia(new File(root), 0, media, wl);
        if (media.size() < 2) return new ArrayList<DupGroup>();

        // 并发算 aHash
        final java.util.Map<String, Long> hashes =
                new java.util.concurrent.ConcurrentHashMap<String, Long>();
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(media.size());
        final java.util.concurrent.Semaphore gate =
                new java.util.concurrent.Semaphore(4);
        for (final File f : media) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        gate.acquire();
                        long h = PerceptualHash.aHash(f);
                        if (h != 0) hashes.put(f.getAbsolutePath(), h);
                    } catch (InterruptedException ignored) {
                    } finally {
                        gate.release();
                        latch.countDown();
                    }
                }
            }).start();
        }
        try { latch.await(5, java.util.concurrent.TimeUnit.MINUTES); }
        catch (InterruptedException ignored) {}

        Map<Long, List<File>> byHash = new HashMap<Long, List<File>>();
        for (File f : media) {
            Long h = hashes.get(f.getAbsolutePath());
            if (h == null) continue;
            List<File> bucket = byHash.get(h);
            if (bucket == null) { bucket = new ArrayList<File>(); byHash.put(h, bucket); }
            bucket.add(f);
        }
        // 哈希距离 ≤ threshold 视为相似
        List<DupGroup> groups = new ArrayList<DupGroup>();
        List<Long> sortedKeys = new ArrayList<Long>(byHash.keySet());
        Collections.sort(sortedKeys);
        for (int i = 0; i < sortedKeys.size(); i++) {
            for (int j = i + 1; j < sortedKeys.size(); j++) {
                long hi = sortedKeys.get(i), hj = sortedKeys.get(j);
                if (PerceptualHash.distance(hi, hj) <= threshold) {
                    List<File> bucket = new ArrayList<File>(byHash.get(hi));
                    bucket.addAll(byHash.get(hj));
                    DupGroup dg = new DupGroup();
                    dg.size = bucket.get(0).length();
                    dg.name = bucket.get(0).getName();
                    for (File f : bucket) {
                        JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName(), f.length());
                        it.checked = false;
                        it.mtime = f.lastModified();
                        dg.files.add(it);
                    }
                    groups.add(dg);
                }
            }
        }
        Collections.sort(groups, new Comparator<DupGroup>() {
            public int compare(DupGroup a, DupGroup b) {
                return Long.compare(b.size * b.files.size(), a.size * a.files.size());
            }
        });
        return groups;
    }

    private static void collectMedia(File dir, int depth, List<File> out, List<String> wl) {
        if (depth > 8) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !inWhitelist(wl, f.getName())) {
                    collectMedia(f, depth + 1, out, wl);
                }
            } else if (PerceptualHash.isImage(f) || PerceptualHash.isVideo(f)) {
                if (f.length() > 4096 && !inWhitelist(wl, f.getName())) {
                    out.add(f);
                }
            }
        }
    }

}
