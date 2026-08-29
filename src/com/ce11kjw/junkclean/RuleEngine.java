package com.ce11kjw.junkclean;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 规则化清理引擎（借鉴 SD Maid SystemCleaner + DirRules + black_and_white_list）。
 *
 * 规则数据落地到 /sdcard/.junkclean/rules.json，用户可编辑/导入/导出。
 * 每条规则按「代价从低到高」短路匹配，与 SD Maid 的评估顺序一致：
 *   targetType → size → age → pathContains → nameEnds/nameStarts → exclusions → regex（最贵）
 */
public class RuleEngine {

    /** 单条清理规则 */
    public static class Rule {
        public String id = "";
        public String label = "";
        public String risk = "low";          // low / mid / high
        public String targetType = "any";     // file / dir / any
        public List<String> pathContains = new ArrayList<String>();
        public List<String> nameStarts = new ArrayList<String>();
        public List<String> nameEnds = new ArrayList<String>();
        public List<String> exclusions = new ArrayList<String>();
        public List<String> regex = new ArrayList<String>();
        public long minSize = 0;               // 字节
        public long maxSize = 0;               // 0 = 不限
        public long minAgeDays = 0;            // 0 = 不限
        public boolean enabled = true;
    }

    /** 规则文件路径 */
    public static String rulesPath() {
        return Util.sdRoot() + "/.junkclean/rules.json";
    }

    /** 加载规则：文件不存在则写入默认规则再读 */
    public static List<Rule> load(Context ctx) {
        File f = new File(rulesPath());
        if (!f.exists()) {
            writeDefault();
        }
        String json = readFile(f);
        if (json == null || json.trim().isEmpty()) json = DEFAULT_RULES_JSON;
        return parse(json);
    }

    /** 写入默认规则到磁盘（覆盖） */
    public static boolean writeDefault() {
        return writeFile(new File(rulesPath()), DEFAULT_RULES_JSON);
    }

    /** 解析 JSON → 规则列表 */
    public static List<Rule> parse(String json) {
        List<Rule> out = new ArrayList<Rule>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("rules");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Rule r = new Rule();
                r.id = o.optString("id", "r" + i);
                r.label = o.optString("label", r.id);
                r.risk = o.optString("risk", "low");
                r.targetType = o.optString("targetType", "any");
                r.minSize = o.optLong("minSize", 0);
                r.maxSize = o.optLong("maxSize", 0);
                r.minAgeDays = o.optLong("minAgeDays", 0);
                r.enabled = o.optBoolean("enabled", true);
                r.pathContains = arrToList(o.optJSONArray("pathContains"));
                r.nameStarts = arrToList(o.optJSONArray("nameStarts"));
                r.nameEnds = arrToList(o.optJSONArray("nameEnds"));
                r.exclusions = arrToList(o.optJSONArray("exclusions"));
                r.regex = arrToList(o.optJSONArray("regex"));
                out.add(r);
            }
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * 扫描给定根目录，返回命中任一规则的文件/目录。
     * @param wl 白名单（跳过）
     */
    public static List<JunkItem> scan(String root, List<Rule> rules, List<String> wl) {
        List<JunkItem> out = new ArrayList<JunkItem>();
        // 预编译 regex
        List<java.util.regex.Pattern> allRegex = new ArrayList<java.util.regex.Pattern>();
        for (Rule r : rules) {
            for (String rx : r.regex) {
                try { allRegex.add(java.util.regex.Pattern.compile(rx)); }
                catch (Exception ignored) {}
            }
        }
        walk(new File(root), 0, 8, rules, wl, out);
        return out;
    }

    private static void walk(File dir, int depth, int maxDepth,
                             List<Rule> rules, List<String> wl, List<JunkItem> out) {
        if (depth > maxDepth || out.size() > 2000) return;
        if (dir.getName().equals(".junkclean_trash") || dir.getName().equals(".junkclean")) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            String name = f.getName();
            String path = f.getAbsolutePath();
            if (Finder.inWhitelist(wl, name) || Finder.inWhitelist(wl, path)) continue;

            for (Rule r : rules) {
                if (!r.enabled) continue;
                if (matches(r, f, name, path)) {
                    long size = f.isDirectory() ? Util.dirSize(f) : f.length();
                    if (size > 0) out.add(new JunkItem(path, name + "  ·  " + r.label, size));
                    break;   // 命中一条即归属，不重复
                }
            }
            // 继续递归子目录（除非该目录本身被规则命中删除，仍递归以覆盖子项）
            if (f.isDirectory()) walk(f, depth + 1, maxDepth, rules, wl, out);
        }
    }

    /** 单文件对单规则匹配，按代价短路 */
    private static boolean matches(Rule r, File f, String name, String path) {
        // 1. targetType（最便宜）
        if ("file".equals(r.targetType) && !f.isFile()) return false;
        if ("dir".equals(r.targetType) && !f.isDirectory()) return false;

        // 2. size
        long size = f.isFile() ? f.length() : -1;
        if (r.minSize > 0 && size >= 0 && size < r.minSize) return false;
        if (r.maxSize > 0 && size >= 0 && size > r.maxSize) return false;

        // 3. age
        if (r.minAgeDays > 0) {
            long ageMs = System.currentTimeMillis() - f.lastModified();
            if (ageMs < r.minAgeDays * 86400000L) return false;
        }

        String low = name.toLowerCase(Locale.US);
        String lowPath = path.toLowerCase(Locale.US);

        // 4. pathContains（OR）
        if (!r.pathContains.isEmpty()) {
            boolean hit = false;
            for (String p : r.pathContains) {
                if (lowPath.contains(p.toLowerCase(Locale.US))) { hit = true; break; }
            }
            if (!hit) return false;
        }
        // 5. nameStarts（OR）
        if (!r.nameStarts.isEmpty()) {
            boolean hit = false;
            for (String p : r.nameStarts) if (low.startsWith(p.toLowerCase(Locale.US))) { hit = true; break; }
            if (!hit) return false;
        }
        // 6. nameEnds（OR）
        if (!r.nameEnds.isEmpty()) {
            boolean hit = false;
            for (String p : r.nameEnds) if (low.endsWith(p.toLowerCase(Locale.US))) { hit = true; break; }
            if (!hit) return false;
        }
        // 7. exclusions（命中任一即排除）
        for (String ex : r.exclusions) {
            if (lowPath.contains(ex.toLowerCase(Locale.US))) return false;
        }
        // 8. regex（最贵，最后）
        if (!r.regex.isEmpty()) {
            boolean hit = false;
            for (String rx : r.regex) {
                try { if (path.matches(rx)) { hit = true; break; } } catch (Exception ignored) {}
            }
            if (!hit) return false;
        }
        // 若规则没有任何 name/path/regex 条件，视为不匹配（避免全盘误删）
        if (r.pathContains.isEmpty() && r.nameStarts.isEmpty()
                && r.nameEnds.isEmpty() && r.regex.isEmpty()) {
            return false;
        }
        return true;
    }

    // ---------- 工具 ----------
    private static List<String> arrToList(JSONArray a) {
        List<String> out = new ArrayList<String>();
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            String s = a.optString(i, "");
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static String readFile(File f) {
        try {
            byte[] buf = new byte[(int) f.length()];
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            int n = in.read(buf);
            in.close();
            return new String(buf, 0, Math.max(n, 0), "UTF-8");
        } catch (Exception e) { return null; }
    }

    private static boolean writeFile(File f, String content) {
        try {
            File p = f.getParentFile();
            if (p != null && !p.isDirectory()) p.mkdirs();
            java.io.FileOutputStream out = new java.io.FileOutputStream(f);
            out.write(content.getBytes("UTF-8"));
            out.close();
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * 默认规则库。借鉴 SD Maid SystemCleaner 分类 + DirRules/black_and_white_list 常见路径。
     * 风险分级：low 默认清理 / mid 谨慎 / high 只列不默认删。
     */
    public static final String DEFAULT_RULES_JSON =
        "{\n" +
        "  \"version\": 1,\n" +
        "  \"rules\": [\n" +
        "    {\"id\":\"log_files\",\"label\":\"日志文件\",\"risk\":\"low\",\"targetType\":\"file\",\n" +
        "     \"nameEnds\":[\".log\",\".log.1\",\".logcat\"],\"exclusions\":[\"backup\",\"important\"]},\n" +
        "    {\"id\":\"temp_files\",\"label\":\"临时文件\",\"risk\":\"low\",\"targetType\":\"file\",\n" +
        "     \"nameEnds\":[\".tmp\",\".temp\",\".part\",\".crdownload\",\".download\",\".bak\",\".old\"]},\n" +
        "    {\"id\":\"thumb_cache\",\"label\":\"缩略图缓存\",\"risk\":\"low\",\"targetType\":\"dir\",\n" +
        "     \"pathContains\":[\"/.thumbnails\",\"/.face\"]},\n" +
        "    {\"id\":\"tencent_log\",\"label\":\"腾讯日志\",\"risk\":\"low\",\n" +
        "     \"pathContains\":[\"/tencent/msflogs\",\"/tencent/qalsdklogs\",\"/tencent/imsdklogs\",\n" +
        "       \"/tencent/wns\",\"/tencent/tbs_live_log\",\"/tencent/beacon\",\"/tencent/tpush/logs\"]},\n" +
        "    {\"id\":\"mac_files\",\"label\":\"Mac 残留\",\"risk\":\"low\",\"targetType\":\"file\",\n" +
        "     \"nameEnds\":[\".ds_store\"],\"nameStarts\":[\"._\"]},\n" +
        "    {\"id\":\"windows_files\",\"label\":\"Windows 残留\",\"risk\":\"low\",\"targetType\":\"file\",\n" +
        "     \"nameEnds\":[\"thumbs.db\",\"desktop.ini\"]},\n" +
        "    {\"id\":\"lostdir\",\"label\":\"LOST.DIR\",\"risk\":\"low\",\"targetType\":\"dir\",\n" +
        "     \"pathContains\":[\"/lost.dir\"]},\n" +
        "    {\"id\":\"ad_files\",\"label\":\"广告缓存\",\"risk\":\"mid\",\n" +
        "     \"pathContains\":[\"/.adcache\",\"/adcache\",\"/.ad/\",\"/gdt_plugin\",\"/adnet\"]},\n" +
        "    {\"id\":\"analytics\",\"label\":\"统计埋点\",\"risk\":\"mid\",\n" +
        "     \"pathContains\":[\"/bugly\",\"/umeng\",\"/.mta/\",\"/mobclick\"]},\n" +
        "    {\"id\":\"crash_dump\",\"label\":\"崩溃转储\",\"risk\":\"low\",\"targetType\":\"file\",\n" +
        "     \"nameEnds\":[\".dmp\",\".hprof\"],\"pathContains\":[\"crash\",\"tombstone\"]},\n" +
        "    {\"id\":\"apk_installer\",\"label\":\"安装包\",\"risk\":\"mid\",\"targetType\":\"file\",\n" +
        "     \"nameEnds\":[\".apk\",\".apks\",\".xapk\",\".apkm\"],\"minSize\":1048576,\n" +
        "     \"exclusions\":[\"backup\"]}\n" +
        "  ]\n" +
        "}\n";
}
