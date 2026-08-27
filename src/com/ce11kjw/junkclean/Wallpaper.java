package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 背景壁纸：下载直链 → 本地缓存 → center-crop 适配屏幕 → 轻模糊 + 蒙版。
 * center-crop 保证不拉伸变形；轻模糊让上层玻璃卡片有真实的磨砂观感。
 */
public final class Wallpaper {

    private static Bitmap cached;
    private static String cacheKey = "";

    private Wallpaper() {}

    private static File file(Context c) {
        return new File(c.getFilesDir(), "wallpaper.jpg");
    }

    public static boolean exists(Context c) {
        File f = file(c);
        return f.exists() && f.length() > 0;
    }

    public static String fetch(Context c, String url) {
        if (url == null || url.trim().isEmpty()) return "地址为空";
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) return "仅支持 http(s) 直链";

        byte[] data = Net.download(u, 20000, 16 * 1024 * 1024);
        if (data == null) return "下载失败或超过 16MB";

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
        cacheKey = "";
    }

    /** 供界面使用的背景：已按屏幕 center-crop，不会被拉伸 */
    public static Drawable drawable(Context c) {
        Bitmap b = bitmap(c);
        if (b == null) return null;
        BitmapDrawable d = new BitmapDrawable(c.getResources(), b);
        d.setGravity(android.view.Gravity.FILL);
        return d;
    }

    private static Bitmap bitmap(Context c) {
        File f = file(c);
        if (!f.exists() || f.length() == 0) return null;

        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int sw = dm.widthPixels, sh = dm.heightPixels;
        String key = f.lastModified() + ":" + sw + "x" + sh + ":" + (Theme.light ? "L" : "D");
        if (cached != null && !cached.isRecycled() && key.equals(cacheKey)) return cached;

        try {
            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), probe);
            if (probe.outWidth <= 0) return null;

            // 降采样到不小于屏幕，再精确 center-crop
            int scale = 1;
            while (probe.outWidth / (scale * 2) >= sw && probe.outHeight / (scale * 2) >= sh) {
                scale *= 2;
            }
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = scale;
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap src = BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
            if (src == null) return null;

            Bitmap out = centerCrop(src, sw, sh);
            if (out != src) src.recycle();

            out = softBlur(out);          // 轻模糊，给玻璃卡片提供磨砂底
            overlay(out);                 // 蒙版，保证文字对比度

            cached = out;
            cacheKey = key;
            return out;
        } catch (Throwable t) {
            return null;                  // OOM 等情况回退纯色，不崩
        }
    }

    /** 等比缩放后居中裁剪到目标尺寸，不变形 */
    private static Bitmap centerCrop(Bitmap src, int dw, int dh) {
        int sw = src.getWidth(), sh = src.getHeight();
        if (sw == dw && sh == dh) return src;

        float ratio = Math.max(dw / (float) sw, dh / (float) sh);
        int nw = Math.max(1, Math.round(sw * ratio));
        int nh = Math.max(1, Math.round(sh * ratio));

        Bitmap scaled = Bitmap.createScaledBitmap(src, nw, nh, true);
        int x = Math.max(0, (nw - dw) / 2);
        int y = Math.max(0, (nh - dh) / 2);
        int cw = Math.min(dw, nw), ch = Math.min(dh, nh);

        Bitmap out = Bitmap.createBitmap(scaled, x, y, cw, ch);
        if (scaled != out) scaled.recycle();
        return out;
    }

    /** 缩小再放大的廉价模糊，成本极低 */
    private static Bitmap softBlur(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int tw = Math.max(1, w / 10), th = Math.max(1, h / 10);
        Bitmap small = Bitmap.createScaledBitmap(src, tw, th, true);
        Bitmap blur = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(blur);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        cv.drawBitmap(small, new Rect(0, 0, tw, th), new Rect(0, 0, w, h), p);
        small.recycle();
        src.recycle();
        return blur;
    }

    private static void overlay(Bitmap b) {
        Canvas cv = new Canvas(b);
        Paint p = new Paint();
        p.setColor(Theme.light ? Color.argb(0x8C, 0xFF, 0xFF, 0xFF)
                               : Color.argb(0xA6, 0x04, 0x04, 0x0A));
        cv.drawRect(0, 0, b.getWidth(), b.getHeight(), p);
    }
}
