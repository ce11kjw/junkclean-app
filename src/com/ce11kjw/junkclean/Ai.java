package com.ce11kjw.junkclean;

import java.util.ArrayList;
import java.util.List;

/** AI 建议：OpenAI 兼容 /chat/completions 接口 */
public final class Ai {

    private Ai() {}

    /** 返回 AI 文本，失败返回 "ERR:..." */
    public static String advise(Store store, String userContent) {
        String base = store.aiEndpoint().trim();
        String key = store.aiKey().trim();
        String model = store.aiModel().trim();
        if (base.isEmpty()) return "ERR:未配置 AI 端点，请在设置中填写";
        if (model.isEmpty()) model = "gpt-4o-mini";
        if (!base.endsWith("/chat/completions")) {
            base = base.replaceAll("/+$", "") + "/chat/completions";
        }

        String sys = "你是 Android 存储清理助手。根据用户提供的扫描结果，"
                + "指出哪些项目可以安全清理、哪些需要谨慎、以及节省空间的建议。"
                + "回答简洁，用中文，分条列出，不超过 300 字。";

        String body = "{\"model\":\"" + Net.esc(model) + "\","
                + "\"messages\":[{\"role\":\"system\",\"content\":\"" + Net.esc(sys) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + Net.esc(userContent) + "\"}],"
                + "\"temperature\":0.4,\"max_tokens\":600}";

        String resp = Net.postJson(base, key, body, 30000);
        if (resp.startsWith("ERR:")) return resp;

        String content = Net.jsonStr(resp, "content");
        if (content == null || content.trim().isEmpty()) {
            String err = Net.jsonStr(resp, "message");
            return "ERR:" + (err != null ? err : "响应中没有 content 字段");
        }
        return content.trim();
    }

    /** 把扫描结果整理成给 AI 的摘要 */
    public static String summarize(List<JunkCategory> cats, long freeBytes, long totalBytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("设备存储：总 ").append(Util.fmtSize(totalBytes))
          .append("，可用 ").append(Util.fmtSize(freeBytes)).append("。\n扫描结果：\n");
        boolean any = false;
        for (JunkCategory c : cats) {
            if (c.items.isEmpty()) continue;
            any = true;
            sb.append("- ").append(c.name).append("：").append(c.items.size())
              .append(" 项，").append(Util.fmtSize(c.total()));
            if (c.careful) sb.append("（谨慎分类）");
            sb.append('\n');
            int n = Math.min(3, c.items.size());
            for (int i = 0; i < n; i++) {
                sb.append("    · ").append(c.items.get(i).name)
                  .append(" ").append(Util.fmtSize(c.items.get(i).size)).append('\n');
            }
        }
        if (!any) sb.append("（未发现垃圾）\n");
        sb.append("请给出清理建议。");
        return sb.toString();
    }

    /**
     * 让 AI 确认一组视觉相似文件是否真重复。
     * 传文件名+大小+感知哈希距离，AI 判断「是否同一张照片/同一段视频」。
     * 返回 true 表示 AI 认为它们重复，false 表示不同内容。
     */
    public static boolean verifyDupGroup(Store store, Finder.DupGroup g) {
        String base = store.aiEndpoint().trim();
        if (base.isEmpty()) return true;  // 没配 AI 就当相似即重复
        String model = store.aiModel().trim();
        if (model.isEmpty()) model = "gpt-4o-mini";

        StringBuilder sb = new StringBuilder();
        sb.append("判断以下文件是否视觉上是重复的（同一张照片/同一段视频）。\n\n");
        sb.append("感知哈希距离（0=完全相同，越大越不同）：\n");
        for (JunkItem it : g.files) {
            sb.append("· ").append(it.name).append("  ").append(Util.fmtSize(it.size)).append('\n');
        }
        sb.append("\n如果它们是同一内容的重复文件，回复「重复」；如果只是构图相似但内容不同，回复「不重复」。");
        String r = advise(store, sb.toString());
        return !r.toLowerCase(java.util.Locale.US).contains("不重复");
    }

    /** 解析 AI 回答中提到的分类名，用于「采纳建议」自动勾选 */
    public static List<String> matchCategories(String advice, List<JunkCategory> cats) {
        List<String> hit = new ArrayList<String>();
        if (advice == null) return hit;
        for (JunkCategory c : cats) {
            if (c.items.isEmpty()) continue;
            if (advice.contains(c.name)) hit.add(c.id);
        }
        return hit;
    }
}
