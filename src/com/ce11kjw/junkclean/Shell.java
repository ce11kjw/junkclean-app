package com.ce11kjw.junkclean;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** root/普通 shell 执行 + 能力探测（自动降级） */
public final class Shell {

    private static volatile Boolean rootCache;
    private static volatile String manager = "";

    private Shell() {}

    public static boolean hasRoot() {
        if (rootCache != null) return rootCache;
        rootCache = Boolean.FALSE;
        for (String p : new String[]{"/system/bin/su", "/system/xbin/su",
                "/sbin/su", "/su/bin/su", "/debug_ramdisk/su"}) {
            if (new File(p).exists()) { rootCache = Boolean.TRUE; break; }
        }
        if (!rootCache) {
            String w = one(false, "which su");
            if (w.contains("su")) rootCache = Boolean.TRUE;
        }
        return rootCache;
    }

    /** 请求授权并验证 uid==0 */
    public static boolean testRoot() {
        String uid = one(true, "id -u");
        boolean ok = "0".equals(uid.trim());
        if (ok) {
            rootCache = Boolean.TRUE;
            detectManager();
        }
        return ok;
    }

    /** 检测 root 管理器类型 */
    public static String detectManager() {
        if (!manager.isEmpty()) return manager;
        if (!hasRoot()) { manager = "无"; return manager; }
        String ksu = one(true, "ksud -V 2>/dev/null || echo ''");
        if (!ksu.isEmpty() && !ksu.contains("not found")) { manager = "KernelSU"; return manager; }
        String ap = one(true, "apd -V 2>/dev/null || echo ''");
        if (!ap.isEmpty() && !ap.contains("not found")) { manager = "APatch"; return manager; }
        String mg = one(true, "magisk -v 2>/dev/null || echo ''");
        if (!mg.isEmpty() && !mg.contains("not found")) {
            manager = "Magisk " + mg.split(":")[0];
            return manager;
        }
        manager = "su";
        return manager;
    }

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
        } catch (Exception ignored) {
        } finally {
            if (p != null) p.destroy();
        }
        return lines;
    }

    public static List<String> auto(String cmd) {
        return exec(hasRoot(), cmd);
    }

    public static String one(boolean root, String cmd) {
        List<String> out = exec(root, cmd);
        return out.isEmpty() ? "" : out.get(0).trim();
    }

    /** shell 单引号安全包裹 */
    public static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    /** du -sk 取字节 */
    public static long du(String path) {
        String s = one(hasRoot(), "du -sk " + quote(path) + " 2>/dev/null | cut -f1");
        try { return Long.parseLong(s.trim()) * 1024; } catch (Exception e) { return 0; }
    }

    /** fstrim 指定挂载点 */
    public static String fstrim(String mount) {
        if (!hasRoot()) return "需要 root";
        List<String> out = exec(true, "fstrim -v " + mount + " 2>&1");
        return out.isEmpty() ? "无输出" : out.get(out.size() - 1);
    }
}
