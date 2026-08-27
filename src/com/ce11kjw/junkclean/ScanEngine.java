package com.ce11kjw.junkclean;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 扫描引擎：8 分类，root 优先，无 root 降级；支持分类开关 + 缓存 */
public class ScanEngine {

    public interface Progress {
        void onCategory(String name, int index, int total);
    }

    private static List<JunkCategory> cache;
    private static long cacheTime;
    private static final long CACHE_TTL = 60000;

    private final Context ctx;
    private final Store store;
    private final Set<String> whitelist = new HashSet<String>();
    private final boolean root;

    public ScanEngine(Context ctx, Store store) {
        this.ctx = ctx;
        this.store = store;
        this.root = Shell.hasRoot();
        whitelist.addAll(store.whitelist());
    }

    public static void invalidate() { cache = null; }

    public static boolean hasCache() {
        return cache != null && System.currentTimeMillis() - cacheTime < CACHE_TTL;
    }

    public List<JunkCategory> scan(boolean force, Progress cb) {
        if (!force && hasCache()) return cache;

        List<JunkCategory> cats = new ArrayList<JunkCategory>();
        String[][] defs = {
                {"cache",    "应用缓存",   root ? "所有应用的 cache / code_cache" : "可访问的外部缓存", "📦", "0", "0"},
                {"webview",  "WebView 缓存", "内置浏览器内核缓存", "🌐", "0", "0"},
                {"log",      "日志文件",   "tombstone / anr / dropbox", "📄", "0", "1"},
                {"temp",     "临时文件",   ".tmp / .part / .crdownload 等", "🗂", "0", "0"},
                {"thumb",    "缩略图缓存", "相册与图库预览缓存", "🖼", "0", "0"},
                {"apkjunk",  "冗余安装包", "已安装应用对应的 apk 文件", "📥", "1", "0"},
                {"emptyjunk","空文件",     "0 字节文件与空目录", "🫙", "0", "0"},
                {"residue",  "应用残留",   "已卸载应用留下的数据目录", "🧹", "1", "0"},
        };

        int total = defs.length;
        for (int i = 0; i < defs.length; i++) {
            String[] d = defs[i];
            if (!store.catEnabled(d[0])) continue;
            JunkCategory c = new JunkCategory(d[0], d[1], d[2], d[3],
                    "1".equals(d[4]), "1".equals(d[5]));
            if (cb != null) cb.onCategory(c.name, i + 1, total);
            if (c.needRoot && !root) { cats.add(c); continue; }

            if ("cache".equals(c.id))          scanAppCache(c);
            else if ("webview".equals(c.id))   scanWebView(c);
            else if ("log".equals(c.id))       scanLogs(c);
            else if ("temp".equals(c.id))      scanTemp(c);
            else if ("thumb".equals(c.id))     scanThumbs(c);
            else if ("apkjunk".equals(c.id))   scanApkJunk(c);
            else if ("emptyjunk".equals(c.id)) scanEmpty(c);
            else if ("residue".equals(c.id))   scanResidue(c);

            cats.add(c);
        }

        cache = cats;
        cacheTime = System.currentTimeMillis();
        return cats;
    }

    private String scanRoot() {
        String r = store.scanRoot();
        return r == null || r.trim().isEmpty() ? Util.sdRoot() : r.trim();
    }

    // ---------- 分类实现 ----------

    private void scanAppCache(JunkCategory c) {
        if (root) {
            List<String> out = Shell.exec(true,
                    "for d in /data/data/*/cache /data/data/*/code_cache; do " +
                    "[ -d \"$d\" ] && echo \"$(du -sk \\\"$d\\\" 2>/dev/null | cut -f1)|$d\"; done");
            for (String l : out) {
                int bar = l.indexOf('|');
                if (bar <= 0) continue;
                try {
                    long kb = Long.parseLong(l.substring(0, bar).trim());
                    if (kb <= 4) continue;
                    String path = l.substring(bar + 1);
                    String pkg = pkgFromPath(path);
                    if (whitelist.contains(pkg)) continue;
                    c.items.add(new JunkItem(path, pkg + " / " + new File(path).getName(), kb * 1024));
                } catch (NumberFormatException ignored) {}
            }
        }
        File own = ctx.getCacheDir();
        long s = Util.dirSize(own);
        if (s > 0) c.items.add(new JunkItem(own.getAbsolutePath(), "JunkClean 自身缓存", s));

        File[] pkgs = new File(Util.sdRoot() + "/Android/data").listFiles();
        if (pkgs != null) {
            for (File p : pkgs) {
                if (whitelist.contains(p.getName())) continue;
                File cache = new File(p, "cache");
                long sz = Util.dirSize(cache);
                if (sz > 65536) c.items.add(new JunkItem(cache.getAbsolutePath(),
                        p.getName() + " / 外部缓存", sz));
            }
        }
    }

    private void scanWebView(JunkCategory c) {
        List<String> dirs = new ArrayList<String>();
        if (root) {
            dirs.addAll(Shell.exec(true,
                    "ls -d /data/data/*/app_webview /data/data/*/app_chrome 2>/dev/null"));
        }
        File own = new File(ctx.getApplicationInfo().dataDir, "app_webview");
        if (own.exists()) dirs.add(own.getAbsolutePath());
        for (String d : dirs) {
            d = d.trim();
            if (d.isEmpty()) continue;
            String pkg = pkgFromPath(d);
            if (whitelist.contains(pkg)) continue;
            long s = root ? Shell.du(d) : Util.dirSize(new File(d));
            if (s > 65536) c.items.add(new JunkItem(d, pkg + " / webview", s));
        }
    }

    private void scanLogs(JunkCategory c) {
        for (String p : new String[]{"/data/tombstones", "/data/anr", "/data/log",
                "/data/system/dropbox", "/cache/recovery", "/data/local/tmp"}) {
            long s = Shell.du(p);
            if (s > 4096) c.items.add(new JunkItem(p, p, s));
        }
    }

    private void scanTemp(JunkCategory c) {
        String[] exts = {".tmp", ".temp", ".part", ".crdownload", ".download", ".log", ".bak", ".old"};
        walkExt(new File(scanRoot()), 0, 7, c, exts);
    }

    private void scanThumbs(JunkCategory c) {
        c.items.addAll(Finder.thumbs());
    }

    /** 冗余安装包：sdcard 上的 apk 且对应包已安装 */
    private void scanApkJunk(JunkCategory c) {
        for (Finder.ApkInfo a : Finder.apks(ctx, scanRoot())) {
            if (!a.installed) continue;
            if (whitelist.contains(a.label)) continue;
            JunkItem it = new JunkItem(a.path, a.label + "（已安装）", a.size);
            c.items.add(it);
        }
    }

    private void scanEmpty(JunkCategory c) {
        c.items.addAll(Finder.empties(scanRoot(), true, 150));
    }

    private void scanResidue(JunkCategory c) {
        Set<String> installed = new HashSet<String>();
        try {
            for (ApplicationInfo a : ctx.getPackageManager().getInstalledApplications(0))
                installed.add(a.packageName);
        } catch (Exception ignored) {}

        // 只扫 data/obb（缓存与数据包）；Android/media 常存用户拍摄内容，不纳入自动清理
        for (String base : new String[]{Util.sdRoot() + "/Android/data",
                                        Util.sdRoot() + "/Android/obb"}) {
            File[] dirs = new File(base).listFiles();
            if (dirs == null) continue;
            for (File d : dirs) {
                String pkg = d.getName();
                if (installed.contains(pkg) || whitelist.contains(pkg)) continue;
                if (!pkg.contains(".")) continue;
                long s = Util.dirSize(d);
                if (s > 0) c.items.add(new JunkItem(d.getAbsolutePath(),
                        pkg + "（已卸载）", s));
            }
        }
    }

    // ---------- 工具 ----------

    private void walkExt(File dir, int depth, int maxDepth, JunkCategory c, String[] exts) {
        if (depth > maxDepth || c.items.size() > 300) return;
        if (dir.getName().equals(".junkclean_trash")) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            String n = f.getName();
            if (n.equals("Android") || n.startsWith(".")) continue;
            if (f.isDirectory()) { walkExt(f, depth + 1, maxDepth, c, exts); continue; }
            if (whitelist.contains(n)) continue;
            String low = n.toLowerCase(Locale.US);
            for (String e : exts) {
                if (low.endsWith(e) && f.length() > 0) {
                    c.items.add(new JunkItem(f.getAbsolutePath(), n, f.length()));
                    break;
                }
            }
        }
    }

    private String pkgFromPath(String path) {
        String[] seg = path.split("/");
        for (int i = 0; i < seg.length; i++) {
            if (seg[i].contains(".") && i + 1 < seg.length) return seg[i];
        }
        return seg.length > 3 ? seg[3] : path;
    }
}
