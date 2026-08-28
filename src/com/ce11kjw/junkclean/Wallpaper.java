package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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

    private static volatile Bitmap cached;
    private static volatile String cacheKey = "";

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

    /**
     * 只丢弃引用，不回收 bitmap。
     * 之前这里直接 recycle()，但同一个 bitmap 可能仍被挂在存活 View 的
     * BitmapDrawable 上，重建界面时会抛 "trying to use a recycled bitmap"。
     * 交给 GC 回收是安全的做法。
     */
    public static void invalidate() {
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
            // 不 recycle：壁纸可能与界面生命周期不同步，回收早了会爆 "recycled bitmap"。
            // 缓存由 invalidate() 通过 null 引用放弃，交给 GC 即可。

            // 不模糊不蒙版：保留壁纸原画质。用户主动装壁纸就是要看清楚的，
            // 玻璃卡片的可读性由卡片自身的对比策略负责（见卡片方案）。
            cached = out;
            cacheKey = key;
            return out;
        } catch (Throwable t) {
            return null;                  // OOM 等情况回退纯色，不崩
        }
    }

    /** 等比缩放后居中裁剪到目标尺寸，不变形。原画质，无模糊。 */
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
        // scaled 是中间产物，不挂任何 UI，裁剪完立刻回收
        if (scaled != out && !scaled.isRecycled()) scaled.recycle();
        // src 也不回收：见 invalidate() 注释
        return out;
    }


}
