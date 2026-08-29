package com.ce11kjw.junkclean;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 扫描引擎：8 分类，root 优先，无 root 降级；支持分类开关 + 缓存 */
public class ScanEngine {

    public interface Progress {
        /** foundItems / foundBytes 是截至此刻的累计发现量 */
        void onCategory(String name, int index, int total, int foundItems, long foundBytes);
        /** 返回 true 时中止后续分类扫描 */
        boolean cancelled();
    }

    private static List<JunkCategory> cache;
    private static long cacheTime;
    private static final long CACHE_TTL = 60000;

    private final Context ctx;
    private final Store store;
    private final List<String> whitelist;
    private final boolean root;

    public ScanEngine(Context ctx, Store store) {
        this.ctx = ctx;
        this.store = store;
        this.root = Shell.hasRoot();
        this.whitelist = store.whitelist();
    }

    public static void invalidate() {
        cache = null;
        Util.clearSizeCache();
    }

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
                {"syscache",  "系统缓存",  "dalvik / 字体 / 包管理器缓存（全盘模式）", "⚙", "1", "1"},
                {"rules",    "规则清理",   "按可编辑规则库扫描（日志/广告/残留等）", "📋", "0", "0"},
        };

        final List<JunkCategory> pending = new ArrayList<JunkCategory>();
        for (String[] d : defs) {
            if (!store.catEnabled(d[0])) continue;
            pending.add(new JunkCategory(d[0], d[1], d[2], d[3],
                    "1".equals(d[4]), "1".equals(d[5])));
        }

        // 并发扫描：各分类互不依赖，串行在全盘模式下慢到无法接受。
        // Semaphore 限制同时 4 个，避免大量并发 IO 反而互相拖慢。
        final int total = pending.size();
        final java.util.concurrent.atomic.AtomicInteger done =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(total);
        final java.util.concurrent.Semaphore gate =
                new java.util.concurrent.Semaphore(4);

        final java.util.List<Thread> tasks = new CopyOnWriteArrayList<Thread>();
        for (final JunkCategory c : pending) {
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        gate.acquire();
                        if (cb != null && cb.cancelled()) return;
                        if (!(c.needRoot && !root)) {
                            try { runCategory(c, cb); } catch (Throwable ignored) {}
                        }
                        int n = done.incrementAndGet();
                        if (cb != null) {
                            int items = 0;
                            long bytes = 0;
                            for (JunkCategory k : pending) {
                                synchronized (k.items) {
                                    items += k.items.size();
                                    for (JunkItem it : k.items) bytes += it.size;
                                }
                            }
                            cb.onCategory(c.name, n, total, items, bytes);
                        }
                    } catch (InterruptedException ignored) {
                        return;
                    } finally {
                        gate.release();
                        latch.countDown();
                    }
                }
            });
            tasks.add(t);
            t.start();
        }

        // 取消时 Thread.interrupt() 唤醒 acquire 与 latch.await
        Thread cancelWatcher = new Thread(new Runnable() {
            public void run() {
                while (latch.getCount() > 0) {
                    if (cb != null && cb.cancelled()) {
                        for (Thread t : tasks) t.interrupt();
                        return;
                    }
                    try { Thread.sleep(80); } catch (InterruptedException ignored) { return; }
                }
            }
        });
        cancelWatcher.start();
        // 分段轮询而非一次性长等待：取消后立刻返回已扫到的部分，
        // 原来要干等到 10 分钟超时才响应
        try {
            while (!latch.await(200, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                if (cb != null && cb.cancelled()) break;
            }
        } catch (InterruptedException ignored) {}
        cats.addAll(pending);

        if (cb == null || !cb.cancelled()) {
            cache = cats;
            cacheTime = System.currentTimeMillis();
        }
        return cats;
    }

    /** 统一白名单判定 */
    private boolean wl(String nameOrPath) {
        return Finder.inWhitelist(whitelist, nameOrPath);
    }

    private void runCategory(JunkCategory c, ScanEngine.Progress cb) {
        if (cb != null && cb.cancelled()) return;
        if ("cache".equals(c.id))          scanAppCache(c);
        else if ("webview".equals(c.id))   scanWebView(c);
        else if ("log".equals(c.id))       scanLogs(c);
        else if ("temp".equals(c.id))      scanTemp(c);
        else if ("thumb".equals(c.id))     scanThumbs(c);
        else if ("apkjunk".equals(c.id))   scanApkJunk(c);
        else if ("emptyjunk".equals(c.id)) scanEmpty(c);
        else if ("residue".equals(c.id))   scanResidue(c);
        else if ("syscache".equals(c.id))  scanSysCache(c);
        else if ("rules".equals(c.id))     scanRules(c);
    }

    private String scanRoot() {
        String r = store.scanRoot();
        return r == null || r.trim().isEmpty() ? Util.sdRoot() : r.trim();
    }

    // ---------- 分类实现 ----------

    private void scanAppCache(JunkCategory c) {
        if (root) {
            // 用单引号包裹路径避免 shell 解释，du 输出 "KB<TAB>path"
            List<String> out = Shell.exec(true,
                    "for d in /data/data/*/cache /data/data/*/code_cache; do " +
                    "test -d '$d' && du -sk '$d' 2>/dev/null; done");
            for (String l : out) {
                // du -sk 输出：第一列是 KB 数，之后是路径（中间有 tab 或空格）
                // 用 split 取第一个 token
                String[] parts = l.trim().split("\\s+", 2);
                if (parts.length < 2) continue;
                try {
                    long kb = Long.parseLong(parts[0].trim());
                    if (kb <= 4) continue;
                    String path = parts[1];
                    String pkg = pkgFromPath(path);
                    if (wl(pkg)) continue;
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
                if (wl(p.getName())) continue;
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
            if (wl(pkg)) continue;
            long s = root ? Shell.du(d) : Util.dirSize(new File(d));
            if (s > 65536) c.items.add(new JunkItem(d, pkg + " / webview", s));
        }
    }

    private void scanLogs(JunkCategory c) {
        List<String> paths = new ArrayList<String>(java.util.Arrays.asList(
                "/data/tombstones", "/data/anr", "/data/log",
                "/data/system/dropbox", "/cache/recovery", "/data/local/tmp"));
        if (store.fullScan()) {
            paths.addAll(java.util.Arrays.asList(
                    "/data/misc/logd", "/data/system/usagestats", "/data/vendor/tombstones",
                    "/data/misc/bootstat", "/cache/lost+found", "/data/dalvik-cache/profiles"));
        }
        for (String p : paths) {
            long s = Shell.du(p);
            if (s > 4096) c.items.add(new JunkItem(p, p, s));
        }
    }

    private void scanTemp(JunkCategory c) {
        String[] exts = {".tmp", ".temp", ".part", ".crdownload", ".download", ".log", ".bak", ".old"};
        for (String r : Finder.roots(scanRoot(), store.fullScan())) {
            walkExt(new File(r), 0, 7, c, exts);
        }
    }

    private void scanThumbs(JunkCategory c) {
        c.items.addAll(Finder.thumbs(whitelist));
    }

    /** 冗余安装包：sdcard 上的 apk 且对应包已安装 */
    private void scanApkJunk(JunkCategory c) {
        for (Finder.ApkInfo a : Finder.apks(ctx, scanRoot(), whitelist, store.fullScan())) {
            if (!a.installed) continue;
            if (wl(a.label) || wl(a.path)) continue;
            JunkItem it = new JunkItem(a.path, a.label + "（已安装）", a.size);
            c.items.add(it);
        }
    }

    private void scanEmpty(JunkCategory c) {
        c.items.addAll(Finder.empties(scanRoot(), true, 150, whitelist, store.fullScan()));
    }

    /** 规则库扫描（RuleEngine）*/
    private void scanRules(JunkCategory c) {
        java.util.List<RuleEngine.Rule> rules = RuleEngine.load(ctx);
        java.util.List<JunkItem> hits = RuleEngine.scan(scanRoot(), rules, whitelist);
        c.items.addAll(hits);
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
                if (installed.contains(pkg) || wl(pkg)) continue;
                if (!pkg.contains(".")) continue;
                long s = Util.dirSize(d);
                if (s > 0) c.items.add(new JunkItem(d.getAbsolutePath(),
                        pkg + "（已卸载）", s));
            }
        }
    }

    /** 系统级缓存，仅全盘模式启用 */
    private void scanSysCache(JunkCategory c) {
        if (!store.fullScan() || !root) return;
        String[] paths = {
                "/data/dalvik-cache/arm64", "/data/dalvik-cache/arm",
                "/data/system/package_cache", "/data/misc/installd",
                "/data/resource-cache", "/data/system/appops",
                "/data/misc/gatekeeper", "/data/font/files"
        };
        for (String p : paths) {
            if (wl(p)) continue;
            long s = Shell.du(p);
            if (s > 65536) c.items.add(new JunkItem(p, p, s));
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
            if (wl(n) || wl(f.getAbsolutePath())) continue;
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
