package com.ce11kjw.junkclean;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/** 轻量持久化：白名单 / 主题 / 统计 */
public class Store {
    private static final String PREF = "junkclean";
    private final SharedPreferences sp;

    public Store(Context c) {
        sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // 白名单
    public List<String> whitelist() {
        String s = sp.getString("whitelist", "");
        List<String> out = new ArrayList<String>();
        if (s.isEmpty()) return out;
        for (String x : s.split("\n")) if (!x.trim().isEmpty()) out.add(x.trim());
        return out;
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

    // 主题
    public String theme() { return sp.getString("theme", "dark"); }
    public void setTheme(String t) { sp.edit().putString("theme", t).apply(); }
    public String accent() { return sp.getString("accent", "emerald"); }
    public void setAccent(String a) { sp.edit().putString("accent", a).apply(); }

    // 统计
    public long totalFreed() { return sp.getLong("totalFreed", 0); }
    public int totalCount() { return sp.getInt("totalCount", 0); }
    public void addStat(long freed, int count) {
        sp.edit().putLong("totalFreed", totalFreed() + freed)
                 .putInt("totalCount", totalCount() + count)
                 .putLong("lastClean", System.currentTimeMillis())
                 .apply();
    }
    public long lastClean() { return sp.getLong("lastClean", 0); }

    private String join(LinkedHashSet<String> set) {
        StringBuilder b = new StringBuilder();
        for (String s : set) { if (b.length() > 0) b.append('\n'); b.append(s); }
        return b.toString();
    }
}
