package com.ce11kjw.junkclean;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 设置：环境 / 外观 / 壁纸 / AI / 清理行为 / 扫描分类 / 白名单 / 更新 / 关于 */
public class SettingsPage extends PageBase {

    private ScrollView scroll;
    private EditText wlInput, rootInput, bgInput, aiEndpoint, aiKey, aiModel, updUrl;
    private TextView rootInfo, bgState, aiState, updState, permState;

    public SettingsPage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, 14);
        root.setPadding(p, p, p, p);

        root.addView(UI.section(act, "运行环境"));
        root.addView(envCard());
        root.addView(UI.section(act, "外观"));
        root.addView(themeCard());
        root.addView(UI.section(act, "背景壁纸"));
        root.addView(wallpaperCard());
        root.addView(UI.section(act, "AI 清理建议"));
        root.addView(aiCard());
        root.addView(UI.section(act, "清理行为"));
        root.addView(behaviorCard());
        root.addView(UI.section(act, "扫描分类"));
        root.addView(catCard());
        root.addView(UI.section(act, "白名单"));
        root.addView(whitelistCard());
        root.addView(UI.section(act, "远程更新"));
        root.addView(updateCard());
        root.addView(UI.section(act, "关于"));
        root.addView(aboutCard());
        root.addView(UI.spacer(act, 24));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        refresh();
        return scroll;
    }

    // ---------- 运行环境 ----------

    private View envCard() {
        LinearLayout c = UI.card(act);
        rootInfo = UI.note(act, "");
        c.addView(rootInfo);
        permState = UI.note(act, "");
        c.addView(permState, UI.lpm(act, UI.MP, UI.WC, 6));

        Button test = UI.secondary(act, "测试 root 权限");
        Button perm = UI.secondary(act, "应用列表权限");
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean ok = Shell.testRoot();
                act.toast(ok ? "root 授权成功，可深度清理" : "未获得 root，运行在受限模式");
                refresh();
                act.homePage().refreshRootBadge();
            }
        });
        perm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { act.requestPackagePermission(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, test, perm), UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    // ---------- 外观 ----------

    private View themeCard() {
        LinearLayout c = UI.card(act);

        LinearLayout tRow = UI.row(act);
        tRow.addView(UI.text(act, "主题", 12.5f, Theme.TEXT),
                new LinearLayout.LayoutParams(Theme.dp(act, 50), UI.WC));
        String[][] themes = {{"dark","深色"},{"oled","OLED"},{"light","浅色"}};
        String curTheme = act.store.theme();
        for (String[] t : themes) {
            final String key = t[0];
            Button b = UI.chip(act, t[1], key.equals(curTheme));
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.store.setTheme(key);
                    act.applyThemeAndRebuild();
                }
            });
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, Theme.dp(act, 26));
            lp.rightMargin = Theme.dp(act, 5);
            tRow.addView(b, lp);
        }
        c.addView(tRow);

        LinearLayout aRow = UI.row(act);
        aRow.addView(UI.text(act, "强调色", 12.5f, Theme.TEXT),
                new LinearLayout.LayoutParams(Theme.dp(act, 50), UI.WC));
        String[][] accents = {{"emerald","青绿"},{"violet","紫"},{"blue","蓝"},{"pink","粉"}};
        String curAccent = act.store.accent();
        for (String[] a : accents) {
            final String key = a[0];
            Button b = UI.chip(act, a[1], key.equals(curAccent));
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.store.setAccent(key);
                    act.applyThemeAndRebuild();
                }
            });
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, Theme.dp(act, 26));
            lp.rightMargin = Theme.dp(act, 5);
            aRow.addView(b, lp);
        }
        c.addView(aRow, UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    // ---------- 壁纸 ----------

    private View wallpaperCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "填图片直链，按屏幕居中裁剪不拉伸；启用后卡片转为半透明玻璃"));

        bgInput = UI.input(act, "https://example.com/bg.jpg", act.store.bgUrl());
        c.addView(bgInput, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 10));

        bgState = UI.note(act, "");
        c.addView(bgState, UI.lpm(act, UI.MP, UI.WC, 6));

        Button apply = UI.primary(act, "下载并应用");
        Button clear = UI.secondary(act, "恢复纯色");
        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { applyWallpaper(); }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Wallpaper.clear(act);
                act.store.setBgUrl("");
                bgInput.setText("");
                act.applyWallpaperAndRebuild();
            }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, apply, clear), UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    private void applyWallpaper() {
        final String url = bgInput.getText().toString().trim();
        if (url.isEmpty()) { act.toast("请填写图片直链"); return; }
        bgState.setText("下载中…");
        new Thread(new Runnable() {
            public void run() {
                final String err = Wallpaper.fetch(act, url);
                ui.post(new Runnable() {
                    public void run() {
                        if (err != null) {
                            bgState.setText("失败：" + err);
                            act.toast("壁纸设置失败");
                            return;
                        }
                        act.store.setBgUrl(url);
                        act.applyWallpaperAndRebuild();
                    }
                });
            }
        }).start();
    }

    // ---------- AI ----------

    private View aiCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "OpenAI 兼容接口；首页扫描后可让 AI 分析并给建议"));

        c.addView(UI.note(act, "API 端点"), UI.lpm(act, UI.MP, UI.WC, 8));
        aiEndpoint = UI.input(act, "https://api.openai.com/v1", act.store.aiEndpoint());
        c.addView(aiEndpoint, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

        c.addView(UI.note(act, "API Key"), UI.lpm(act, UI.MP, UI.WC, 6));
        aiKey = UI.input(act, "sk-…", act.store.aiKey());
        aiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        c.addView(aiKey, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

        c.addView(UI.note(act, "模型（留空用 gpt-4o-mini）"), UI.lpm(act, UI.MP, UI.WC, 6));
        aiModel = UI.input(act, "gpt-4o-mini", act.store.aiModel());
        c.addView(aiModel, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

        aiState = UI.note(act, "");
        c.addView(aiState, UI.lpm(act, UI.MP, UI.WC, 8));

        Button save = UI.primary(act, "保存");
        Button test = UI.secondary(act, "测试连接");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveAi();
                aiState.setText("已保存");
                act.toast("AI 配置已保存");
            }
        });
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { testAi(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, save, test), UI.lpm(act, UI.MP, UI.WC, 6));
        return c;
    }

    private void saveAi() {
        act.store.setAi(aiEndpoint.getText().toString().trim(),
                aiKey.getText().toString().trim(),
                aiModel.getText().toString().trim());
    }

    private void testAi() {
        saveAi();
        aiState.setText("请求中…");
        new Thread(new Runnable() {
            public void run() {
                final String r = Ai.advise(act.store, "这是连通性测试，请回复「连接正常」。");
                ui.post(new Runnable() {
                    public void run() {
                        aiState.setText(r.startsWith("ERR:") ? "✗ " + r.substring(4)
                                : "✓ " + (r.length() > 60 ? r.substring(0, 60) + "…" : r));
                    }
                });
            }
        }).start();
    }

    // ---------- 清理行为 ----------

    private View behaviorCard() {
        LinearLayout c = UI.card(act);

        c.addView(UI.switchRow(act, "先移入回收站",
                "sdcard 文件先进回收站可恢复；系统缓存直接删除",
                act.store.toTrash(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                act.store.setToTrash(on);
            }
        }));

        LinearLayout dRow = UI.row(act);
        LinearLayout dInfo = UI.col(act);
        dInfo.addView(UI.text(act, "回收站保留天数", 12.5f, Theme.TEXT));
        final TextView dVal = UI.note(act, daysLabel(act.store.trashDays()));
        dInfo.addView(dVal);
        dRow.addView(dInfo, new LinearLayout.LayoutParams(0, UI.WC, 1f));
        Button dBtn = UI.secondary(act, "修改");
        dBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String[] labels = {"永久保留", "3 天", "7 天", "14 天", "30 天"};
                final int[] vals = {0, 3, 7, 14, 30};
                int cur = 0;
                for (int i = 0; i < vals.length; i++) if (vals[i] == act.store.trashDays()) cur = i;
                UI.pick(act, "回收站保留天数", labels, cur,
                        new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        act.store.setTrashDays(vals[w]);
                        dVal.setText(daysLabel(vals[w]));
                        d.dismiss();
                    }
                });
            }
        });
        dRow.addView(dBtn, UI.lp(Theme.dp(act, 56), Theme.dp(act, 30)));
        c.addView(dRow, UI.lpm(act, UI.MP, UI.WC, 6));

        c.addView(UI.switchRow(act, "全盘扫描（需 root）",
                "额外扫描 /data /cache 等系统分区，耗时更长",
                act.store.fullScan(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                if (on && !Shell.hasRoot()) {
                    v.setChecked(false);
                    act.toast("全盘扫描需要 root 权限");
                    return;
                }
                act.store.setFullScan(on);
                ScanEngine.invalidate();
                act.toast(on ? "已开启全盘扫描" : "已关闭全盘扫描");
            }
        }));

        c.addView(UI.note(act, "自定义扫描根目录（留空用 sdcard 根）"), UI.lpm(act, UI.MP, UI.WC, 10));
        rootInput = UI.input(act, Util.sdRoot(), act.store.scanRoot());
        c.addView(rootInput, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 4));

        Button saveRoot = UI.primary(act, "保存目录");
        Button clearCache = UI.secondary(act, "清除扫描缓存");
        Button autoTrash = UI.secondary(act, "清理过期项");
        saveRoot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.store.setScanRoot(rootInput.getText().toString().trim());
                ScanEngine.invalidate();
                act.toast("已保存，下次扫描生效");
            }
        });
        clearCache.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ScanEngine.invalidate();
                act.toast("扫描缓存已清除");
            }
        });
        autoTrash.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int d = act.store.trashDays();
                if (d <= 0) { act.toast("当前设置为永久保留"); return; }
                long f = Trash.autoClean(d);
                act.toast(f > 0 ? "已清理过期项 · 释放 " + Util.fmtSize(f) : "没有过期项目");
            }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, saveRoot, clearCache, autoTrash),
                UI.lpm(act, UI.MP, UI.WC, 8));

        Button resetRules = UI.secondary(act, "恢复默认整理规则");
        Button resetMap = UI.secondary(act, "恢复默认分类映射");
        resetRules.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.confirm(act, "恢复默认规则", "将覆盖现有整理规则，恢复为内置的 7 条默认规则。",
                        new Runnable() {
                    public void run() {
                        act.store.resetRules();
                        act.toast("已恢复默认整理规则");
                    }
                });
            }
        });
        resetMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.confirm(act, "恢复默认映射", "将覆盖现有分类映射，恢复为内置的 12 类默认映射。",
                        new Runnable() {
                    public void run() {
                        act.store.resetExtMap();
                        act.toast("已恢复默认分类映射");
                    }
                });
            }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, resetRules, resetMap), UI.lpm(act, UI.MP, UI.WC, 6));
        return c;
    }

    private String daysLabel(int d) {
        return d <= 0 ? "永久保留，不自动删除" : d + " 天后自动删除";
    }

    // ---------- 扫描分类 ----------

    private View catCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "关闭的分类在首页扫描时会被跳过"));
        String[][] cats = {
                {"cache", "应用缓存"}, {"webview", "WebView 缓存"}, {"log", "日志文件"},
                {"temp", "临时文件"}, {"thumb", "缩略图缓存"}, {"apkjunk", "冗余安装包"},
                {"emptyjunk", "空文件"}, {"residue", "应用残留"},
                {"syscache", "系统缓存（全盘）"}
        };
        for (String[] cat : cats) {
            final String id = cat[0];
            c.addView(UI.switchRow(act, cat[1], null, act.store.catEnabled(id),
                    new android.widget.CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                    act.store.setCatEnabled(id, on);
                    ScanEngine.invalidate();
                }
            }));
        }
        return c;
    }

    // ---------- 白名单 ----------

    private View whitelistCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "每行一个文件名、目录名或包名。所有扫描功能都会跳过。列表项长按可快捷加入。"));
        wlInput = UI.multiline(act, "例如：\ncom.tencent.mm\nWeiXin", "", 4);
        c.addView(wlInput, UI.lpm(act, UI.MP, UI.WC, 10));

        Button save = UI.primary(act, "保存");
        Button clear = UI.secondary(act, "清空");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                List<String> list = new ArrayList<String>();
                for (String s : wlInput.getText().toString().split("\n"))
                    if (!s.trim().isEmpty()) list.add(s.trim());
                act.store.setWhitelist(list);
                ScanEngine.invalidate();
                act.toast("已保存 " + list.size() + " 条白名单");
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.confirm(act, "清空白名单", "确认清空所有白名单条目？", new Runnable() {
                    public void run() {
                        act.store.setWhitelist(new ArrayList<String>());
                        wlInput.setText("");
                        ScanEngine.invalidate();
                        act.toast("白名单已清空");
                    }
                });
            }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, save, clear), UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    // ---------- 远程更新 ----------

    private View updateCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "默认读取本项目 GitHub Release；也可填自定义 JSON（version / apkUrl / notes）"));

        updUrl = UI.input(act, "更新源地址", act.store.updateUrl());
        c.addView(updUrl, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 10));

        updState = UI.note(act, "当前版本 v" + MainActivity.VERSION);
        c.addView(updState, UI.lpm(act, UI.MP, UI.WC, 6));

        Button save = UI.secondary(act, "保存地址");
        Button check = UI.primary(act, "检查更新");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.store.setUpdateUrl(updUrl.getText().toString().trim());
                act.toast("更新源已保存");
            }
        });
        check.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { checkUpdate(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, save, check), UI.lpm(act, UI.MP, UI.WC, 8));
        return c;
    }

    private void checkUpdate() {
        act.store.setUpdateUrl(updUrl.getText().toString().trim());
        updState.setText("检查中…");
        new Thread(new Runnable() {
            public void run() {
                final Updater.Info info = Updater.check(act.store);
                ui.post(new Runnable() {
                    public void run() {
                        if (info.error != null) {
                            updState.setText("检查失败：" + info.error);
                            return;
                        }
                        if (!info.newer) {
                            updState.setText("已是最新版本 v" + MainActivity.VERSION
                                    + "（远程 v" + info.version + "）");
                            return;
                        }
                        updState.setText("发现新版本 v" + info.version);
                        String notes = info.notes;
                        if (notes.length() > 600) notes = notes.substring(0, 600) + "…";
                        UI.confirm(act, "发现新版本 v" + info.version,
                                (notes.isEmpty() ? "" : notes + "\n\n") + "下载并安装？",
                                new Runnable() {
                            public void run() { doUpdate(info); }
                        });
                    }
                });
            }
        }).start();
    }

    private void doUpdate(final Updater.Info info) {
        if (info.apkUrl == null || info.apkUrl.isEmpty()) {
            updState.setText("更新源中没有 APK 下载地址");
            return;
        }
        updState.setText("下载中…");
        new Thread(new Runnable() {
            public void run() {
                final File f = Updater.download(act, info.apkUrl);
                ui.post(new Runnable() {
                    public void run() {
                        if (f == null) {
                            updState.setText("下载失败");
                            return;
                        }
                        boolean ok = Updater.install(act, f);
                        updState.setText(ok ? "已调起安装程序"
                                : "无法自动安装，请手动打开：\n" + Updater.publicApkPath());
                    }
                });
            }
        }).start();
    }

    // ---------- 关于 ----------

    private View aboutCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act,
                "JunkClean v" + MainActivity.VERSION + "\n"
                + "Android 垃圾清理工具 · 深空玻璃 UI\n"
                + "纯原生 Java，无第三方依赖\n"
                + "有 root 深度清理，无 root 自动降级\n\n"
                + "App：github.com/ce11kjw/junkclean-app\n"
                + "模块：github.com/ce11kjw/junkclean"));
        return c;
    }

    // ---------- 刷新 ----------

    public void refresh() {
        if (rootInfo == null) return;
        boolean root = Shell.hasRoot();
        rootInfo.setText((root ? "✓ 已获得 root（" + Shell.detectManager() + "）" : "⚠ 未检测到 root")
                + "\n清理模式：" + (root ? "深度（全应用缓存 + 系统日志）"
                                       : "受限（公共目录 + 外部缓存 + 自身缓存）")
                + "\n设备：" + android.os.Build.MODEL
                + " · Android " + android.os.Build.VERSION.RELEASE);

        permState.setText("应用列表权限：" + (act.hasPackagePermission() ? "已授予" : "未授予（影响残留与安装包识别）")
                + "\n存储权限：" + (act.hasStoragePermission() ? "已授予" : "未授予"));

        Store s = act.store;
        StringBuilder wl = new StringBuilder();
        for (String w : s.whitelist()) {
            if (wl.length() > 0) wl.append('\n');
            wl.append(w);
        }
        if (wlInput != null) wlInput.setText(wl.toString());
        if (rootInput != null) rootInput.setText(s.scanRoot());
        if (bgInput != null) bgInput.setText(s.bgUrl());
        if (bgState != null) bgState.setText(Wallpaper.exists(act) ? "当前已启用壁纸" : "当前为纯色背景");
        if (aiEndpoint != null) aiEndpoint.setText(s.aiEndpoint());
        if (aiKey != null) aiKey.setText(s.aiKey());
        if (aiModel != null) aiModel.setText(s.aiModel());
        if (updUrl != null) updUrl.setText(s.updateUrl());
    }
}
