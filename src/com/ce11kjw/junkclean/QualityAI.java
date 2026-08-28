package com.ce11kjw.junkclean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 质量评估：用用户已配的 OpenAI 兼容视觉模型分析照片/视频帧。
 * 替代 ML Kit（依赖太重，破坏纯 Java 架构）。
 *
 * 评估维度（每个文件一个 verdict）：
 *   - sharpness：清晰度（0~1）
 *   - composition：构图（0~1）
 *   - has_face：是否含人脸
 *   - face_count：人脸数
 *   - reason：模型给的简短原因
 *
 * 用途：FilesPage 重复文件卡在「保留策略=best」时，AI 评分挑最优。
 */
public final class QualityAI {

    public static class Verdict {
        public float sharpness = 0;
        public float composition = 0;
        public boolean hasFace = false;
        public int faceCount = 0;
        public String reason = "";
        public float score = 0;   // 综合分 sharpness*0.4 + composition*0.3 + faceBonus*0.3
    }

    /**
     * 对一组文件评分，按综合分排序。
     * 需要 aiReady()=true 且 aiEndpoint 配好；否则本地启发式评分。
     */
    public static void judge(android.content.Context ctx, List<JunkItem> files,
                             Store store, java.util.Map<String, Verdict> out) {
        if (!store.aiReady() || files.isEmpty()) {
            // 无 AI 时用启发式：文件大小 + 修改时间作为质量代理
            long maxSize = 0;
            for (JunkItem it : files) if (it.size > maxSize) maxSize = it.size;
            for (JunkItem it : files) {
                Verdict v = new Verdict();
                v.sharpness = maxSize > 0 ? Math.min(1f, (float) it.size / maxSize) : 0.5f;
                v.composition = 0.5f;
                v.score = v.sharpness * 0.7f + 0.3f;
                v.reason = "本地启发式评分（未配 AI）";
                // 人脸检测加分
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(it.path);
                if (bmp != null) {
                    int faces = FaceDetector.countFaces(ctx, bmp);
                    if (faces > 0) { v.score += 0.15f * Math.min(faces, 3); v.hasFace = true; v.faceCount = faces; }
                    if (!bmp.isRecycled()) bmp.recycle();
                }
                out.put(it.path, v);
            }
            return;
        }

        // 有 AI：批量评分（一次请求多张）
        StringBuilder sb = new StringBuilder(
                "你是照片质量评估员。逐张 1~100 分打分并给出 1~3 词理由。\\n\\n"
              + "评分维度：\\n"
              + "- sharpness：清晰度（模糊曝光扣分）\\n"
              + "- composition：构图（人像/风景/构图合理性）\\n"
              + "- content：表情/瞬间捕捉/故事性\\n\\n"
              + "对每张图输出一行：`<序号> <sharp 0~100> <comp 0~100> <content 0~100> <理由>`\\n\\n");
        for (int i = 0; i < files.size(); i++) {
            sb.append("图").append(i + 1).append("：").append(files.get(i).name).append("\\n");
        }
        sb.append("\\n只输出评分行，不要其他文字。");

        // 编码图片 + 发请求（简化：一次发一张，避免 token 超限）
        for (int i = 0; i < files.size(); i++) {
            Verdict v = judgeOne(ctx, files.get(i), sb.toString().replace("图" + (i + 1),
                    files.get(i).name), store);
            out.put(files.get(i).path, v);
        }
    }

    private static Verdict judgeOne(android.content.Context ctx, JunkItem item, String prompt, Store store) {
        Verdict v = new Verdict();
        String base64 = encodeImage(item.path);
        if (base64 == null) {
            v.reason = "图片解码失败";
            v.score = 0;
            return v;
        }

        // 构建多模态消息体：图片 base64 + 文本 prompt
        String body = "{"
                + "\"model\":\"" + esc(store.aiModel()) + "\","
                + "\"messages\":[{"
                + "\"role\":\"user\","
                + "\"content\":[{"
                + "\"type\":\"image_url\","
                + "\"image_url\":{\"url\":\"data:image/jpeg;base64," + base64 + "\"}"
                + "},{"
                + "\"type\":\"text\","
                + "\"text\":\"" + esc(prompt) + "\""
                + "}]}]}";

        String endpoint = store.aiEndpoint().trim();
        if (!endpoint.endsWith("/chat/completions")) {
            endpoint = endpoint.replaceAll("/+$", "") + "/chat/completions";
        }
        String resp = Net.postJson(endpoint, store.aiKey().trim(), body, 30000);
        if (resp.startsWith("ERR:")) {
            v.reason = resp.substring(4);
            v.score = 0;
            return v;
        }

        String content = Net.jsonStr(resp, "content");
        if (content != null) {
            // 简单解析：首行 "sharp comp content reason"
            String firstLine = content.split("\\n", 2)[0].trim();
            String[] parts = firstLine.split("\\s+", 4);
            if (parts.length >= 3) {
                try {
                    int sharp = Integer.parseInt(parts[0]);
                    int comp = Integer.parseInt(parts[1]);
                    int cont = Integer.parseInt(parts[2]);
                    v.sharpness = sharp / 100f;
                    v.composition = comp / 100f;
                    v.score = (v.sharpness * 0.4f + v.composition * 0.3f + (cont / 100f) * 0.3f);
                    if (parts.length >= 4) v.reason = parts[3];
                } catch (NumberFormatException ignored) {}
            }
            // 人脸检测加分（本地 OpenCV）
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(item.path);
            if (bmp != null) {
                int faces = FaceDetector.countFaces(ctx, bmp);
                if (faces > 0) {
                    v.score += 0.15f * Math.min(faces, 3);
                    v.hasFace = true;
                    v.faceCount = faces;
                }
                if (!bmp.isRecycled()) bmp.recycle();
            }
        }
        return v;
    }

    private static String encodeImage(String path) {
        // 缩到 512x512，JPEG 70% 质量，节省 token
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 8;
        Bitmap bm = BitmapFactory.decodeFile(path, opt);
        if (bm == null) return null;
        Bitmap small = bm;
        if (bm.getWidth() > 512 || bm.getHeight() > 512) {
            int w = bm.getWidth(), h = bm.getHeight();
            float scale = Math.min(512f / w, 512f / h);
            small = Bitmap.createScaledBitmap(bm, (int)(w * scale), (int)(h * scale), true);
            if (small != bm && !small.isRecycled()) small.recycle();
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, bos);
        byte[] data = bos.toByteArray();
        if (bm != small && !bm.isRecycled()) bm.recycle();
        if (!small.isRecycled()) small.recycle();
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return Net.esc(s);
    }
}
