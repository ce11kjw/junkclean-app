package com.ce11kjw.junkclean;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** 极简 HTTP 客户端（HttpURLConnection，无第三方依赖） */
public final class Net {

    /** 下载进度回调 */
    public interface Progress {
        void onProgress(int done, int total);
    }

    private Net() {}

    /** POST JSON，返回响应体；失败返回 "ERR:" 前缀的说明 */
    public static String postJson(String url, String bearer, String body, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "JunkClean-App");
            if (bearer != null && !bearer.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + bearer);
            }
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String resp = readAll(in);
            if (code >= 400) return "ERR:HTTP " + code + " " + brief(resp);
            return resp;
        } catch (Exception e) {
            return "ERR:" + e.getClass().getSimpleName() + " " + String.valueOf(e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 最近一次 download 的失败原因，供 UI 展示 */
    public static String lastError = "";

    /** 下载二进制到字节数组，超出 maxBytes 视为失败 */
    public static byte[] download(String url, int timeoutMs, int maxBytes) {
        return download(url, timeoutMs, maxBytes, null);
    }

    public static byte[] download(String url, int timeoutMs, int maxBytes, Progress cb) {
        lastError = "";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            // GitHub API 拒绝没有 User-Agent 的请求（403）
            conn.setRequestProperty("User-Agent", "JunkClean-App");
            conn.setRequestProperty("Accept", "*/*");
            int code = conn.getResponseCode();
            if (code >= 400) {
                lastError = "HTTP " + code;
                return null;
            }
            int len = conn.getContentLength();
            InputStream in = conn.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[32768];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > maxBytes) {
                    in.close();
                    lastError = "超过大小上限";
                    return null;
                }
                bos.write(buf, 0, n);
                if (cb != null && len > 0) cb.onProgress(total, len);
            }
            in.close();
            return bos.toByteArray();
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName() + " " + String.valueOf(e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) sb.append(l).append('\n');
        r.close();
        return sb.toString();
    }

    private static String brief(String s) {
        if (s == null) return "";
        s = s.replace('\n', ' ').trim();
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    /** 极简 JSON 字符串取值：找 "key":"value"，自动反转义 */
    public static String jsonStr(String json, String key) {
        String pat = "\"" + key + "\"";
        int i = json.indexOf(pat);
        while (i >= 0) {
            int c = json.indexOf(':', i + pat.length());
            if (c < 0) return null;
            int j = c + 1;
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
            if (j < json.length() && json.charAt(j) == '"') {
                StringBuilder sb = new StringBuilder();
                boolean esc = false;
                for (int k = j + 1; k < json.length(); k++) {
                    char ch = json.charAt(k);
                    if (esc) {
                        if (ch == 'n') sb.append('\n');
                        else if (ch == 't') sb.append('\t');
                        else if (ch == 'r') sb.append('\r');
                        else if (ch == 'u' && k + 4 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(k + 1, k + 5), 16));
                                k += 4;
                            } catch (Exception ignored) {}
                        } else sb.append(ch);
                        esc = false;
                    } else if (ch == '\\') {
                        esc = true;
                    } else if (ch == '"') {
                        return sb.toString();
                    } else {
                        sb.append(ch);
                    }
                }
                return sb.toString();
            }
            i = json.indexOf(pat, i + pat.length());
        }
        return null;
    }

    /** JSON 字符串转义（构造请求体用） */
    public static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
