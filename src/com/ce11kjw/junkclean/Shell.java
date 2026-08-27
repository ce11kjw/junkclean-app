package com.ce11kjw.junkclean;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** root/普通 shell 执行 + 能力探测（自动降级） */
public final class Shell {

    private static Boolean rootCache;

    private Shell() {}

    /** 是否有可用 root（缓存结果） */
    public static boolean hasRoot() {
        if (rootCache != null) return rootCache;
        rootCache = Boolean.FALSE;
        for (String p : new String[]{"/system/bin/su", "/system/xbin/su",
                "/sbin/su", "/su/bin/su", "/debug_ramdisk/su"}) {
            if (new java.io.File(p).exists()) {
                rootCache = Boolean.TRUE;
                break;
            }
        }
        if (!rootCache) {
            // PATH 里找 su
            List<String> out = exec(false, "which su");
            if (!out.isEmpty() && out.get(0).contains("su")) rootCache = Boolean.TRUE;
        }
        return rootCache;
    }

    /** 请求一次 root 授权并验证 id */
    public static boolean testRoot() {
        List<String> out = exec(true, "id -u");
        boolean ok = !out.isEmpty() && out.get(0).trim().equals("0");
        rootCache = ok ? Boolean.TRUE : rootCache;
        return ok;
    }

    /** 执行命令，返回 stdout 行（root=true 时走 su） */
    public static List<String> exec(boolean root, String cmd) {
        List<String> lines = new ArrayList<String>();
        Process p = null;
        try {
            if (root) {
                p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                os.writeBytes(cmd + "\n");
                os.writeBytes("exit\n");
                os.flush();
            } else {
                p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;
            while ((l = r.readLine()) != null) lines.add(l);
            r.close();
            p.waitFor();
        } catch (Exception e) {
            // 忽略，返回已读到的行
        } finally {
            if (p != null) p.destroy();
        }
        return lines;
    }

    /** 便捷：自动选择 root 或普通 */
    public static List<String> auto(String cmd) {
        return exec(hasRoot(), cmd);
    }

    /** 单行输出 */
    public static String one(boolean root, String cmd) {
        List<String> out = exec(root, cmd);
        return out.isEmpty() ? "" : out.get(0).trim();
    }
}
