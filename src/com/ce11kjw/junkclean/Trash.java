package com.ce11kjw.junkclean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 回收站：移入 / 恢复 / 彻底删 / 自动过期清理 */
public class Trash {

    public static class Item {
        public String path;     // 回收站内路径
        public String orig;     // 原始路径
        public long size;
        public long time;       // 移入时间戳（秒）
        public int left;        // 剩余保留天数，-1 = 不自动删
    }

    private static final String DIR_NAME = ".junkclean_trash";
    private static final String META = "meta.tsv";

    public static File dir() {
        File d = new File(Util.sdRoot(), DIR_NAME);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static File metaFile() {
        return new File(dir(), META);
    }

    /** 移入回收站，返回释放字节 */
    public static long moveIn(File src) {
        if (src == null || !src.exists()) return 0;
        long size = Util.dirSize(src);
        String stamp = System.currentTimeMillis() + "-" + src.getName();
        File dst = new File(dir(), stamp);
        boolean ok = src.renameTo(dst);
        if (!ok) {
            // 跨分区，退化为复制 + 删除
            ok = copy(src, dst) && Util.rmrf(src);
        }
        if (!ok) return 0;
        appendMeta(dst.getAbsolutePath(), src.getAbsolutePath(), size);
        return size;
    }

    /** 列出回收站条目（按时间倒序），带剩余天数 */
    public static List<Item> list(int autoDays) {
        List<Item> out = new ArrayList<Item>();
        List<String> lines = Util.readLines(metaFile());
        long now = System.currentTimeMillis() / 1000;
        boolean stale = false;
        for (String l : lines) {
            String[] p = l.split("\t");
            if (p.length < 4) continue;
            Item it = new Item();
            it.path = p[0];
            it.orig = p[1];
            try { it.size = Long.parseLong(p[2]); } catch (Exception e) { it.size = 0; }
            try { it.time = Long.parseLong(p[3]); } catch (Exception e) { it.time = now; }
            if (!new File(it.path).exists()) { stale = true; continue; }
            it.left = autoDays > 0
                    ? Math.max(0, autoDays - (int) ((now - it.time) / 86400))
                    : -1;
            out.add(it);
        }
        Collections.sort(out, new Comparator<Item>() {
            public int compare(Item a, Item b) { return Long.compare(b.time, a.time); }
        });
        if (stale) rewriteMeta(out);   // 清掉指向已不存在文件的记录
        return out;
    }

    /** 用当前有效条目重写 meta，避免记录无限增长 */
    private static void rewriteMeta(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item it : items) {
            sb.append(it.path).append('\t').append(it.orig).append('\t')
              .append(it.size).append('\t').append(it.time).append('\n');
        }
        Util.write(metaFile(), sb.toString());
    }

    /** 恢复到原路径 */
    public static boolean restore(Item it) {
        File src = new File(it.path);
        File dst = new File(it.orig);
        if (dst.getParentFile() != null) dst.getParentFile().mkdirs();
        if (dst.exists()) dst = new File(it.orig + ".restored");
        boolean ok = src.renameTo(dst);
        if (!ok) ok = copy(src, dst) && Util.rmrf(src);
        if (ok) removeMeta(it.path);
        return ok;
    }

    /** 彻底删除 */
    public static long delete(Item it) {
        long s = it.size;
        boolean ok = Util.rmrf(new File(it.path));
        if (!ok && Shell.hasRoot()) {
            Shell.exec(true, "rm -rf " + Shell.quote(it.path));
            ok = !new File(it.path).exists();
        }
        if (ok) { removeMeta(it.path); return s; }
        return 0;
    }

    /** 清空 */
    public static long empty() {
        long freed = 0;
        for (Item it : list(-1)) freed += delete(it);
        Util.write(metaFile(), "");
        return freed;
    }

    /** 自动清理过期项，返回释放字节 */
    public static long autoClean(int days) {
        if (days <= 0) return 0;
        long freed = 0;
        for (Item it : list(days)) {
            if (it.left == 0) freed += delete(it);
        }
        return freed;
    }

    public static long totalSize(List<Item> items) {
        long t = 0;
        for (Item it : items) t += it.size;
        return t;
    }

    // ---------- 内部 ----------

    private static void appendMeta(String path, String orig, long size) {
        String line = path + "\t" + orig + "\t" + size + "\t"
                + (System.currentTimeMillis() / 1000) + "\n";
        Util.append(metaFile(), line);
    }

    private static void removeMeta(String path) {
        List<String> lines = Util.readLines(metaFile());
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            if (l.startsWith(path + "\t")) continue;
            sb.append(l).append('\n');
        }
        Util.write(metaFile(), sb.toString());
    }

    private static boolean copy(File src, File dst) {
        if (src.isDirectory()) {
            if (!dst.mkdirs() && !dst.isDirectory()) return false;
            File[] fs = src.listFiles();
            if (fs != null) for (File f : fs) {
                if (!copy(f, new File(dst, f.getName()))) return false;
            }
            return true;
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }
}
