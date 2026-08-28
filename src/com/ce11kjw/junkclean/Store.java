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

    /**
     * 内置保护路径：手机上存放用户重要内容的目录。
     * 这些始终生效，不可通过清空白名单移除，避免误删照片、聊天记录、备份等。
     */
    public static final String[] PROTECTED = {
            // 相册与影像
            "DCIM", "Camera", "Pictures", "Screenshots", "ScreenRecorder", "MIUI/Gallery",
            // 影音与文档
            "Movies", "Music", "Documents", "Recordings", "Sounds", "Ringtones",
            "Alarms", "Notifications", "Podcasts", "Audiobooks", "eBooks", "Books",
            // 备份
            "Backup", "Backups", "backup", "MIUI/backup", "ColorOS/Backup",
            "Huawei/Backup", "Samsung/SmartSwitch", "SmartSwitch",
            // 社交软件的用户内容
            "MicroMsg", "WeiXin", "Weixin", "QQ_Images", "QQfile_recv", "Tencent/QQfile_recv",
            "Tencent/MicroMsg", "Tencent/QQ_Images", "DingTalk", "Telegram", "WhatsApp",
            // 笔记与同步盘
            "Notes", "notes", "Obsidian", "Joplin", "MarginNote",
            "Nextcloud", "Syncthing", "Dropbox", "OneDrive", "GoogleDrive", "aliyunpan",
            // 开发与密钥
            ".ssh", ".gnupg", "keystore", "KeyStore", ".android",
            // 系统与工具配置
            "Android/obb", "MagiskManager", "KernelSU", "APatch", "TWRP",
            "Fonts", "下载", "Termux",
            // 游戏存档
            "games", "Games", "gameData", "Unity", "com.miHoYo"
    };

    /** 用户白名单 + 内置保护路径 */
    /** 用户从内置保护中删除的条目（持久化） */
    public List<String> removedProt() { return split(sp.getString("removedProt", "")); }
    public void removeFromProt(String name) {
        List<String> r = removedProt();
        if (!r.contains(name)) r.add(name);
        sp.edit().putString("removedProt", joinForSet(r)).apply();
    }
    public void restoreToProt(String name) {
        List<String> r = removedProt();
        r.remove(name);
        sp.edit().putString("removedProt", joinForSet(r)).apply();
    }
    public boolean isRemovedFromProt(String name) {
        return removedProt().contains(name);
    }

    /** PROTECTED 减去被用户移除的 */
    public java.util.List<String> effectiveProtected() {
        java.util.List<String> r = new ArrayList<String>();
        java.util.List<String> removed = removedProt();
        for (String p : PROTECTED) if (!removed.contains(p)) r.add(p);
        return r;
    }

    private String joinForSet(List<String> list) {
        StringBuilder b = new StringBuilder();
        for (String s : list) { if (b.length() > 0) b.append('\n'); b.append(s); }
        return b.toString();
    }

    /** 用户白名单 + 内置保护（减去被用户移除的） */
    public List<String> whitelist() {
        List<String> out = split(sp.getString("whitelist", ""));
        for (String p : effectiveProtected()) if (!out.contains(p)) out.add(p);
        return out;
    }

    /** 仅用户自定义部分（设置页编辑框用） */
    public List<String> userWhitelist() {
        return split(sp.getString("whitelist", ""));
    }

    public boolean addWhitelist(String name) {
        LinkedHashSet<String> set = new LinkedHashSet<String>(userWhitelist());
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

    /** 玻璃模式：0=关 1=浅色玻璃 2=深色玻璃 */
    public int glassMode() { return sp.getInt("glassMode", 2); }
    public void setGlassMode(int m) { sp.edit().putInt("glassMode", m).apply(); }

    /** 玻璃穿透度 0..1（存 0..100 整数避免精度漂移） */
    public float glassOpacity() { return sp.getInt("glassOpacity", 18) / 100f; }
    public void setGlassOpacity(float v) { sp.edit().putInt("glassOpacity", (int)(v * 100)).apply(); }

    /** 玻璃边缘模糊半径 dp：0/3/6/10 */
    public int glassBlur() { return sp.getInt("glassBlur", 6); }
    public void setGlassBlur(int b) { sp.edit().putInt("glassBlur", b).apply(); }

    // ---------- 重复文件保留策略 ----------
    public String keepPolicy() { return sp.getString("keepPolicy", "newest"); }
    public void setKeepPolicy(String p) { sp.edit().putString("keepPolicy", p).apply(); }

    // ---------- 目录排行缓存（重启后仍可见） ----------
    /** 每行：path\tsize */
    public List<String> rankCache() { return split(sp.getString("rankCache", "")); }
    public long rankTime() { return sp.getLong("rankTime", 0); }
    public void setRankCache(List<String> lines) {
        StringBuilder b = new StringBuilder();
        for (String l : lines) { if (b.length() > 0) b.append('\n'); b.append(l); }
        sp.edit().putString("rankCache", b.toString())
                 .putLong("rankTime", System.currentTimeMillis()).apply();
    }
    public void clearRankCache() {
        sp.edit().remove("rankCache").remove("rankTime").apply();
    }

    /** 排行统计深度：1=仅一级，2=两级 */
    public int rankDepth() { return sp.getInt("rankDepth", 2); }
    public void setRankDepth(int d) { sp.edit().putInt("rankDepth", d).apply(); }

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

    /** 端点与 Key 是否已配置好（用于折叠输入框，只留模型可改） */
    public boolean aiConfigured() {
        return sp.getBoolean("aiConfigured", false)
                && !aiEndpoint().trim().isEmpty();
    }
    public void setAiConfigured(boolean b) { sp.edit().putBoolean("aiConfigured", b).apply(); }

    // ---------- 壁纸 ----------
    public String bgUrl() { return sp.getString("bgUrl", ""); }
    public void setBgUrl(String u) { sp.edit().putString("bgUrl", u).apply(); }

    // ---------- 设置开关 ----------
    public boolean toTrash() { return sp.getBoolean("toTrash", true); }
    public void setToTrash(boolean b) { sp.edit().putBoolean("toTrash", b).apply(); }

    public int trashDays() { return sp.getInt("trashDays", 7); }
    public void setTrashDays(int d) { sp.edit().putInt("trashDays", d).apply(); }

    public String scanRoot() { return sp.getString("scanRoot", ""); }

    /** 全盘扫描（需 root）：扫描 /data /system /cache 等系统分区 */
    public boolean fullScan() { return sp.getBoolean("fullScan", false); }
    public void setFullScan(boolean b) { sp.edit().putBoolean("fullScan", b).apply(); }
    public void setScanRoot(String s) { sp.edit().putString("scanRoot", s).apply(); }

    public boolean catEnabled(String id) { return sp.getBoolean("cat_" + id, true); }
    public void setCatEnabled(String id, boolean on) {
        sp.edit().putBoolean("cat_" + id, on).apply();
    }

    // ---------- 整理中心：全局统一源目录 ----------
    public String orgSrc() {
        return sp.getString("orgSrc", Util.sdRoot() + "/下载");
    }
    public void setOrgSrc(String v) { sp.edit().putString("orgSrc", v).apply(); }

    // ---------- 远程更新 ----------
    public String updateUrl() {
        return sp.getString("updateUrl",
                "https://api.github.com/repos/ce11kjw/junkclean-app/releases/latest");
    }
    public void setUpdateUrl(String v) { sp.edit().putString("updateUrl", v).apply(); }

    // ---------- 整理规则（dst|recursive|integrity 每行一条，源统一取 orgSrc） ----------
    public List<String> rules() {
        String sd = Util.sdRoot();
        String def = sd + "/JunkClean整理|1|1\n"
                + sd + "/Pictures/整理相册|1|1\n"
                + sd + "/Movies/整理视频|1|1\n"
                + sd + "/Music/整理音频|1|1\n"
                + sd + "/Documents/整理文档|1|1\n"
                + sd + "/Download/整理压缩包|0|1\n"
                + sd + "/Download/整理安装包|0|1";
        return split(sp.getString("rules", def));
    }

    public void setRules(List<String> list) {
        sp.edit().putString("rules", joinList(list)).apply();
    }

    public String extMap() {
        return sp.getString("extMap", DEFAULT_EXT_MAP);
    }

    /** 默认分类映射，覆盖常见文件类型 */
    public static final String DEFAULT_EXT_MAP =
            ".jpg,.jpeg,.png,.gif,.webp,.heic,.heif,.bmp,.avif,.tiff,.tif,.jfif=图片\n"
          + ".mp4,.mkv,.mov,.avi,.webm,.3gp,.m4v,.flv,.wmv,.mpeg,.mpg,.ts,.rmvb=视频\n"
          + ".mp3,.flac,.wav,.m4a,.ogg,.aac,.ape,.wma,.opus,.amr,.mid=音频\n"
          + ".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.rtf,.odt,.csv,.md=文档\n"
          + ".epub,.mobi,.azw3,.fb2,.djvu=电子书\n"
          + ".zip,.7z,.rar,.tar,.gz,.xz,.bz2,.zst,.tgz,.iso=压缩包\n"
          + ".apk,.apks,.xapk,.apkm,.aab=安装包\n"
          + ".psd,.ai,.sketch,.fig,.xd,.svg,.eps=设计文件\n"
          + ".java,.kt,.py,.js,.ts,.c,.cpp,.h,.go,.rs,.php,.rb,.sh,.json,.xml,.yaml,.yml=代码\n"
          + ".ttf,.otf,.woff,.woff2=字体\n"
          + ".torrent,.magnet=种子\n"
          + ".log,.bak,.old,.dmp=日志备份";

    public void setExtMap(String s) { sp.edit().putString("extMap", s).apply(); }
    public void resetExtMap() { sp.edit().remove("extMap").apply(); }
    public void resetRules() { sp.edit().remove("rules").apply(); }

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

    // ---------- 配置导出 / 导入 ----------

    /** 导出为纯文本，换机后可粘贴回来。密钥不导出，避免明文外泄 */
    public String exportConfig() {
        StringBuilder b = new StringBuilder();
        b.append("# JunkClean 配置\n");
        b.append("theme=").append(theme()).append('\n');
        b.append("accent=").append(accent()).append('\n');
        b.append("toTrash=").append(toTrash() ? 1 : 0).append('\n');
        b.append("trashDays=").append(trashDays()).append('\n');
        b.append("scanRoot=").append(scanRoot()).append('\n');
        b.append("fullScan=").append(fullScan() ? 1 : 0).append('\n');
        b.append("keepPolicy=").append(keepPolicy()).append('\n');
        b.append("orgSrc=").append(orgSrc()).append('\n');
        b.append("[whitelist]\n");
        for (String w : userWhitelist()) b.append(w).append('\n');
        b.append("[rules]\n");
        for (String r : rules()) b.append(r).append('\n');
        b.append("[extmap]\n").append(extMap()).append('\n');
        return b.toString();
    }

    /** 导入配置，返回应用的条目数 */
    public int importConfig(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        int applied = 0;
        String section = "";
        List<String> wl = new ArrayList<String>();
        List<String> rl = new ArrayList<String>();
        StringBuilder ext = new StringBuilder();

        for (String line : text.split("\n")) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            if (l.startsWith("[") && l.endsWith("]")) {
                section = l.substring(1, l.length() - 1);
                continue;
            }
            if ("whitelist".equals(section)) { wl.add(l); continue; }
            if ("rules".equals(section)) { rl.add(l); continue; }
            if ("extmap".equals(section)) { ext.append(l).append('\n'); continue; }

            int eq = l.indexOf('=');
            if (eq <= 0) continue;
            String k = l.substring(0, eq).trim();
            String v = l.substring(eq + 1).trim();
            if ("theme".equals(k)) { setTheme(v); applied++; }
            else if ("accent".equals(k)) { setAccent(v); applied++; }
            else if ("toTrash".equals(k)) { setToTrash("1".equals(v)); applied++; }
            else if ("trashDays".equals(k)) { setTrashDays(parseInt(v, 7)); applied++; }
            else if ("scanRoot".equals(k)) { setScanRoot(v); applied++; }
            else if ("fullScan".equals(k)) { setFullScan("1".equals(v)); applied++; }
            else if ("keepPolicy".equals(k)) { setKeepPolicy(v); applied++; }
            else if ("orgSrc".equals(k)) { setOrgSrc(v); applied++; }
        }
        if (!wl.isEmpty()) { setWhitelist(wl); applied += wl.size(); }
        if (!rl.isEmpty()) { setRules(rl); applied += rl.size(); }
        if (ext.length() > 0) { setExtMap(ext.toString().trim()); applied++; }
        return applied;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    // ---------- 分类累计统计 ----------

    /** 按分类累计释放量，用于「哪类垃圾最多」 */
    public void addCatStat(String catId, long freed, int count) {
        if (catId == null || catId.isEmpty()) return;
        sp.edit().putLong("cat_freed_" + catId, catFreed(catId) + freed)
                 .putInt("cat_count_" + catId, catCount(catId) + count).apply();
    }
    public long catFreed(String catId) { return sp.getLong("cat_freed_" + catId, 0); }
    public int catCount(String catId) { return sp.getInt("cat_count_" + catId, 0); }

    public void resetCatStats() {
        SharedPreferences.Editor e = sp.edit();
        for (String id : CAT_IDS) e.remove("cat_freed_" + id).remove("cat_count_" + id);
        e.apply();
    }

    public static final String[] CAT_IDS = {
            "cache", "webview", "log", "temp", "thumb",
            "apkjunk", "emptyjunk", "residue", "syscache"
    };
    public static final String[] CAT_NAMES = {
            "应用缓存", "WebView", "日志", "临时文件", "缩略图",
            "冗余安装包", "空文件", "应用残留", "系统缓存"
    };

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
