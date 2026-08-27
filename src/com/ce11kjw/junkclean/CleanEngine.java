package com.ce11kjw.junkclean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 清理引擎：路径安全校验 + 回收站可选 + root 批量 rm 降级 */
public class CleanEngine {

    public static class Result {
        public long freed;
        public int count;
        public int toTrash;
        public List<String> errors = new ArrayList<String>();
    }

    private final boolean root;
    private final boolean useTrash;

    public CleanEngine(boolean useTrash) {
        this.root = Shell.hasRoot();
        this.useTrash = useTrash;
    }

    /** 清理已勾选的分类项 */
    public Result clean(List<JunkCategory> cats) {
        Result r = new Result();
        StringBuilder batch = new StringBuilder();
        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) {
                if (!it.checked) continue;
                step(it.path, it.size, r, batch, c.needRoot);
            }
        }
        flush(batch);
        return r;
    }

    /** 清理任意路径列表 */
    public Result clean(List<JunkItem> items, boolean allowTrash) {
        Result r = new Result();
        StringBuilder batch = new StringBuilder();
        for (JunkItem it : items) {
            if (!it.checked) continue;
            step(it.path, it.size, r, batch, false);
        }
        flush(batch);
        return r;
    }

    private void step(String path, long size, Result r, StringBuilder batch, boolean systemPath) {
        if (!isSafe(path)) {
            r.errors.add("拒绝: " + Util.shortPath(path));
            return;
        }
        File f = new File(path);
        if (!f.exists()) return;

        // sdcard 上的用户文件走回收站；系统缓存目录直接删（回收站装不下也没意义）
        boolean sdFile = path.startsWith(Util.sdRoot() + "/");
        if (useTrash && sdFile && !systemPath && !path.contains("/.junkclean_trash/")) {
            long moved = Trash.moveIn(f);
            if (moved > 0) {
                r.freed += moved;
                r.count++;
                r.toTrash++;
                return;
            }
        }

        if (Util.rmrf(f)) {
            r.freed += size;
            r.count++;
        } else if (root) {
            batch.append("rm -rf ").append(Shell.quote(path)).append('\n');
            r.freed += size;
            r.count++;
        } else {
            r.errors.add(Util.shortPath(path));
        }
    }

    private void flush(StringBuilder batch) {
        if (batch.length() > 0 && root) Shell.exec(true, batch.toString());
    }

    /** 路径白名单：只允许 sdcard 与已知系统缓存目录，拒绝 .. */
    public static boolean isSafe(String p) {
        if (p == null || p.isEmpty() || p.contains("..")) return false;
        String sd = Util.sdRoot();
        if (p.equals(sd) || p.equals("/") || p.equals("/data") || p.equals("/data/data")) return false;
        return p.startsWith(sd + "/")
                || p.startsWith("/data/data/")
                || p.startsWith("/data/user/")
                || p.startsWith("/data/tombstones")
                || p.startsWith("/data/anr")
                || p.startsWith("/data/log")
                || p.startsWith("/data/system/dropbox")
                || p.startsWith("/data/local/tmp/")
                || p.startsWith("/cache/");
    }

    /** 清单个应用缓存，返回释放字节 */
    public long cleanAppCache(String pkg) {
        long freed = 0;
        if (root) {
            String base = "/data/data/" + pkg;
            long before = Shell.du(base + "/cache") + Shell.du(base + "/code_cache");
            Shell.exec(true, "rm -rf " + base + "/cache/* " + base + "/code_cache/* 2>/dev/null");
            long after = Shell.du(base + "/cache") + Shell.du(base + "/code_cache");
            freed += Math.max(0, before - after);
        }
        File ext = new File(Util.sdRoot() + "/Android/data/" + pkg + "/cache");
        long s = Util.dirSize(ext);
        if (s > 0) {
            File[] kids = ext.listFiles();
            boolean ok = true;
            if (kids != null) for (File k : kids) ok &= Util.rmrf(k);
            if (ok) freed += s;
        }
        return freed;
    }
}
