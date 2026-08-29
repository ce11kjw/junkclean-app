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
    private EditText wlInput, rootInput, bgInput, aiEndpoint, aiKey, aiModel, protInput;
    private LinearLayout protList;
    private TextView rootInfo, bgState, aiState, updState, permState, aboutRuntime;
    private LinearLayout aiBody;

    public SettingsPage(MainActivity a) { super(a); }

    @Override
    public View view() {
        if (scroll != null) return scroll;

        LinearLayout root = UI.col(act);
        int p = Theme.dp(act, Theme.S4);
        root.setPadding(p, Theme.dp(act, Theme.S5), p, p);

        LinearLayout head = UI.col(act);
        head.addView(UI.eyebrow(act, "配置"));
        TextView ht = UI.display(act, "设置", Theme.T_TITLE, Theme.TEXT);
        ht.setTypeface(Theme.display(), android.graphics.Typeface.BOLD);
        head.addView(ht, UI.lpm(act, UI.WC, UI.WC, 2));
        root.addView(head);

        root.addView(UI.section(act, "运行环境"));
        root.addView(envCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "外观"));
        root.addView(themeCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "背景壁纸"));
        root.addView(wallpaperCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "AI 清理建议"));
        root.addView(aiCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "清理行为"));
        root.addView(behaviorCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "扫描分类"));
        root.addView(catCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "白名单"));
        root.addView(whitelistCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "定时清理"));
        root.addView(scheduleCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "远程更新"));
        root.addView(updateCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.section(act, "关于"));
        root.addView(aboutCard(), UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        root.addView(UI.spacer(act, Theme.S8));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        refresh();
        return scroll;
    }

    // ---------- 运行环境 ----------

    private View envCard() {
        LinearLayout c = UI.card(act);
        rootInfo = UI.data(act, "", Theme.T_DATA_S, Theme.MUTED);
        c.addView(rootInfo);
        permState = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
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

        // 主题
        c.addView(UI.eyebrow(act, "主题"));
        LinearLayout tRow = UI.row(act);
        tRow.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
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

        // 强调色
        c.addView(UI.eyebrow(act, "强调色"), UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        LinearLayout aRow = UI.row(act);
        aRow.setPadding(0, Theme.dp(act, 4), 0, Theme.dp(act, 4));
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
        c.addView(aRow);

        // 穿透度
        c.addView(UI.eyebrow(act, "穿透度"), UI.lpm(act, UI.MP, UI.WC, Theme.S4));
        c.addView(UI.note(act, "0% = 实色卡片；100% = 真玻璃（几乎全透，剩边框+羽化+阴影）"),
                UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        LinearLayout opa = UI.glassSlider(act, "玻璃感 0% → 100%（真玻璃）",
                (int) (act.store.glass() * 100),
                new UI.Callback<Integer>() {
            public void call(Integer v) {
                // 拖动中已实时更新 Theme.glass；这里只持久化，不再重建（避免松手闪一下）
                act.store.setGlass(v / 100f);
            }
        });
        c.addView(opa, UI.lpm(act, UI.MP, UI.WC, 0));

        // 模糊带强度
        c.addView(UI.eyebrow(act, "模糊带强度"), UI.lpm(act, UI.MP, UI.WC, Theme.S4));
        LinearLayout blur = UI.featherSegmented(act, act.store.glassBlur(),
                new UI.Callback<Integer>() {
            public void call(Integer v) {
                act.store.setGlassBlur(v);
                act.applyThemeAndRebuild();
            }
        });
        c.addView(blur, UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        return c;
    }
    private View wallpaperCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.note(act, "填图片直链，按屏幕居中裁剪不拉伸；启用后卡片转为半透明玻璃"));

        bgInput = UI.input(act, "https://example.com/bg.jpg", act.store.bgUrl());
        c.addView(bgInput, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 10));

        bgState = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
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
                post(new Runnable() {
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
        aiBody = UI.col(act);
        c.addView(aiBody);
        buildAiBody();
        return c;
    }

    /** 未配置时展示完整表单；配置好后折叠端点与 Key，只留模型可改 */
    private void buildAiBody() {
        aiBody.removeAllViews();
        boolean done = act.store.aiConfigured();

        if (done) {
            aiBody.addView(UI.note(act, "已配置 OpenAI 兼容接口，可在首页使用 AI 建议"));

            LinearLayout info = UI.row(act);
            info.addView(UI.badge(act, "端点已保存", Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)));
            LinearLayout.LayoutParams bp = UI.lp(UI.WC, UI.WC);
            bp.leftMargin = Theme.dp(act, 6);
            info.addView(UI.badge(act, "密钥已保存", Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)), bp);
            aiBody.addView(info, UI.lpm(act, UI.MP, UI.WC, 8));

            aiBody.addView(UI.note(act, "模型 ID"), UI.lpm(act, UI.MP, UI.WC, 10));
            aiModel = UI.input(act, "gpt-4o-mini", act.store.aiModel());
            aiBody.addView(aiModel, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

            aiState = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
            aiBody.addView(aiState, UI.lpm(act, UI.MP, UI.WC, 8));

            Button saveModel = UI.primary(act, "保存模型");
            Button test = UI.secondary(act, "测试");
            Button edit = UI.secondary(act, "修改接口");
            saveModel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (aiModel == null) return;
                    act.store.setAi(act.store.aiEndpoint(), act.store.aiKey(),
                            aiModel.getText().toString().trim());
                    aiState.setText("模型已保存");
                    act.toast("模型已保存");
                }
            });
            test.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { testAi(); }
            });
            edit.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.store.setAiConfigured(false);
                    buildAiBody();
                }
            });
            aiBody.addView(UI.btnRow(act, UI.BTN_H, saveModel, test, edit),
                    UI.lpm(act, UI.MP, UI.WC, 6));
            return;
        }

        aiBody.addView(UI.note(act, "填写 OpenAI 兼容接口，保存后端点与密钥会自动隐藏"));

        aiBody.addView(UI.note(act, "API 端点"), UI.lpm(act, UI.MP, UI.WC, 10));
        aiEndpoint = UI.input(act, "https://api.openai.com/v1", act.store.aiEndpoint());
        aiBody.addView(aiEndpoint, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

        aiBody.addView(UI.note(act, "API Key"), UI.lpm(act, UI.MP, UI.WC, 6));
        aiKey = UI.input(act, "sk-…", act.store.aiKey());
        aiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        aiBody.addView(aiKey, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

        aiBody.addView(UI.note(act, "模型 ID（留空用 gpt-4o-mini）"), UI.lpm(act, UI.MP, UI.WC, 6));
        aiModel = UI.input(act, "gpt-4o-mini", act.store.aiModel());
        aiBody.addView(aiModel, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), 2));

        aiState = UI.data(act, "", Theme.T_DATA_S, Theme.DIM);
        aiBody.addView(aiState, UI.lpm(act, UI.MP, UI.WC, 8));

        Button save = UI.primary(act, "保存");
        Button test = UI.secondary(act, "测试连接");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (aiEndpoint == null) return;
                String ep = aiEndpoint.getText().toString().trim();
                if (ep.isEmpty()) { act.toast("请填写 API 端点"); return; }
                if (!ep.startsWith("http://") && !ep.startsWith("https://")) {
                    act.toast("端点必须以 http(s):// 开头");
                    return;
                }
                saveAi();
                act.store.setAiConfigured(true);
                act.toast("已保存，端点与密钥已隐藏");
                buildAiBody();
            }
        });
        test.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { testAi(); }
        });
        aiBody.addView(UI.btnRow(act, UI.BTN_H, save, test), UI.lpm(act, UI.MP, UI.WC, 6));
    }

    private void saveAi() {
        String ep = aiEndpoint != null ? aiEndpoint.getText().toString().trim()
                                       : act.store.aiEndpoint();
        String key = aiKey != null ? aiKey.getText().toString().trim() : act.store.aiKey();
        String model = aiModel != null ? aiModel.getText().toString().trim() : act.store.aiModel();
        act.store.setAi(ep, key, model);
    }

    private void testAi() {
        saveAi();
        aiState.setText("请求中…");
        new Thread(new Runnable() {
            public void run() {
                final String r = Ai.advise(act.store, "这是连通性测试，请回复「连接正常」。");
                post(new Runnable() {
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
        LinearLayout pRow = UI.row(act);
        pRow.addView(UI.badge(act, "内置保护 " + Store.DEFAULT_PROTECTED.length + " 条",
                Theme.ACCENT, Theme.alpha(Theme.ACCENT, 0x22)));
        Button viewP = UI.chip(act, "查看", false);
        viewP.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                sb.append("以下目录始终受保护，不会被扫描或清理：\n\n");
                for (String p : Store.DEFAULT_PROTECTED) sb.append("· ").append(p).append('\n');
                UI.info(act, "内置保护路径", sb.toString());
            }
        });
        LinearLayout.LayoutParams vp = UI.lp(UI.WC, Theme.dp(act, 26));
        vp.leftMargin = Theme.dp(act, 8);
        pRow.addView(viewP, vp);
        c.addView(pRow, UI.lpm(act, UI.MP, UI.WC, 8));
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

        // 保护路径管理
        c.addView(UI.divider(act));
        c.addView(UI.h2(act, "保护路径（目录，扫描跳过 + 删除拦截）"));
        c.addView(UI.note(act, "支持通配符 *，如 /Android/data/*/cache 匹配所有应用缓存目录。"),
                UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        LinearLayout protAdd = UI.row(act);
        protInput = UI.input(act, "/sdcard/重要目录", "");
        protAdd.addView(protInput, UI.weight(1f, UI.BTN_H, act));
        Button addProt = UI.primary(act, "添加");
        addProt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addProtectedPath(); }
        });
        LinearLayout.LayoutParams app = UI.lp(Theme.dp(act, 56), Theme.dp(act, UI.BTN_H));
        app.leftMargin = Theme.dp(act, Theme.S2);
        protAdd.addView(addProt, app);
        c.addView(protAdd, UI.lpm(act, UI.MP, UI.WC, Theme.S2));

        protList = UI.col(act);
        c.addView(protList, UI.lpm(act, UI.MP, UI.WC, Theme.S1));
        renderProtectedPaths();

        Button resetProt = UI.secondary(act, "恢复默认保护路径");
        resetProt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.confirm(act, "恢复默认", "将保护路径重置为出厂 66 条？", new Runnable() {
                    public void run() { act.store.resetProtected(); renderProtectedPaths(); }
                });
            }
        });
        c.addView(resetProt, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));

        // 规则库管理（RuleEngine）
        c.addView(UI.divider(act));
        c.addView(UI.h2(act, "清理规则库"));
        c.addView(UI.note(act, "规则库控制「规则清理」分类扫什么。文件位于 "
                + Util.shortPath(RuleEngine.rulesPath()) + "，可用文件管理器编辑。"),
                UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        Button viewRules = UI.secondary(act, "查看规则");
        Button resetRules = UI.secondary(act, "恢复默认规则");
        viewRules.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                java.util.List<RuleEngine.Rule> rs = RuleEngine.load(act);
                StringBuilder sb = new StringBuilder();
                sb.append("共 ").append(rs.size()).append(" 条规则：\n\n");
                for (RuleEngine.Rule r : rs) {
                    sb.append(r.enabled ? "✓ " : "○ ").append(r.label)
                      .append("  [").append(r.risk).append("]\n");
                }
                UI.info(act, "清理规则库", sb.toString());
            }
        });
        resetRules.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UI.confirm(act, "恢复默认规则",
                        "将规则库重置为内置默认（覆盖你的自定义修改）？", new Runnable() {
                    public void run() {
                        if (RuleEngine.writeDefault()) act.toast("规则已重置");
                        else act.toast("写入失败，检查存储权限");
                    }
                });
            }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, viewRules, resetRules), UI.lpm(act, UI.MP, UI.WC, Theme.S2));
        return c;
    }

    private void addProtectedPath() {
        String path = protInput.getText().toString().trim();
        if (path.isEmpty()) { act.toast("请输入目录名或路径"); return; }
        act.store.addProtected(path);
        protInput.setText("");
        renderProtectedPaths();
        act.toast("已添加保护：" + path);
    }

    private void renderProtectedPaths() {
        protList.removeAllViews();
        List<String> list = act.store.protectedPaths();
        if (list.isEmpty()) {
            protList.addView(UI.note(act, "暂无保护路径"));
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            LinearLayout row = UI.row(act);
            row.setPadding(0, Theme.dp(act, 3), 0, Theme.dp(act, 3));
            TextView nm = UI.data(act, list.get(i), Theme.T_DATA_S, Theme.MUTED);
            nm.setSingleLine(true);
            nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            row.addView(nm, new LinearLayout.LayoutParams(0, UI.WC, 1f));
            Button del = UI.danger(act, "×");
            del.setTextSize(13);
            LinearLayout.LayoutParams dp = UI.lp(Theme.dp(act, 32), Theme.dp(act, 26));
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.store.removeProtected(idx);
                    renderProtectedPaths();
                }
            });
            row.addView(del, dp);
            protList.addView(row);
        }
    }

    // ---------- 远程更新 ----------

    private View updateCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "版本"));
        c.addView(UI.data(act, "v" + MainActivity.VERSION, Theme.T_TITLE, Theme.TEXT), UI.lpm(act, UI.MP, UI.WC, 2));

        updState = UI.data(act, "点击检查是否有新版本", Theme.T_DATA_S, Theme.DIM);
        c.addView(updState, UI.lpm(act, UI.MP, UI.WC, 8));

        Button check = UI.primary(act, "检查更新");
        Button src = UI.secondary(act, "更新源");
        check.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { checkUpdate(); }
        });
        src.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { editUpdateSource(); }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, check, src), UI.lpm(act, UI.MP, UI.WC, 6));
        return c;
    }

    /** 更新源默认指向本项目 Release，一般用户不需要改，因此收进对话框 */
    private void editUpdateSource() {
        UI.prompt(act, "更新源地址",
                "GitHub Release API 或自定义 JSON",
                act.store.updateUrl(), 1,
                new UI.Callback<String>() {
            public void call(String v) {
                String u = v.trim();
                if (u.isEmpty()) {
                    act.store.setUpdateUrl("");
                    act.toast("已恢复默认更新源");
                } else if (!u.startsWith("http://") && !u.startsWith("https://")) {
                    act.toast("地址必须以 http(s):// 开头");
                } else {
                    act.store.setUpdateUrl(u);
                    act.toast("更新源已保存");
                }
            }
        });
    }

    private void checkUpdate() {
        updState.setText("检查中…");
        new Thread(new Runnable() {
            public void run() {
                final Updater.Info info = Updater.check(act.store);
                post(new Runnable() {
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
                        showUpdateDialog(info);
                    }
                });
            }
        }).start();
    }

    /** 下载 → 展示进度 → 自动调起安装 */
    private void doUpdate(final Updater.Info info) {
        if (info.apkUrl == null || info.apkUrl.isEmpty()) {
            updState.setText("更新源中没有 APK 下载地址");
            act.toast("该更新源未提供安装包");
            return;
        }
        final Object[] dlg = UI.progress(act, "下载更新 v" + info.version, "准备中…");
        final android.app.Dialog d = (android.app.Dialog) dlg[0];
        final TextView msg = (TextView) dlg[1];
        final SegmentGauge bar = (SegmentGauge) dlg[2];

        new Thread(new Runnable() {
            public void run() {
                final java.io.File f = Updater.download(act, info.apkUrl, new Net.Progress() {
                    public void onProgress(final int done, final int total) {
                        post(new Runnable() {
                            public void run() {
                                msg.setText(Util.fmtSize(done) + " / " + Util.fmtSize(total));
                                bar.setPercent(total > 0 ? done * 100f / total : 0);
                            }
                        });
                    }
                });
                post(new Runnable() {
                    public void run() {
                        d.dismiss();
                        if (f == null) {
                            updState.setText("下载失败：" + Updater.lastError);
                            UI.confirm(act, "下载失败",
                                    "原因：" + Updater.lastError
                                    + "\n\n可稍后重试，或到 GitHub 手动下载安装包。",
                                    null);
                            return;
                        }
                        updState.setText("已下载：" + Util.shortPath(f.getAbsolutePath()));
                        boolean ok = Updater.install(act, f);
                        if (!ok) {
                            UI.confirm(act, "无法自动安装",
                                    "安装包已保存到：\n" + Updater.publicApkPath()
                                    + "\n\n请用文件管理器打开手动安装。"
                                    + "\n若系统提示禁止安装未知应用，需在设置中允许本应用安装。",
                                    null);
                        }
                    }
                });
            }
        }).start();
    }


    /**
     * 新版本对话框：三个按钮，下载按钮点了才走流量。
     */
    private void showUpdateDialog(final Updater.Info info) {
        Object[] parts = UI.glassDialog(act, "发现新版本 v" + info.version);
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        if (info.notes != null && !info.notes.isEmpty()) {
            String notes = info.notes;
            if (notes.length() > 1200) notes = notes.substring(0, 1200) + "…";
            TextView m = UI.data(act, notes, Theme.T_DATA_S, Theme.MUTED);
            m.setLineSpacing(0, 1.4f);
            android.widget.ScrollView sv = new android.widget.ScrollView(act);
            sv.setVerticalScrollBarEnabled(false);
            sv.addView(m);
            int maxH = (int) (act.getResources().getDisplayMetrics().heightPixels * 0.4f);
            body.addView(sv, new LinearLayout.LayoutParams(UI.MP, maxH));
        }
        if (info.apkUrl != null && !info.apkUrl.isEmpty()) {
            TextView url = UI.data(act, info.apkUrl, Theme.T_DATA_S, Theme.DIM);
            url.setPadding(0, Theme.dp(act, Theme.S3), 0, 0);
            url.setSingleLine(true);
            url.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            body.addView(url, UI.lpm(act, UI.MP, UI.WC, 0));
        }

        Button download = UI.primary(act, "下载并安装");
        Button copyUrl = UI.secondary(act, "复制下载链接");
        Button later = UI.secondary(act, "稍后再说");
        download.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); doUpdate(info); }
        });
        copyUrl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (info.apkUrl == null || info.apkUrl.isEmpty()) { return; }
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        act.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText(
                        "JunkClean update", info.apkUrl));
                act.toast("下载链接已复制");
            }
        });
        later.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        wrap.addView(UI.btnRow(act, UI.BTN_H, later, copyUrl), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        wrap.addView(download, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H_MAIN), Theme.S3));
        d.show();
    }

    // ---------- 定时清理 ----------

    private View scheduleCard() {
        LinearLayout c = UI.card(act);
        c.addView(UI.eyebrow(act, "定时清理"));
        c.addView(UI.title(act, "自动清理间隔"), UI.lpm(act, UI.MP, UI.WC, 2));
        c.addView(UI.note(act, "到点自动扫描并清理（不影响手动操作），重启手机后自动恢复"),
                UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        // 总开关
        c.addView(UI.switchRow(act, "启用定时清理", "默认每 30 分钟一次，启动不立即跑",
                act.store.scheduleEnabled(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                act.store.setScheduleEnabled(on);
                ScheduleManager.apply(act, act.store);
            }
        }));

        // 间隔选择
        c.addView(UI.eyebrow(act, "间隔"), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        final int[] opts = {15, 30, 60, 120, 240};
        final String[] labels = {"15 分钟", "30 分钟", "1 小时", "2 小时", "4 小时"};
        LinearLayout row = UI.row(act);
        int cur = 0;
        for (int i = 0; i < opts.length; i++) if (opts[i] == act.store.scheduleIntervalMin()) cur = i;
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            Button b = UI.chip(act, labels[i], i == cur);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    act.store.setScheduleIntervalMin(opts[idx]);
                    ScheduleManager.apply(act, act.store);
                }
            });
            LinearLayout.LayoutParams lp = UI.lp(UI.WC, Theme.dp(act, 26));
            lp.rightMargin = Theme.dp(act, Theme.S1);
            row.addView(b, lp);
        }
        c.addView(row);

        // 条件
        c.addView(UI.eyebrow(act, "触发条件"), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        c.addView(UI.switchRow(act, "仅在充电时", "避免低电量时跑", act.store.scheduleOnlyCharging(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                act.store.setScheduleOnlyCharging(on);
            }
        }));
        c.addView(UI.switchRow(act, "仅在 Wi-Fi 时", "避免移动数据流量", act.store.scheduleOnlyWifi(),
                new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                act.store.setScheduleOnlyWifi(on);
            }
        }));

        // 下次时间 + 立即跑一次
        c.addView(UI.eyebrow(act, "状态"), UI.lpm(act, UI.MP, UI.WC, Theme.S3));
        TextView next = UI.data(act, nextScheduleText(), Theme.T_DATA_S, Theme.MUTED);
        c.addView(next, UI.lpm(act, UI.MP, UI.WC, Theme.S1));

        Button runNow = UI.secondary(act, "立即跑一次");
        runNow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                act.toast("已触发清理");
                CleanReceiver.runOnce(act);
            }
        });
        c.addView(runNow, UI.lpm(act, UI.MP, Theme.dp(act, UI.BTN_H), Theme.S2));
        return c;
    }

    private String nextScheduleText() {
        if (!act.store.scheduleEnabled()) return "未启用";
        long next = act.store.scheduleNextAt();
        if (next <= 0) return "已调度";
        long delta = (next - System.currentTimeMillis()) / 60_000;
        if (delta < 0) return "即将执行（< 1 分钟）";
        return "下次：" + delta + " 分钟后";
    }

    /**
     * AndroidManifest 里也注册这两个 receiver（在 v3.0.6 通过 patch 加）
     */

        // ---------- 关于 ----------

    private View aboutCard() {
        LinearLayout c = UI.card(act);

        TextView name = UI.text(act, "JunkClean", 18, Theme.TEXT);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.addView(name);
        c.addView(UI.text(act, "v" + MainActivity.VERSION + " · Android 垃圾清理工具",
                12, Theme.ACCENT), UI.lpm(act, UI.MP, UI.WC, 2));

        c.addView(UI.divider(act));

        c.addView(UI.h2(act, "功能"));
        c.addView(UI.note(act,
                "· 9 类垃圾扫描，支持 root 全盘模式\n"
              + "· 大文件 / 重复文件 / 空文件 / 安装包专项\n"
              + "· 文件清理：浏览目录手动删除任意文件\n"
              + "· 整理中心：按类型归档，可预览与还原\n"
              + "· 回收站：误删可恢复，支持保留期\n"
              + "· AI 清理建议（OpenAI 兼容接口）\n"
              + "· 清理统计与 7 天趋势"), UI.lpm(act, UI.MP, UI.WC, 4));

        c.addView(UI.h2(act, "安全设计"), UI.lpm(act, UI.MP, UI.WC, 12));
        c.addView(UI.note(act,
                "· 内置 " + Store.DEFAULT_PROTECTED.length + " 条保护路径，覆盖相册、聊天记录、备份、密钥等\n"
              + "· 路径白名单机制，拒绝 .. 遍历与系统根目录操作\n"
              + "· 只删除已勾选项，删除前二次确认\n"
              + "· 谨慎分类默认不勾选"), UI.lpm(act, UI.MP, UI.WC, 4));

        c.addView(UI.h2(act, "技术"), UI.lpm(act, UI.MP, UI.WC, 12));
        c.addView(UI.note(act,
                "· 纯原生 Java，无第三方依赖，无 Gradle\n"
              + "· 界面代码构建，自绘存储条与统计图\n"
              + "· minSdk 26 / targetSdk 34\n"
              + "· 有 root 深度清理，无 root 自动降级"), UI.lpm(act, UI.MP, UI.WC, 4));

        c.addView(UI.h2(act, "运行状态"), UI.lpm(act, UI.MP, UI.WC, 12));
        aboutRuntime = UI.data(act, "", Theme.T_DATA_S, Theme.MUTED);
        c.addView(aboutRuntime, UI.lpm(act, UI.MP, UI.WC, 4));

        c.addView(UI.divider(act));

        c.addView(UI.h2(act, "项目地址"));
        c.addView(UI.note(act,
                "App：github.com/ce11kjw/junkclean-app\n"
              + "模块：github.com/ce11kjw/junkclean\n"
              + "许可：MIT"), UI.lpm(act, UI.MP, UI.WC, 4));

        Button copyInfo = UI.secondary(act, "复制诊断信息");
        Button openRepo = UI.secondary(act, "打开项目主页");
        copyInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { copyDiagnostics(); }
        });
        openRepo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    android.content.Intent i = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/ce11kjw/junkclean-app"));
                    i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(i);
                } catch (Exception e) {
                    act.toast("没有可用的浏览器");
                }
            }
        });
        c.addView(UI.btnRow(act, UI.BTN_H, copyInfo, openRepo), UI.lpm(act, UI.MP, UI.WC, 12));
        return c;
    }

    private void copyDiagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("JunkClean v").append(MainActivity.VERSION).append('\n');
        sb.append("设备：").append(android.os.Build.MANUFACTURER).append(' ')
          .append(android.os.Build.MODEL).append('\n');
        sb.append("系统：Android ").append(android.os.Build.VERSION.RELEASE)
          .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
        sb.append("root：").append(Shell.hasRoot() ? Shell.detectManager() : "无").append('\n');
        sb.append("存储权限：").append(act.hasStoragePermission() ? "已授予" : "未授予").append('\n');
        sb.append("应用列表权限：").append(act.hasPackagePermission() ? "已授予" : "未授予").append('\n');
        sb.append("全盘扫描：").append(act.store.fullScan() ? "开启" : "关闭").append('\n');
        sb.append("主题：").append(act.store.theme()).append(" / ").append(act.store.accent()).append('\n');
        sb.append("壁纸：").append(Wallpaper.exists(act) ? "已启用" : "未启用").append('\n');
        sb.append("累计清理：").append(act.store.totalCount()).append(" 项 / ")
          .append(Util.fmtSize(act.store.totalFreed()));
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                act.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("JunkClean", sb.toString()));
        act.toast("诊断信息已复制");
    }

    // ---------- 刷新 ----------

    public void refresh() {
        if (rootInfo == null) return;   // 视图尚未构建
        Store s = act.store;
        boolean root = Shell.hasRoot();
        rootInfo.setText((root ? "✓ 已获得 root（" + Shell.detectManager() + "）" : "⚠ 未检测到 root")
                + "\n清理模式：" + (root ? "深度（全应用缓存 + 系统日志）"
                                       : "受限（公共目录 + 外部缓存 + 自身缓存）")
                + "\n设备：" + android.os.Build.MODEL
                + " · Android " + android.os.Build.VERSION.RELEASE);

        if (aboutRuntime != null) {
            aboutRuntime.setText("root：" + (Shell.hasRoot() ? Shell.detectManager() : "未获得")
                    + "\n设备：" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                    + "\n系统：Android " + android.os.Build.VERSION.RELEASE
                    + "（API " + android.os.Build.VERSION.SDK_INT + "）"
                    + "\n累计清理：" + s.totalCount() + " 项 / " + Util.fmtSize(s.totalFreed()));
        }
        permState.setText("应用列表权限：" + (act.hasPackagePermission() ? "已授予" : "未授予（影响残留与安装包识别）")
                + "\n存储权限：" + (act.hasStoragePermission() ? "已授予" : "未授予"));
        StringBuilder wl = new StringBuilder();
        for (String w : s.userWhitelist()) {
            if (wl.length() > 0) wl.append('\n');
            wl.append(w);
        }
        if (wlInput != null) wlInput.setText(wl.toString());
        if (rootInput != null) rootInput.setText(s.scanRoot());
        if (bgInput != null) bgInput.setText(s.bgUrl());
        if (bgState != null) bgState.setText(Wallpaper.exists(act) ? "当前已启用壁纸" : "当前为纯色背景");
        // AI 表单由 buildAiBody 自行填充，这里不覆盖，避免折叠态下空指针
    }
}
