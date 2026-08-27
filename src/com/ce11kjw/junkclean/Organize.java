package com.ce11kjw.junkclean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 整理中心：按扩展名分类归档 + 干跑预览 + 历史还原 */
public class Organize {

    /** 一条整理规则 */
    public static class Rule {
        public String src;
        public String dst;
        public boolean recursive = true;
        public boolean integrity = true;   // 跳过未完成下载

        public Rule(String src, String dst, boolean recursive, boolean integrity) {
            this.src = src; this.dst = dst;
            this.recursive = recursive; this.integrity = integrity;
        }

        public String serialize() {
            return src + "|" + dst + "|" + (recursive ? 1 : 0) + "|" + (integrity ? 1 : 0);
        }

        public static Rule parse(String line) {
            String[] p = line.split("\\|");
            if (p.length < 2) return null;
            return new Rule(p[0], p[1],
                    p.length < 3 || "1".equals(p[2]),
                    p.length < 4 || "1".equals(p[3]));
        }
    }

    /** 一次移动 */
    public static class Move {
        public String from, to;
        public long size;
        public Move(String f, String t, long s) { from = f; to = t; size = s; }
    }

    public static class Result {
        public List<Move> moves = new ArrayList<Move>();
        public long total;
        public int skipped;
    }

    private static final String[] INCOMPLETE = {
            ".part", ".tmp", ".temp", ".crdownload", ".download", ".!qb", ".td", ".aria2"};

    private final List<String[]> extMap = new ArrayList<String[]>();   // {ext, 分类名}
    private final List<String> whitelist;

    /** extMapText 格式：.jpg,.png=图片 每行一条 */
    public Organize(String extMapText, List<String> whitelist) {
        this.whitelist = whitelist == null ? new ArrayList<String>() : whitelist;
        for (String line : extMapText.split("\n")) {
            int eq = line.lastIndexOf('=');
            if (eq <= 0) continue;
            String cat = line.substring(eq + 1).trim();
            for (String e : line.substring(0, eq).split(",")) {
                e = e.trim().toLowerCase(java.util.Locale.US);
                if (!e.isEmpty()) extMap.add(new String[]{e, cat});
            }
        }
    }

    /** 干跑：只算出移动清单，不落盘 */
    public Result preview(Rule r) {
        Result res = new Result();
        if (r.src == null || r.dst == null || r.src.isEmpty() || r.dst.isEmpty()) return res;
        // 目标不能是源本身，也不能是源的父级（否则会把文件搬到自己上层反复移动）
        if (r.src.equals(r.dst) || r.src.startsWith(r.dst + "/")) return res;
        File src = new File(r.src);
        if (!src.isDirectory()) return res;
        collect(src, r, res, 0);
        return res;
    }

    /** 执行整理，写入历史，返回结果 */
    public Result run(Rule r) {
        Result res = preview(r);
        List<Move> done = new ArrayList<Move>();
        StringBuilder hist = new StringBuilder();
        for (Move m : res.moves) {
            File from = new File(m.from);
            File to = Util.uniqueName(new File(m.to));
            if (Util.move(from, to)) {
                m.to = to.getAbsolutePath();
                done.add(m);
                hist.append(m.to).append('\t').append(m.from).append('\t')
                    .append(System.currentTimeMillis() / 1000).append('\n');
            }
        }
        if (hist.length() > 0) Util.append(historyFile(), hist.toString());
        res.moves = done;
        long t = 0;
        for (Move m : done) t += m.size;
        res.total = t;
        return res;
    }

    /** 历史记录（新到旧），每项 [现路径, 原路径, 时间] */
    public static List<String[]> history(int limit) {
        List<String> lines = Util.readLines(historyFile());
        List<String[]> out = new ArrayList<String[]>();
        for (int i = lines.size() - 1; i >= 0 && out.size() < limit; i--) {
            String[] p = lines.get(i).split("\t");
            if (p.length >= 3) out.add(p);
        }
        return out;
    }

    /** 撤销一条整理（移回原位） */
    public static boolean undo(String[] entry) {
        File cur = new File(entry[0]);
        if (!cur.exists()) { removeHistory(entry[0]); return false; }
        File back = Util.uniqueName(new File(entry[1]));
        boolean ok = Util.move(cur, back);
        if (ok) removeHistory(entry[0]);
        return ok;
    }

    /** 撤销全部历史 */
    public static int undoAll() {
        int n = 0;
        for (String[] e : history(9999)) if (undo(e)) n++;
        return n;
    }

    public static void clearHistory() {
        Util.write(historyFile(), "");
    }

    // ---------- 内部 ----------

    private void collect(File dir, Rule r, Result res, int depth) {
        if (depth > (r.recursive ? 8 : 0)) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            String name = f.getName();
            if (name.startsWith(".")) continue;
            if (f.isDirectory()) {
                // 不进入目标目录，避免自我搬运
                String ap = f.getAbsolutePath();
                if (ap.equals(r.dst) || ap.startsWith(r.dst + "/")) continue;
                if (r.recursive) collect(f, r, res, depth + 1);
                continue;
            }
            if (whitelist.contains(name)) continue;
            if (r.integrity && incomplete(name)) { res.skipped++; continue; }
            String cat = categoryOf(name);
            if (cat == null) continue;
            File to = new File(new File(r.dst, cat), name);
            if (to.getAbsolutePath().equals(f.getAbsolutePath())) continue;
            res.moves.add(new Move(f.getAbsolutePath(), to.getAbsolutePath(), f.length()));
            res.total += f.length();
        }
    }

    private boolean incomplete(String name) {
        String low = name.toLowerCase(java.util.Locale.US);
        for (String s : INCOMPLETE) if (low.endsWith(s)) return true;
        return false;
    }

    private String categoryOf(String name) {
        String e = Util.ext(name);
        if (e.isEmpty()) return null;
        for (String[] kv : extMap) if (kv[0].equals(e)) return kv[1];
        return null;
    }

    private static File historyFile() {
        return new File(Util.sdRoot() + "/.junkclean_trash", "organize.tsv");
    }

    private static void removeHistory(String curPath) {
        List<String> lines = Util.readLines(historyFile());
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            if (l.startsWith(curPath + "\t")) continue;
            sb.append(l).append('\n');
        }
        Util.write(historyFile(), sb.toString());
    }
}
