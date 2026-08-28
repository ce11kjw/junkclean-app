package com.ce11kjw.junkclean;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增量扫描缓存：跨会话复用文件哈希，二次扫描只重算变化文件。
 *
 * 思路（参考 CachedDupeScanner）：
 *   1. 每条记录 path|size|mtime|hash
 *   2. 二次扫描：path+size+mtime 都没变 → 直接用缓存的 hash（不重读文件）
 *   3. 任何一项变了 → 标记 STALE，重新计算
 *
 * 存储：app 私有目录 hash_cache.tsv（TSV 分隔，逐行）。不用 SQLite，
 * 保持零依赖；全盘文件几千行，纯文本足够。
 */
public final class HashCache {

    private static final String FILE = "hash_cache.tsv";
    private static final int MAX_ENTRIES = 20000;

    private final File file;
    private Map<String, Entry> cache;

    public static class Entry {
        public long size;
        public long mtime;
        public String hash;   // 空串 = 未哈希（之前大小没冲突）
    }

    public HashCache(Context c) {
        file = new File(c.getFilesDir(), FILE);
        load();
    }

    /** 命中缓存且 size/mtime 未变 → 返回缓存值；否则返回 null（需重算） */
    public String get(String path, long size, long mtime) {
        Entry e = cache.get(path);
        if (e == null) return null;
        if (e.size != size || e.mtime != mtime) {
            cache.remove(path);   // stale，移除让后续重算
            return null;
        }
        return e.hash == null ? "" : e.hash;   // "" = 已知无需哈希
    }

    /** 记录：mark=false 表示「已知无冲突不需要哈希」 */
    public void put(String path, long size, long mtime, String hash, boolean mark) {
        Entry e = new Entry();
        e.size = size;
        e.mtime = mtime;
        e.hash = mark ? hash : "";
        if (cache.size() >= MAX_ENTRIES && !cache.containsKey(path)) {
            save();          // 防膨胀：先落盘再继续
            cache.clear();
        }
        cache.put(path, e);
    }

    public int size() { return cache.size(); }

    public void clear() {
        cache.clear();
        save();
    }

    // ---------- 持久化 ----------

    private void load() {
        cache = new HashMap<String, Entry>();
        if (file == null || !file.exists()) return;
        List<String> lines = Util.readLines(file);
        for (String l : lines) {
            String[] p = l.split("\t", 4);
            if (p.length < 4) continue;
            try {
                Entry e = new Entry();
                e.size = Long.parseLong(p[1]);
                e.mtime = Long.parseLong(p[2]);
                e.hash = "1".equals(p[3]) ? null : "";   // 1=有哈希, 0=无冲突
                if (e.hash != null) e.hash = p.length > 4 ? p[4] : "";
                cache.put(p[0], e);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void save() {
        if (file == null) return;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Entry> kv : cache.entrySet()) {
            Entry e = kv.getValue();
            String hasHash = e.hash != null ? "1" : "0";
            String h = e.hash == null ? "" : e.hash;
            sb.append(kv.getKey()).append('\t')
              .append(e.size).append('\t')
              .append(e.mtime).append('\t')
              .append(hasHash).append('\t')
              .append(h).append('\n');
        }
        Util.write(file, sb.toString());
    }

    /** 清理失效路径（文件已不存在） */
    public void prune(List<String> livePaths) {
        boolean changed = false;
        for (String k : new java.util.ArrayList<String>(cache.keySet())) {
            if (!livePaths.contains(k)) {
                cache.remove(k);
                changed = true;
            }
        }
        if (changed) save();
    }
}
