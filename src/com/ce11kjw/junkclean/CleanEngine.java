package com.ce11kjw.junkclean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 清理引擎：路径安全校验 + 回收站可选 + root 批量 rm 降级 */
public class CleanEngine {

    public static class Result {
        public long freed;        // 真正释放的字节（删除）
        public long trashed;      // 移入回收站的字节（空间未释放）
        public int count;
        public int toTrash;
        public List<String> errors = new ArrayList<String>();
        /** catId → [释放字节, 项数] */
        public java.util.Map<String, long[]> catFreed = new java.util.HashMap<String, long[]>();

        /** 用于统计展示：入回收站的不计入释放量 */
        public long realFreed() { return freed; }
    }

    private final boolean root;
    private final boolean useTrash;
    private final List<Object[]> pendingRoot = new ArrayList<Object[]>();

    public CleanEngine(boolean useTrash) {
        this.root = Shell.hasRoot();
        this.useTrash = useTrash;
    }

    /** 清理已勾选的分类项。同时按分类记账，供统计页展示占比 */
    public Result clean(List<JunkCategory> cats) {
        Result r = new Result();
        StringBuilder batch = new StringBuilder();
        for (JunkCategory c : cats) {
            long before = r.freed + r.trashed;
            int cntBefore = r.count;
            for (JunkItem it : c.items) {
                if (!it.checked) continue;
                step(it.path, it.size, r, batch, c.needRoot);
            }
            long delta = (r.freed + r.trashed) - before;
            int cnt = r.count - cntBefore;
            if (cnt > 0) r.catFreed.put(c.id, new long[]{delta, cnt});
        }
        flush(batch, r);
        return r;
    }

    /** 清理任意路径列表 */
    public Result cleanItems(List<JunkItem> items) {
        Result r = new Result();
        StringBuilder batch = new StringBuilder();
        for (JunkItem it : items) {
            if (!it.checked) continue;
            step(it.path, it.size, r, batch, false);
        }
        flush(batch, r);
        return r;
    }

    private void step(String path, long size, Result r, StringBuilder batch, boolean systemPath) {
        if (!isSafe(path)) {
            r.errors.add("受保护路径：" + Util.shortPath(path));
            return;
        }
        File f = new File(path);
        if (!f.exists()) {
            // 文件已不在（可能上一轮已删），算作成功，不报错
            r.count++;
            return;
        }

        // sdcard 上的用户文件走回收站；系统缓存目录直接删（回收站装不下也没意义）
        boolean sdFile = path.startsWith(Util.sdRoot() + "/");
        if (useTrash && sdFile && !systemPath && !path.contains("/.junkclean_trash/")) {
            long moved = Trash.moveIn(f);
            if (moved > 0) {
                r.trashed += moved;
                r.count++;
                r.toTrash++;
                return;
            }
            // 回收站失败（跨分区、空间不足等）时继续走直删，不静默丢弃
        }

        if (Util.rmrf(f)) {
            r.freed += size;
            r.count++;
            return;
        }

        if (root) {
            // 攒批量 root 删除，flush 后统一校验，避免乐观计数
            batch.append("rm -rf ").append(Shell.quote(path)).append('\n');
            pendingRoot.add(new Object[]{path, Long.valueOf(size)});
        } else {
            r.errors.add(reason(f) + "：" + Util.shortPath(path));
        }
    }

    /** 推断删除失败原因，便于用户判断该怎么办 */
    private String reason(File f) {
        if (!f.canWrite()) {
            String p = f.getAbsolutePath();
            if (p.startsWith(Util.sdRoot() + "/Android/data")
                    || p.startsWith(Util.sdRoot() + "/Android/obb")) {
                return "Android/data 受限（需 root 或 SAF 授权）";
            }
            return "无写入权限";
        }
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null && kids.length > 0) return "目录未能清空";
            return "目录删除被拒绝";
        }
        return "删除被拒绝（可能被其他应用占用）";
    }

    /** 执行 root 批量删除并校验结果，只有真正消失的才计入释放量 */
    private void flush(StringBuilder batch, Result r) {
        if (batch.length() == 0 || !root) {
            for (Object[] p : pendingRoot) r.errors.add(Util.shortPath((String) p[0]));
            pendingRoot.clear();
            return;
        }
        Shell.exec(true, batch.toString());
        for (Object[] p : pendingRoot) {
            String path = (String) p[0];
            long size = ((Long) p[1]).longValue();
            if (new File(path).exists()) {
                r.errors.add(Util.shortPath(path));
            } else {
                r.freed += size;
                r.count++;
            }
        }
        pendingRoot.clear();
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
