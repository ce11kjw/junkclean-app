package com.ce11kjw.junkclean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import java.io.File;

/**
 * 感知哈希：找「构图相似」的照片视频。
 *
 * - aHash（平均哈希）：缩到 8x8 灰度，比较每像素与均值的差，64 bit。
 * - dHash（差分哈希）：每像素与右边像素比，64 bit，抗亮度变化更好。
 *
 * 视频抽首帧比对。纯 CPU 计算，不依赖任何系统 API。
 *
 * 汉明距离 < 阈值 → 视觉相似；=0 → 完全相同。
 */
public final class PerceptualHash {

    private PerceptualHash() {}

    public static final int HASH_BITS = 64;
    /** 汉明距离 ≤ 此值视为「同一组」。12/64 = 81% 相似度。 */
    public static final int DEFAULT_THRESHOLD = 12;

    /** 视频抽帧失败/不支持的格式时直接跳过，避免崩溃 */
    public static long safeHash(File f) {
        try {
            return aHash(f);
        } catch (Throwable ignored) {
            return 0L;   // 返回 0 表示「无法参与视觉比较」，调用方跳过
        }
    }

    /** aHash：均值比较 */
    public static long aHash(File f) {
        Bitmap bm = decode(f);
        if (bm == null) return 0L;
        try {
            Bitmap small = Bitmap.createScaledBitmap(bm, 8, 8, true);
            int[] g = toGray(small);
            int avg = avg(g);
            long hash = 0L;
            for (int i = 0; i < 64; i++) {
                if (g[i] >= avg) hash |= (1L << i);
            }
            return hash;
        } finally {
            bm.recycle();
        }
    }

    /** dHash：差分。抗亮度变化。 */
    public static long dHash(File f) {
        Bitmap bm = decode(f);
        if (bm == null) return 0L;
        try {
            // 9x8 灰度，第 i 行第 j 位 = 第 i 行 (j+1) - 第 i 行 j 的正负
            Bitmap small = Bitmap.createScaledBitmap(bm, 9, 8, true);
            int[] g = toGray(small);
            long hash = 0L;
            for (int i = 0; i < 64; i++) {
                int row = i / 8, col = i % 8;
                int a = g[row * 9 + col], b = g[row * 9 + col + 1];
                if (a > b) hash |= (1L << i);
            }
            return hash;
        } finally {
            bm.recycle();
        }
    }

    /** 汉明距离（64 bit 之间的不同位数） */
    public static int distance(long a, long b) {
        long x = a ^ b;
        return Long.bitCount(x);
    }

    /**
     * 媒体抽帧：图片直接解码；视频抽第 1 帧
     * @return 解码后的 Bitmap，失败返回 null
     */
    private static Bitmap decode(File f) {
        String n = f.getName().toLowerCase(java.util.Locale.US);
        boolean video = n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".mov")
                || n.endsWith(".avi") || n.endsWith(".webm") || n.endsWith(".3gp")
                || n.endsWith(".flv") || n.endsWith(".m4v") || n.endsWith(".wmv")
                || n.endsWith(".ts");
        if (video) return extractFrame(f);
        // 图片：缩到 9x8 即可，省内存
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = Math.max(1, (int) (Math.max(f.length() / 4096, 1)));
        return BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
    }

    private static Bitmap extractFrame(File f) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            return r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
    }

    private static int[] toGray(Bitmap bm) {
        int w = bm.getWidth(), h = bm.getHeight();
        int[] out = new int[w * h];
        int[] px = new int[w * h];
        bm.getPixels(px, 0, w, 0, 0, w, h);
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            // ITU-R BT.601 luma
            out[i] = (r * 299 + g * 587 + b * 114) / 1000;
        }
        return out;
    }

    private static int avg(int[] g) {
        long s = 0;
        for (int v : g) s += v;
        return (int) (s / g.length);
    }

    public static boolean isImage(File f) {
        String n = f.getName().toLowerCase(java.util.Locale.US);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                || n.endsWith(".webp") || n.endsWith(".heic") || n.endsWith(".heif")
                || n.endsWith(".bmp") || n.endsWith(".gif") || n.endsWith(".avif");
    }

    public static boolean isVideo(File f) {
        String n = f.getName().toLowerCase(java.util.Locale.US);
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".mov")
                || n.endsWith(".avi") || n.endsWith(".webm") || n.endsWith(".3gp")
                || n.endsWith(".flv") || n.endsWith(".m4v") || n.endsWith(".wmv")
                || n.endsWith(".ts");
    }
}
