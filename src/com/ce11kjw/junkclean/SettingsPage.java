package com.ce11kjw.junkclean;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 设置：root 状态 / 白名单 / 统计 / 关于 */
public class SettingsPage {

    private final MainActivity act;
    private ScrollView scroll;
    private EditText wlInput;
    private TextView rootInfo, statInfo;

    public SettingsPage(MainActivity a) { this.act = a; }

    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        // root 状态卡
        LinearLayout rc = UI.card(act);
        rc.addView(UI.title(act, "运行环境"));
        rootInfo = UI.note(act, "");
        rc.addView(rootInfo, UI.lpm(act, UI.MP, UI.WC, 6));
        Button test = UI.secondary(act, "测试 root 权限");
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean ok = Shell.testRoot();
                act.toast(ok ? "root 授权成功" : "未获得 root（将以受限模式运行）");
                refresh();
            }
        });
        rc.addView(test, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        root.addView(rc);

        // 白名单卡
        LinearLayout wc = UI.card(act);
        wc.addView(UI.title(act, "白名单"));
        wc.addView(UI.note(act, "每行一个文件名或包名，扫描时跳过。首页长按条目可快捷加入。"));
        wlInput = new EditText(act);
        wlInput.setTextSize(12.5f);
        wlInput.setTextColor(Theme.TEXT);
        wlInput.setHintTextColor(Theme.DIM);
        wlInput.setHint("例如：com.tencent.mm\nWeiXin");
        wlInput.setBackground(Theme.card(act, 12));
        wlInput.setGravity(Gravity.TOP | Gravity.START);
        wlInput.setMinLines(4);
        int ip = Theme.dp(act, 10);
        wlInput.setPadding(ip, ip, ip, ip);
        wc.addView(wlInput, UI.lpm(act, UI.MP, UI.WC, 10));
        Button save = UI.primary(act, "保存白名单");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                List<String> list = new ArrayList<String>();
                for (String s : wlInput.getText().toString().split("\n")) {
                    if (!s.trim().isEmpty()) list.add(s.trim());
                }
                act.store.setWhitelist(list);
                act.toast("已保存 " + list.size() + " 条白名单");
            }
        });
        wc.addView(save, UI.lpm(act, UI.MP, Theme.dp(act, 42), 10));
        root.addView(wc, UI.lpm(act, UI.MP, UI.WC, 12));

        // 统计卡
        LinearLayout sc = UI.card(act);
        sc.addView(UI.title(act, "清理统计"));
        statInfo = UI.note(act, "");
        sc.addView(statInfo, UI.lpm(act, UI.MP, UI.WC, 6));
        root.addView(sc, UI.lpm(act, UI.MP, UI.WC, 12));

        // 关于卡
        LinearLayout ac = UI.card(act);
        ac.addView(UI.title(act, "关于"));
        ac.addView(UI.note(act, "JunkClean v1.0.0\n"
                + "Android 垃圾清理工具 · 深空玻璃 UI\n"
                + "有 root 时深度清理，无 root 自动降级\n"
                + "对应模块版本：github.com/ce11kjw/junkclean"),
                UI.lpm(act, UI.MP, UI.WC, 6));
        root.addView(ac, UI.lpm(act, UI.MP, UI.WC, 12));
        root.addView(UI.spacer(act, 20));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        refresh();
        return scroll;
    }

    public void refresh() {
        if (rootInfo == null) return;
        boolean root = Shell.hasRoot();
        rootInfo.setText((root ? "✓ 检测到 root" : "⚠ 未检测到 root")
                + "\n模式：" + (root ? "深度清理（/data/data 全应用缓存、系统日志）"
                                    : "受限清理（公共目录、外部缓存、自身缓存）")
                + "\nAndroid " + android.os.Build.VERSION.RELEASE
                + " · " + android.os.Build.MODEL);

        StringBuilder sb = new StringBuilder();
        Store s = act.store;
        sb.append("累计清理：").append(s.totalCount()).append(" 项\n");
        sb.append("累计释放：").append(Util.fmtSize(s.totalFreed())).append('\n');
        long last = s.lastClean();
        sb.append("上次清理：").append(last == 0 ? "从未"
                : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                    .format(new java.util.Date(last)));
        statInfo.setText(sb.toString());

        StringBuilder wl = new StringBuilder();
        for (String w : act.store.whitelist()) {
            if (wl.length() > 0) wl.append('\n');
            wl.append(w);
        }
        if (wlInput != null) wlInput.setText(wl.toString());
    }
}
