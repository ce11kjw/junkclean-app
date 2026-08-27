package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileOutputStream;

/** 背景壁纸：下载直链 → 本地缓存 → 压暗后作为窗口背景 */
public final class Wallpaper {

    private static Bitmap cached;
    private static String cachedUrl = "";

    private Wallpaper() {}

    private static File file(Context c) {
        return new File(c.getFilesDir(), "wallpaper.jpg");
    }

    public static boolean exists(Context c) {
        return file(c).exists() && file(c).length() > 0;
    }

    /** 从直链下载并落盘，成功返回 null，失败返回原因 */
    public static String fetch(Context c, String url) {
        if (url == null || url.trim().isEmpty()) return "地址为空";
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            return "仅支持 http(s) 直链";
        }
        byte[] data = Net.download(u, 15000, 12 * 1024 * 1024);
        if (data == null) return "下载失败或超过 12MB";

        // 校验能否解码为图片，同时按屏幕尺寸降采样，避免大图占内存
        BitmapFactory.Options probe = new BitmapFactory.Options();
        probe.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, probe);
        if (probe.outWidth <= 0 || probe.outHeight <= 0) return "不是有效的图片";

        try {
            FileOutputStream out = new FileOutputStream(file(c));
            out.write(data);
            out.close();
        } catch (Exception e) {
            return "写入失败：" + e.getMessage();
        }
        invalidate();
        return null;
    }

    public static void clear(Context c) {
        Util.rmrf(file(c));
        invalidate();
    }

    public static void invalidate() {
        if (cached != null && !cached.isRecycled()) cached.recycle();
        cached = null;
        cachedUrl = "";
    }

    /**
     * 生成窗口背景：按屏幕降采样 + 叠加暗色蒙版，
     * 保证卡片与文字在任意壁纸上都有足够对比度。
     */
    public static Drawable drawable(Context c) {
        File f = file(c);
        if (!f.exists() || f.length() == 0) return null;

        String key = f.getAbsolutePath() + ":" + f.lastModified();
        if (cached != null && !cached.isRecycled() && key.equals(cachedUrl)) {
            return new BitmapDrawable(c.getResources(), cached);
        }

        try {
            android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
            int reqW = dm.widthPixels, reqH = dm.heightPixels;

            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), probe);
            int scale = 1;
            while (probe.outWidth / (scale * 2) >= reqW && probe.outHeight / (scale * 2) >= reqH) {
                scale *= 2;
            }
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = scale;
            opt.inPreferredConfig = Bitmap.Config.RGB_565;   // 省一半内存
            Bitmap src = BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
            if (src == null) return null;

            // 叠加蒙版：深色主题压暗，浅色主题提亮
            Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
            if (out == null) out = src;
            else if (out != src) src.recycle();

            Canvas cv = new Canvas(out);
            Paint p = new Paint();
            p.setColor(Theme.light ? Color.argb(0x9E, 0xFF, 0xFF, 0xFF)
                                   : Color.argb(0xB0, 0x05, 0x05, 0x09));
            cv.drawRect(0, 0, out.getWidth(), out.getHeight(), p);

            cached = out;
            cachedUrl = key;
            return new BitmapDrawable(c.getResources(), out);
        } catch (Throwable t) {
            return null;   // OOM 等情况直接放弃壁纸，回退纯色
        }
    }
}
