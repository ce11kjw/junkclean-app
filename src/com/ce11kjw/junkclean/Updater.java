package com.ce11kjw.junkclean;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;

/** 远程更新：读取 GitHub Release 或自定义 JSON，下载 APK 后交系统安装 */
public final class Updater {

    public static class Info {
        public String version = "";
        public String notes = "";
        public String apkUrl = "";
        public boolean newer;
        public String error;
    }

    private Updater() {}

    /** 查询远程版本。支持 GitHub Release JSON 与自定义 {version,apkUrl,notes} */
    public static Info check(Store store) {
        Info info = new Info();
        String url = store.updateUrl().trim();
        if (url.isEmpty()) { info.error = "未配置更新地址"; return info; }

        byte[] data = Net.download(url, 15000, 2 * 1024 * 1024);
        if (data == null) { info.error = "无法访问更新地址"; return info; }
        String json;
        try { json = new String(data, "UTF-8"); }
        catch (Exception e) { info.error = "响应编码异常"; return info; }

        String tag = Net.jsonStr(json, "tag_name");
        if (tag == null) tag = Net.jsonStr(json, "version");
        if (tag == null) { info.error = "响应中没有版本字段"; return info; }
        info.version = tag.startsWith("v") ? tag.substring(1) : tag;

        String notes = Net.jsonStr(json, "body");
        if (notes == null) notes = Net.jsonStr(json, "notes");
        info.notes = notes == null ? "" : notes;

        // GitHub Release 的 browser_download_url，优先取 .apk
        String apk = findApkUrl(json);
        if (apk == null) apk = Net.jsonStr(json, "apkUrl");
        info.apkUrl = apk == null ? "" : apk;

        info.newer = compare(info.version, MainActivity.VERSION) > 0;
        return info;
    }

    /** 扫描所有 browser_download_url，返回第一个 .apk */
    private static String findApkUrl(String json) {
        String key = "\"browser_download_url\"";
        int i = json.indexOf(key);
        while (i >= 0) {
            int q1 = json.indexOf('"', json.indexOf(':', i + key.length()) + 1);
            int q2 = q1 < 0 ? -1 : json.indexOf('"', q1 + 1);
            if (q1 > 0 && q2 > q1) {
                String u = json.substring(q1 + 1, q2);
                if (u.toLowerCase(java.util.Locale.US).endsWith(".apk")) return u;
            }
            i = json.indexOf(key, i + key.length());
        }
        return null;
    }

    /** 语义化版本比较：a>b 返回正数 */
    public static int compare(String a, String b) {
        String[] x = a.split("[^0-9]+"), y = b.split("[^0-9]+");
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int xi = i < x.length ? parse(x[i]) : 0;
            int yi = i < y.length ? parse(y[i]) : 0;
            if (xi != yi) return xi - yi;
        }
        return 0;
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    /** 下载 APK 到私有目录，返回文件；失败返回 null */
    public static File download(Context c, String url) {
        byte[] data = Net.download(url, 60000, 64 * 1024 * 1024);
        if (data == null || data.length < 1024) return null;
        // APK 是 zip，头部应为 PK\003\004
        if (!(data[0] == 0x50 && data[1] == 0x4B)) return null;
        try {
            File dir = new File(c.getFilesDir(), "update");
            dir.mkdirs();
            File f = new File(dir, "JunkClean-update.apk");
            FileOutputStream out = new FileOutputStream(f);
            out.write(data);
            out.close();
            f.setReadable(true, false);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    /** 调起系统安装器 */
    public static boolean install(Context c, File apk) {
        try {
            Uri uri = androidx_FileProvider_fallback(c, apk);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 不引入 androidx，直接把文件复制到公共下载目录再用 file:// 打开。
     * Android 7+ 禁止直接分享 file://，因此改为提示用户手动安装。
     */
    private static Uri androidx_FileProvider_fallback(Context c, File apk) {
        File pub = new File(Util.sdRoot() + "/Download/JunkClean-update.apk");
        if (Util.move(new File(apk.getAbsolutePath()), pub)) {
            return Uri.fromFile(pub);
        }
        return Uri.fromFile(apk);
    }

    /** 返回公共下载目录中的更新包路径，供 UI 提示 */
    public static String publicApkPath() {
        return Util.sdRoot() + "/Download/JunkClean-update.apk";
    }
}
