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
            return isVideo(f) ? mediaHash(f) : aHash(f);
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
            int[] g = preprocessAndGray(small);
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
            int[] g = preprocessAndGray(small);
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
        // 视频交给 mediaHash() 专门处理（多帧指纹）；这里只解码图片
        if (isVideo(f)) return null;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = Math.max(1, (int) (Math.max(f.length() / 4096, 1)));
        return BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
    }

    /**
     * 视频多帧指纹：抽首/中/尾 3 帧的 dHash，取「多数一致」位。
     * 比单首帧鲁棒：转码（画面不变）三帧都一致；剪辑（部分帧不同）
     * 多数帧仍一致 → 识别为同一视频。参考 VDF 思路的视觉降级版。
     */
    public static long mediaHash(File f) {
        if (!isVideo(f)) return aHash(f);
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            long durUs = 0;
            String dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                try { durUs = Long.parseLong(dur) * 1000L; } catch (Exception ignored) {}
            }
            long[] frames = new long[3];
            Bitmap b0 = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            Bitmap b1 = durUs > 0 ? r.getFrameAtTime(durUs / 2, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                   : r.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            Bitmap b2 = durUs > 0 ? r.getFrameAtTime(durUs - 1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                   : r.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            frames[0] = b0 != null ? dHashOf(b0) : 0;
            frames[1] = b1 != null ? dHashOf(b1) : 0;
            frames[2] = b2 != null ? dHashOf(b2) : 0;
            if (b0 != null && !b0.isRecycled()) b0.recycle();
            if (b1 != null && !b1.isRecycled()) b1.recycle();
            if (b2 != null && !b2.isRecycled()) b2.recycle();

            // 多数一致位：三帧里 ≥2 帧相同的位置取 1
            long maj = 0;
            for (int bit = 0; bit < 64; bit++) {
                int ones = 0;
                for (long fh : frames) ones += ((fh >> bit) & 1L);
                if (ones >= 2) maj |= (1L << bit);
            }
            return maj;
        } catch (Throwable ignored) {
            return 0;
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
    }

    /** 对 Bitmap 直接算 dHash（复用预处理） */
    private static long dHashOf(Bitmap bm) {
        try {
            Bitmap small = Bitmap.createScaledBitmap(bm, 9, 8, true);
            int[] g = preprocessAndGray(small);
            long hash = 0L;
            for (int i = 0; i < 64; i++) {
                int row = i / 8, col = i % 8;
                int a = g[row * 9 + col], b = g[row * 9 + col + 1];
                if (a > b) hash |= (1L << i);
            }
            if (small != bm && !small.isRecycled()) small.recycle();
            return hash;
        } catch (Throwable t) {
            return 0;
        }
    }


    /**
     * 预处理（抗滤镜/亮度变化）：
     *   gamma 校正 — 亮度归一，弱化不同曝光
     *   直方图均衡 — 拉伸对比度，弱化滤镜带来的色偏
     * 参考 file-deduplicator 的 preprocessing。
     */
    private static int[] preprocessAndGray(Bitmap bm) {
        int w = bm.getWidth(), h = bm.getHeight();
        int[] px = new int[w * h];
        bm.getPixels(px, 0, w, 0, 0, w, h);

        // gamma 校正：value^(1/gamma)，gamma=1.2 轻微提亮暗部
        int[] gammaLut = new int[256];
        for (int i = 0; i < 256; i++) {
            double v = Math.pow(i / 255.0, 1.0 / 1.2);
            gammaLut[i] = (int) (v * 255);
        }

        // 先算灰度 + 直方图（用 gamma 后的值）
        int[] gray = new int[w * h];
        int[] hist = new int[256];
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int r = gammaLut[(c >> 16) & 0xFF];
            int g = gammaLut[(c >> 8) & 0xFF];
            int b = gammaLut[c & 0xFF];
            int lum = (r * 299 + g * 587 + b * 114) / 1000;
            gray[i] = lum;
            hist[lum]++;
        }

        // 直方图均衡：CDF 映射
        int total = px.length;
        int[] cdf = new int[256];
        int cum = 0;
        for (int i = 0; i < 256; i++) {
            cum += hist[i];
            cdf[i] = (int) (255.0 * cum / total);
        }
        for (int i = 0; i < gray.length; i++) {
            gray[i] = cdf[gray[i]];
        }
        return gray;
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
