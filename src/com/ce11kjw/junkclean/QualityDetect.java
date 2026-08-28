package com.ce11kjw.junkclean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

/**
 * 低质量废片识别（参考 photoo 思路，纯本地实现）：
 *
 *   1. 模糊 — 用边缘检测（Sobel 近似）统计高梯度像素比例，比例过低 = 糊
 *   2. 暗光 — 平均灰度 < 阈值
 *   3. 过曝 — 高亮（>245）像素比例 > 阈值
 *
 * 不用拉普拉斯/FFT（贵），用 3x3 邻域差分近似 Sobel，单线程也能跑几千张。
 */
public final class QualityDetect {

    private QualityDetect() {}

    public static class Verdict {
        public boolean blurry, dark, overexposed;
        public float sharpness, brightness, highlight;
        public String summary() {
            StringBuilder sb = new StringBuilder();
            if (blurry) sb.append("模糊 ");
            if (dark) sb.append("暗光 ");
            if (overexposed) sb.append("过曝 ");
            return sb.length() == 0 ? "正常" : sb.toString().trim();
        }
    }

    /** 判定单张图片质量 */
    public static Verdict judge(File f) {
        Verdict v = new Verdict();
        Bitmap bm = load(f);
        if (bm == null) {
            v.brightness = -1;
            return v;   // 解码失败，不归类
        }
        try {
            Bitmap small = Bitmap.createScaledBitmap(bm, 64, 64, true);
            int w = small.getWidth(), h = small.getHeight();
            int[] px = new int[w * h];
            small.getPixels(px, 0, w, 0, 0, w, h);

            long sum = 0, bright = 0, edge = 0, total = w * h;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int gray = luma(px[y * w + x]);
                    sum += gray;
                    if (gray > 245) bright++;
                    // 3x3 Sobel-X + Sobel-Y 差分（只算上/左邻域，够用）
                    if (x > 0 && y > 0) {
                        int g00 = luma(px[(y - 1) * w + (x - 1)]);
                        int g10 = luma(px[(y - 1) * w + x]);
                        int g01 = luma(px[y * w + (x - 1)]);
                        int g11 = luma(px[y * w + x]);
                        int gx = Math.abs(g00 + g01 - g10 - g11);
                        int gy = Math.abs(g00 + g10 - g01 - g11);
                        if (gx + gy > 90) edge++;   // 强梯度 = 清晰边缘
                    }
                }
            }
            v.brightness = (float) sum / total;
            v.highlight = (float) bright / total;
            v.sharpness = (float) edge / total;

            // 阈值（经验值，可调）
            v.blurry = v.sharpness < 0.015f;
            v.dark = v.brightness < 45f;
            v.overexposed = v.highlight > 0.6f;
            return v;
        } finally {
            if (bm != null && !bm.isRecycled()) bm.recycle();
        }
    }

    /** 全量扫描：返回判定为「低质量」的图片文件 */
    public static void scan(String root, java.util.List<String> wl,
                            java.util.List<JunkItem> out, java.util.List<Verdict> verdicts) {
        java.io.File dir = new java.io.File(root);
        java.io.File[] fs = dir.listFiles();
        if (fs == null) return;
        for (java.io.File f : fs) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !Finder.inWhitelist(wl, f.getName())) {
                    scan(f.getAbsolutePath(), wl, out, verdicts);
                }
            } else if (PerceptualHash.isImage(f)) {
                if (Finder.inWhitelist(wl, f.getName())) continue;
                Verdict v = judge(f);
                if (v.blurry || v.dark || v.overexposed) {
                    JunkItem it = new JunkItem(f.getAbsolutePath(), f.getName(), f.length());
                    it.checked = false;
                    out.add(it);
                    verdicts.add(v);
                }
            }
        }
    }

    private static int luma(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    private static Bitmap load(File f) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 8;   // 64x64 就够，省内存
        try {
            return BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
        } catch (Throwable t) {
            return null;
        }
    }
}
