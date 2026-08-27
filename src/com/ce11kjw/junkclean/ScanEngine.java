package com.ce11kjw.junkclean;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 扫描引擎：5 大分类，有 root 走 root 路径，无 root 降级到公共目录 */
public class ScanEngine {

    public interface Progress {
        void onCategory(String name);
        void onDone(List<JunkCategory> cats);
    }

    private final Context ctx;
    private final Set<String> whitelist = new HashSet<String>();
    private final boolean root;

    public ScanEngine(Context ctx, List<String> wl) {
        this.ctx = ctx;
        this.root = Shell.hasRoot();
        if (wl != null) whitelist.addAll(wl);
    }

    public boolean isRoot() { return root; }

    public List<JunkCategory> scan(Progress cb) {
        List<JunkCategory> cats = new ArrayList<JunkCategory>();

        JunkCategory appCache = new JunkCategory("cache", "应用缓存",
                root ? "所有应用的 cache 目录" : "本应用缓存（无 root 受限）", "📦", false, false);
        if (cb != null) cb.onCategory(appCache.name);
        scanAppCache(appCache);
        cats.add(appCache);

        JunkCategory webview = new JunkCategory("webview", "WebView 缓存",
                "内置浏览器内核缓存", "🌐", false, false);
        if (cb != null) cb.onCategory(webview.name);
        scanWebView(webview);
        cats.add(webview);

        JunkCategory logs = new JunkCategory("log", "日志文件",
                "崩溃日志 / tombstone / anr", "📄", false, true);
        if (cb != null) cb.onCategory(logs.name);
        scanLogs(logs);
        cats.add(logs);

        JunkCategory temp = new JunkCategory("temp", "临时文件",
                "sdcard 上的 .tmp/.temp/.part 等", "🗂", false, false);
        if (cb != null) cb.onCategory(temp.name);
        scanTemp(temp);
        cats.add(temp);

        JunkCategory thumb = new JunkCategory("thumb", "缩略图缓存",
                ".thumbnails / 相册预览缓存", "🖼", false, false);
        if (cb != null) cb.onCategory(thumb.name);
        scanThumbs(thumb);
        cats.add(thumb);

        JunkCategory residue = new JunkCategory("residue", "应用残留",
                "已卸载应用留下的数据目录", "🧹", true, false);
        if (cb != null) cb.onCategory(residue.name);
        scanResidue(residue);
        cats.add(residue);

        if (cb != null) cb.onDone(cats);
        return cats;
    }

    // ---------- 各分类实现 ----------

    private void scanAppCache(JunkCategory c) {
        if (root) {
            // root：遍历 /data/data/*/cache
            List<String> out = Shell.exec(true,
                    "for d in /data/data/*/cache /data/data/*/code_cache; do " +
                    "[ -d \"$d\" ] && echo \"$(du -sk \"$d\" 2>/dev/null | cut -f1) $d\"; done");
            for (String l : out) {
                String[] p = l.trim().split("\\s+", 2);
                if (p.length != 2) continue;
                try {
                    long kb = Long.parseLong(p[0]);
                    if (kb <= 0) continue;
                    String path = p[1];
                    String pkg = pkgFromPath(path);
                    if (whitelist.contains(pkg)) continue;
                    c.items.add(new JunkItem(path, pkg + " / " + new File(path).getName(), kb * 1024));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            // 无 root：只能清自己 + Android/data 下可读的
            File own = ctx.getCacheDir();
            long s = Util.dirSize(own);
            if (s > 0) c.items.add(new JunkItem(own.getAbsolutePath(), "JunkClean 自身缓存", s));
            File extRoot = new File(Util.sdRoot() + "/Android/data");
            File[] pkgs = extRoot.listFiles();
            if (pkgs != null) {
                for (File p : pkgs) {
                    if (whitelist.contains(p.getName())) continue;
                    File cache = new File(p, "cache");
                    long sz = Util.dirSize(cache);
                    if (sz > 0) c.items.add(new JunkItem(cache.getAbsolutePath(),
                            p.getName() + " / cache", sz));
                }
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
            long s = root ? duKb(d) * 1024 : Util.dirSize(new File(d));
            if (s > 0) c.items.add(new JunkItem(d, pkg + " / webview", s));
        }
    }

    private void scanLogs(JunkCategory c) {
        if (!root) return;
        String[] paths = {"/data/tombstones", "/data/anr", "/data/log",
                "/data/system/dropbox", "/cache/recovery"};
        for (String p : paths) {
            long kb = duKb(p);
            if (kb > 0) c.items.add(new JunkItem(p, p, kb * 1024));
        }
    }

    private void scanTemp(JunkCategory c) {
        String[] exts = {".tmp", ".temp", ".part", ".crdownload", ".download", ".log"};
        walk(new File(Util.sdRoot()), 0, 6, c, exts, null);
    }

    private void scanThumbs(JunkCategory c) {
        String sd = Util.sdRoot();
        String[] dirs = {
                sd + "/DCIM/.thumbnails", sd + "/Pictures/.thumbnails",
                sd + "/.thumbnails", sd + "/Android/data/com.miui.gallery/cache",
                sd + "/MIUI/Gallery/cloud/.cache", sd + "/tencent/MicroMsg/Cache"
        };
        for (String d : dirs) {
            File f = new File(d);
            if (!f.isDirectory()) continue;
            long s = Util.dirSize(f);
            if (s > 0) c.items.add(new JunkItem(d, d.replace(sd, "…"), s));
        }
    }

    private void scanResidue(JunkCategory c) {
        PackageManager pm = ctx.getPackageManager();
        Set<String> installed = new HashSet<String>();
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            for (ApplicationInfo a : apps) installed.add(a.packageName);
        } catch (Exception ignored) {}

        File extData = new File(Util.sdRoot() + "/Android/data");
        File[] dirs = extData.listFiles();
        if (dirs == null) return;
        for (File d : dirs) {
            String pkg = d.getName();
            if (installed.contains(pkg) || whitelist.contains(pkg)) continue;
            if (!pkg.contains(".")) continue;   // 不像包名的跳过
            long s = Util.dirSize(d);
            if (s > 0) c.items.add(new JunkItem(d.getAbsolutePath(), pkg + "（已卸载）", s));
        }
    }

    // ---------- 工具 ----------

    private void walk(File dir, int depth, int maxDepth, JunkCategory c,
                      String[] exts, String[] names) {
        if (depth > maxDepth || dir == null) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            String n = f.getName();
            if (n.equals("Android") || n.startsWith(".junkclean")) continue;
            if (f.isDirectory()) {
                walk(f, depth + 1, maxDepth, c, exts, names);
                continue;
            }
            if (whitelist.contains(n)) continue;
            String low = n.toLowerCase(java.util.Locale.US);
            boolean hit = false;
            if (exts != null) for (String e : exts) if (low.endsWith(e)) { hit = true; break; }
            if (!hit && names != null) for (String x : names) if (low.equals(x)) { hit = true; break; }
            if (hit && f.length() > 0) {
                c.items.add(new JunkItem(f.getAbsolutePath(), n, f.length()));
            }
        }
    }

    private long duKb(String path) {
        String s = Shell.one(root, "du -sk " + path + " 2>/dev/null | cut -f1");
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }

    private String pkgFromPath(String path) {
        String[] seg = path.split("/");
        for (int i = 0; i < seg.length; i++) {
            if (seg[i].contains(".") && i + 1 < seg.length) return seg[i];
        }
        return seg.length > 3 ? seg[3] : path;
    }
}
