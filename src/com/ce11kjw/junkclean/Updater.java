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

        byte[] data = Net.download(url, 20000, 4 * 1024 * 1024);
        if (data == null) {
            info.error = Net.lastError.isEmpty() ? "无法访问更新地址" : Net.lastError;
            return info;
        }
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

    /** 最近一次下载失败的原因 */
    public static volatile String lastError = "";

    /** 下载 APK 到公共下载目录，返回文件；失败返回 null */
    public static String lastWritePath = "";

    public static File download(Context c, String url, Net.Progress cb) {
        lastError = "";
        lastWritePath = "";
        byte[] data = Net.download(url, 120000, 128 * 1024 * 1024, cb);
        if (data == null) {
            lastError = Net.lastError.isEmpty() ? "网络错误" : Net.lastError;
            return null;
        }
        if (data.length < 4096) {
            lastError = "文件过小，可能不是有效安装包";
            return null;
        }
        // APK 本质是 zip，头部必须是 PK
        if (!(data[0] == 0x50 && data[1] == 0x4B)) {
            lastError = "不是有效的 APK（缺少 zip 头）";
            return null;
        }
        try {
            // 优先写公共 Download（系统安装器能直接读），失败降级 app 私有目录
            File f = null;
            File dir = new File(Util.sdRoot() + "/Download");
            if ((dir.exists() || dir.mkdirs()) && dir.isDirectory()) {
                f = new File(dir, "JunkClean-update.apk");
            }
            if (f == null) {
                // 私有目录 + FileProvider 安装
                File appDir = new File(c.getFilesDir(), "update");
                if (appDir.mkdirs() || appDir.isDirectory()) {
                    f = new File(appDir, "JunkClean-update.apk");
                }
            }
            if (f == null) {
                lastError = "没有可写的下载位置";
                return null;
            }
            FileOutputStream out = new FileOutputStream(f);
            out.write(data);
            out.close();
            f.setReadable(true, false);
            lastWritePath = f.getAbsolutePath();
            return f;
        } catch (Exception e) {
            lastError = "写入失败：" + e.getMessage();
            return null;
        }
    }

    /**
     * 调起系统安装器。
     * Android 7+ 不允许传 file:// URI，这里用 MediaStore 查询已落盘文件拿 content://；
     * 拿不到时退回 file://（旧系统可用），全部失败则由调用方提示手动安装。
     */
    public static boolean install(Context c, File apk) {
        Uri uri = contentUri(c, apk);
        if (uri == null) uri = Uri.fromFile(apk);
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName();
            return false;
        }
    }

    /** 通过 MediaStore 反查已落盘文件的 content:// */
    private static Uri contentUri(Context c, File apk) {
        android.database.Cursor cur = null;
        try {
            cur = c.getContentResolver().query(
                    android.provider.MediaStore.Files.getContentUri("external"),
                    new String[]{android.provider.MediaStore.Files.FileColumns._ID},
                    android.provider.MediaStore.Files.FileColumns.DATA + "=?",
                    new String[]{apk.getAbsolutePath()}, null);
            if (cur != null && cur.moveToFirst()) {
                long id = cur.getLong(0);
                return Uri.withAppendedPath(
                        android.provider.MediaStore.Files.getContentUri("external"),
                        String.valueOf(id));
            }
            // 未收录则主动插入一条记录
            android.content.ContentValues v = new android.content.ContentValues();
            v.put(android.provider.MediaStore.Files.FileColumns.DATA, apk.getAbsolutePath());
            v.put(android.provider.MediaStore.Files.FileColumns.MIME_TYPE,
                    "application/vnd.android.package-archive");
            return c.getContentResolver().insert(
                    android.provider.MediaStore.Files.getContentUri("external"), v);
        } catch (Throwable t) {
            return null;
        } finally {
            if (cur != null) try { cur.close(); } catch (Exception ignored) {}
        }
    }

    /** 返回更新包实际写入路径（fallback 到私有目录时也正确） */
    public static String publicApkPath() {
        return lastWritePath != null && !lastWritePath.isEmpty()
                ? lastWritePath : Util.sdRoot() + "/Download/JunkClean-update.apk";
    }
}
