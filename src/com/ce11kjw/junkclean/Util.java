package com.ce11kjw.junkclean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class Util {
    private Util() {}

    public static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        double v = b;
        String[] u = {"KB", "MB", "GB", "TB"};
        int i = -1;
        while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
        return String.format(Locale.US, "%.1f %s", v, u[i]);
    }

    public static String fmtTime(long ms) {
        if (ms <= 0) return "从未";
        return new SimpleDateFormat("MM-dd HH:mm", Locale.US).format(new Date(ms));
    }

    public static String fmtDate(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ms));
    }

    /**
     * 目录体积缓存。同一目录在一次会话里可能被多处反复统计
     * （分类扫描、排行、浏览各算一遍），缓存能省掉大量重复递归。
     */
    private static final java.util.Map<String, long[]> sizeCache =
            java.util.Collections.synchronizedMap(new java.util.HashMap<String, long[]>());
    private static final long CACHE_TTL = 30000;

    public static void clearSizeCache() {
        sizeCache.clear();
    }

    /** 递归目录体积（不跟随符号链接，限制深度防止栈溢出） */
    private static final int CACHE_MAX = 512;

    public static long dirSize(File d) {
        if (d == null) return 0;
        if (d.isFile()) return d.length();
        String key = d.getAbsolutePath();
        long now = System.currentTimeMillis();
        long[] hit = sizeCache.get(key);
                if (hit != null && now - hit[1] < CACHE_TTL) return hit[0];
        // 全盘扫描会遍历几千个目录，无上限缓存会一直占内存
        if (sizeCache.size() >= CACHE_MAX) sizeCache.clear();
        long v = dirSize(d, 0);
        sizeCache.put(key, new long[]{v, now});
        return v;
    }

    private static final int MAX_DEPTH = 24;

    private static long dirSize(File d, int depth) {
        if (d == null || depth > MAX_DEPTH || !d.exists()) return 0;
        if (d.isFile()) return d.length();
        long s = 0;
        File[] fs = d.listFiles();
        if (fs == null) return 0;
        for (File f : fs) {
            try {
                // canonicalPath 不等于 absolutePath 说明是符号链接，跳过避免成环
                if (!f.getCanonicalPath().equals(f.getAbsolutePath())) continue;
            } catch (Exception e) { continue; }
            s += f.isDirectory() ? dirSize(f, depth + 1) : f.length();
        }
        return s;
    }

    public static boolean rmrf(File f) {
        return rmrf(f, 0);
    }

    private static boolean rmrf(File f, int depth) {
        if (f == null || !f.exists()) return true;
        if (depth > MAX_DEPTH) return false;
        if (f.isDirectory()) {
            // 符号链接目录直接删链接本身，不进入
            boolean symlink = false;
            try {
                symlink = !f.getCanonicalPath().equals(f.getAbsolutePath());
            } catch (Exception ignored) {}
            if (!symlink) {
                File[] fs = f.listFiles();
                if (fs != null) for (File c : fs) rmrf(c, depth + 1);
            }
        }
        return f.delete();
    }

    public static String sdRoot() {
        for (String p : new String[]{"/storage/emulated/0", "/sdcard", "/mnt/sdcard"}) {
            if (new File(p).isDirectory()) return p;
        }
        return "/sdcard";
    }

    public static String shortPath(String p) {
        String sd = sdRoot();
        return p != null && p.startsWith(sd) ? "…" + p.substring(sd.length()) : p;
    }

    public static String ext(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i).toLowerCase(Locale.US);
    }

    /** 文件 MD5（大文件只取首尾 512KB + 大小，够用且快） */
    public static String quickHash(File f) {
        FileInputStream in = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(String.valueOf(f.length()).getBytes());
            in = new FileInputStream(f);
            byte[] buf = new byte[524288];
            int n = in.read(buf);
            if (n > 0) md.update(buf, 0, n);
            if (f.length() > 1048576L) {
                // InputStream.skip 不保证一次跳够，需循环
                long need = f.length() - 524288L - n;
                while (need > 0) {
                    long skipped = in.skip(need);
                    if (skipped <= 0) break;
                    need -= skipped;
                }
                n = in.read(buf);
                if (n > 0) md.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return f.getName() + ":" + f.length();
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    // ---------- 文本读写 ----------

    public static List<String> readLines(File f) {
        List<String> out = new ArrayList<String>();
        if (f == null || !f.exists()) return out;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(f));
            String l;
            while ((l = r.readLine()) != null) if (!l.trim().isEmpty()) out.add(l);
        } catch (Exception ignored) {
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) {}
        }
        return out;
    }

    public static void write(File f, String s) {
        FileWriter w = null;
        try {
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            w = new FileWriter(f, false);
            w.write(s);
        } catch (Exception ignored) {
        } finally {
            try { if (w != null) w.close(); } catch (Exception ignored) {}
        }
    }

    public static void append(File f, String s) {
        FileWriter w = null;
        try {
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            w = new FileWriter(f, true);
            w.write(s);
        } catch (Exception ignored) {
        } finally {
            try { if (w != null) w.close(); } catch (Exception ignored) {}
        }
    }

    /** 移动文件（同分区 rename，跨分区复制） */
    public static boolean move(File src, File dst) {
        if (dst.getParentFile() != null) dst.getParentFile().mkdirs();
        if (src.renameTo(dst)) return true;
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.close(); out = null;
            in.close(); in = null;
            return src.delete();
        } catch (Exception e) {
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    /** 重名时追加序号 */
    public static File uniqueName(File dst) {
        if (!dst.exists()) return dst;
        String name = dst.getName();
        String base = name, e = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) { base = name.substring(0, dot); e = name.substring(dot); }
        for (int i = 1; i < 1000; i++) {
            File f = new File(dst.getParentFile(), base + "_" + i + e);
            if (!f.exists()) return f;
        }
        return dst;
    }
}
