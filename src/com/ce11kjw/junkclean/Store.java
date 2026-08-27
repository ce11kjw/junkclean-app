package com.ce11kjw.junkclean;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 持久化：白名单 / 主题 / 统计 / 整理规则 / 设置开关 */
public class Store {
    private static final String PREF = "junkclean";
    private final SharedPreferences sp;

    public Store(Context c) {
        sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // ---------- 白名单 ----------
    public List<String> whitelist() {
        return split(sp.getString("whitelist", ""));
    }

    public boolean addWhitelist(String name) {
        LinkedHashSet<String> set = new LinkedHashSet<String>(whitelist());
        boolean added = set.add(name);
        sp.edit().putString("whitelist", join(set)).apply();
        return added;
    }

    public void setWhitelist(List<String> list) {
        sp.edit().putString("whitelist", join(new LinkedHashSet<String>(list))).apply();
    }

    // ---------- 主题 ----------
    public String theme() { return sp.getString("theme", "dark"); }
    public void setTheme(String t) { sp.edit().putString("theme", t).apply(); }
    public String accent() { return sp.getString("accent", "emerald"); }
    public void setAccent(String a) { sp.edit().putString("accent", a).apply(); }

    // ---------- AI ----------
    public String aiEndpoint() { return sp.getString("aiEndpoint", ""); }
    public String aiKey() { return sp.getString("aiKey", ""); }
    public String aiModel() { return sp.getString("aiModel", ""); }
    public void setAi(String endpoint, String key, String model) {
        sp.edit().putString("aiEndpoint", endpoint)
                 .putString("aiKey", key)
                 .putString("aiModel", model).apply();
    }
    public boolean aiReady() { return !aiEndpoint().trim().isEmpty(); }

    // ---------- 壁纸 ----------
    public String bgUrl() { return sp.getString("bgUrl", ""); }
    public void setBgUrl(String u) { sp.edit().putString("bgUrl", u).apply(); }

    // ---------- 设置开关 ----------
    public boolean toTrash() { return sp.getBoolean("toTrash", true); }
    public void setToTrash(boolean b) { sp.edit().putBoolean("toTrash", b).apply(); }

    public int trashDays() { return sp.getInt("trashDays", 7); }
    public void setTrashDays(int d) { sp.edit().putInt("trashDays", d).apply(); }

    public String scanRoot() { return sp.getString("scanRoot", ""); }
    public void setScanRoot(String s) { sp.edit().putString("scanRoot", s).apply(); }

    public boolean catEnabled(String id) { return sp.getBoolean("cat_" + id, true); }
    public void setCatEnabled(String id, boolean on) {
        sp.edit().putBoolean("cat_" + id, on).apply();
    }

    // ---------- 整理规则（src|dst|recursive|integrity 每行一条） ----------
    public List<String> rules() {
        String def = Util.sdRoot() + "/Download|" + Util.sdRoot() + "/JunkClean整理|1|1";
        return split(sp.getString("rules", def));
    }

    public void setRules(List<String> list) {
        sp.edit().putString("rules", joinList(list)).apply();
    }

    public String extMap() {
        return sp.getString("extMap",
                ".jpg,.jpeg,.png,.gif,.webp,.heic=图片\n"
              + ".mp4,.mkv,.mov,.avi,.webm=视频\n"
              + ".mp3,.flac,.wav,.m4a,.ogg=音频\n"
              + ".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt=文档\n"
              + ".zip,.7z,.rar,.tar,.gz=压缩包\n"
              + ".apk,.apks,.xapk=安装包");
    }

    public void setExtMap(String s) { sp.edit().putString("extMap", s).apply(); }

    // ---------- 统计 ----------
    public long totalFreed() { return sp.getLong("totalFreed", 0); }
    public int totalCount() { return sp.getInt("totalCount", 0); }
    public long lastClean() { return sp.getLong("lastClean", 0); }

    public void addStat(long freed, int count) {
        String today = Util.fmtDate(System.currentTimeMillis());
        sp.edit().putLong("totalFreed", totalFreed() + freed)
                 .putInt("totalCount", totalCount() + count)
                 .putLong("lastClean", System.currentTimeMillis())
                 .putLong("day_" + today, dayFreed(today) + freed)
                 .putInt("dayN_" + today, dayCount(today) + count)
                 .apply();
    }

    public long dayFreed(String date) { return sp.getLong("day_" + date, 0); }
    public int dayCount(String date) { return sp.getInt("dayN_" + date, 0); }

    /** 最近 7 天 [date, freed, count] */
    public List<Object[]> recent7() {
        List<Object[]> out = new ArrayList<Object[]>();
        long now = System.currentTimeMillis();
        for (int i = 6; i >= 0; i--) {
            String d = Util.fmtDate(now - i * 86400000L);
            out.add(new Object[]{d, dayFreed(d), dayCount(d)});
        }
        return out;
    }

    public void resetStats() {
        SharedPreferences.Editor e = sp.edit();
        e.remove("totalFreed").remove("totalCount").remove("lastClean");
        long now = System.currentTimeMillis();
        for (int i = 0; i < 40; i++) {
            String d = Util.fmtDate(now - i * 86400000L);
            e.remove("day_" + d).remove("dayN_" + d);
        }
        e.apply();
    }

    // ---------- 工具 ----------
    private List<String> split(String s) {
        List<String> out = new ArrayList<String>();
        if (s == null || s.isEmpty()) return out;
        for (String x : s.split("\n")) if (!x.trim().isEmpty()) out.add(x.trim());
        return out;
    }

    private String join(LinkedHashSet<String> set) {
        StringBuilder b = new StringBuilder();
        for (String s : set) { if (b.length() > 0) b.append('\n'); b.append(s); }
        return b.toString();
    }

    private String joinList(List<String> list) {
        StringBuilder b = new StringBuilder();
        for (String s : list) { if (b.length() > 0) b.append('\n'); b.append(s); }
        return b.toString();
    }
}
