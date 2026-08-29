package com.ce11kjw.junkclean;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 文件浏览：列目录、算体积、手动删除任意文件或文件夹 */
public final class Browser {

    public static class Entry {
        public String name;
        public String path;
        public boolean dir;
        public long size;        // 目录为递归体积
        public int children;     // 目录下条目数
        public long mtime;
        public boolean checked;
        public boolean protectedPath;
    }

    private Browser() {}

    /** 列出目录内容，目录在前、按体积倒序 */
    public static List<Entry> list(String path, boolean withDirSize) {
        List<Entry> out = new ArrayList<Entry>();
        File dir = new File(path);
        File[] fs = dir.listFiles();
        if (fs == null) return out;

        for (File f : fs) {
            Entry e = new Entry();
            e.name = f.getName();
            e.path = f.getAbsolutePath();
            e.dir = f.isDirectory();
            e.mtime = f.lastModified();
            e.protectedPath = isProtected(e.name, e.path);
            if (e.dir) {
                File[] kids = f.listFiles();
                e.children = kids == null ? 0 : kids.length;
                e.size = withDirSize ? Util.dirSize(f) : 0;
            } else {
                e.size = f.length();
            }
            out.add(e);
        }

        Collections.sort(out, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                if (a.dir != b.dir) return a.dir ? -1 : 1;
                return Long.compare(b.size, a.size);
            }
        });
        return out;
    }

    /** 命中保护路径的条目，UI 上标记并阻止删除 */
    public static boolean isProtected(String name, String path) {
        return Store.isProtected(name) || Store.isProtected(path);
    }

    /** 上一级目录，已在根则返回 null */
    public static String parent(String path) {
        if (path == null) return null;
        File f = new File(path);
        File p = f.getParentFile();
        if (p == null) return null;
        // 不允许退到 sdcard 之外，避免误入系统根目录
        String sd = Util.sdRoot();
        if (!path.startsWith(sd)) return sd;
        if (path.equals(sd)) return null;
        return p.getAbsolutePath();
    }

    /** 快捷入口 */
    public static String[][] shortcuts() {
        String sd = Util.sdRoot();
        return new String[][]{
                {"存储根目录", sd},
                {"下载", sd + "/Download"},
                {"相册", sd + "/DCIM"},
                {"图片", sd + "/Pictures"},
                {"影片", sd + "/Movies"},
                {"音乐", sd + "/Music"},
                {"文档", sd + "/Documents"},
                {"应用数据", sd + "/Android/data"},
                {"游戏数据包", sd + "/Android/obb"}
        };
    }
}
