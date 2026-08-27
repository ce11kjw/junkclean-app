package com.ce11kjw.junkclean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 清理引擎：只删扫描结果中已勾选的项，root 优先，失败降级 */
public class CleanEngine {

    public static class Result {
        public long freed;
        public int count;
        public List<String> errors = new ArrayList<String>();
    }

    private final boolean root;

    public CleanEngine() {
        this.root = Shell.hasRoot();
    }

    public Result clean(List<JunkCategory> cats) {
        Result r = new Result();
        StringBuilder rootBatch = new StringBuilder();

        for (JunkCategory c : cats) {
            for (JunkItem it : c.items) {
                if (!it.checked) continue;
                File f = new File(it.path);
                if (!isSafe(it.path)) {
                    r.errors.add("拒绝: " + it.path);
                    continue;
                }
                boolean ok;
                if (f.canWrite() || !root) {
                    ok = Util.rmrf(f);
                } else {
                    // 攒批量 root 命令，减少 su 调用
                    rootBatch.append("rm -rf '").append(it.path.replace("'", "")).append("'\n");
                    ok = true;
                }
                if (ok) {
                    r.freed += it.size;
                    r.count++;
                } else {
                    r.errors.add(it.path);
                }
            }
        }

        if (rootBatch.length() > 0 && root) {
            Shell.exec(true, rootBatch.toString());
        }
        return r;
    }

    /** 路径安全检查：必须在允许区域内，不含 .. */
    private boolean isSafe(String p) {
        if (p == null || p.contains("..")) return false;
        String sd = Util.sdRoot();
        return p.startsWith(sd + "/")
                || p.startsWith("/data/data/")
                || p.startsWith("/data/user/")
                || p.startsWith("/data/tombstones")
                || p.startsWith("/data/anr")
                || p.startsWith("/data/log")
                || p.startsWith("/data/system/dropbox")
                || p.startsWith("/cache/");
    }

    /** 通过 PM 清理指定包缓存（需 root） */
    public long cleanAppCache(String pkg) {
        if (!root) return 0;
        long before = 0, after = 0;
        String base = "/data/data/" + pkg;
        before = duKb(base + "/cache") + duKb(base + "/code_cache");
        Shell.exec(true, "rm -rf " + base + "/cache/* " + base + "/code_cache/* 2>/dev/null");
        after = duKb(base + "/cache") + duKb(base + "/code_cache");
        return Math.max(0, (before - after)) * 1024;
    }

    private long duKb(String path) {
        String s = Shell.one(root, "du -sk " + path + " 2>/dev/null | cut -f1");
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }
}
