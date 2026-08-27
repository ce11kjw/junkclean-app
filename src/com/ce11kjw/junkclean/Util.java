package com.ce11kjw.junkclean;

import java.io.File;

public final class Util {
    private Util() {}

    /** 人类可读体积 */
    public static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        double v = b;
        String[] u = {"KB", "MB", "GB", "TB"};
        int i = -1;
        while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
        return String.format(java.util.Locale.US, "%.1f %s", v, u[i]);
    }

    /** 递归目录体积（不跟随符号链接） */
    public static long dirSize(File d) {
        if (d == null || !d.exists()) return 0;
        if (d.isFile()) return d.length();
        long s = 0;
        File[] fs = d.listFiles();
        if (fs == null) return 0;
        for (File f : fs) {
            try {
                if (f.getCanonicalPath().equals(f.getAbsolutePath())) {
                    s += f.isDirectory() ? dirSize(f) : f.length();
                }
            } catch (Exception ignored) {}
        }
        return s;
    }

    /** 递归删除 */
    public static boolean rmrf(File f) {
        if (f == null || !f.exists()) return true;
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            if (fs != null) for (File c : fs) rmrf(c);
        }
        return f.delete();
    }

    /** sdcard 根目录 */
    public static String sdRoot() {
        for (String p : new String[]{"/storage/emulated/0", "/sdcard", "/mnt/sdcard"}) {
            if (new File(p).isDirectory()) return p;
        }
        return "/sdcard";
    }
}
