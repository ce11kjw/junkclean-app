package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 视图工厂。所有容器走「双层机加工外壳」：外壳是铝合金托盘，
 * 内芯是嵌进去的玻璃面板，同心圆角由外半径减内边距算出。
 */
public final class UI {
    private UI() {}

    public static final int MP = LinearLayout.LayoutParams.MATCH_PARENT;
    public static final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;

    public static final int BTN_H = 38;
    public static final int BTN_H_MAIN = 48;

    // ---------- 布局 ----------

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /**
     * 双层卡片。返回外壳，内容加进 core()。
     * 直接把内容塞进外壳会失去嵌入感，必须走内芯。
     */
    public static LinearLayout card(Context c) {
        return new Card(c);
    }

    /** 取出卡片内芯（一般不需要，Card 会自动转发 addView） */
    public static LinearLayout core(LinearLayout card) {
        return card instanceof Card ? ((Card) card).core() : card;
    }

    // ---------- 文字 ----------

    public static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Theme.body());
        return t;
    }

    /** 大号读数：细体 + 负字距 */
    public static TextView display(Context c, String s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(Theme.display());
        t.setLetterSpacing(-0.025f);
        return t;
    }

    /** 等宽数据：字节数 / 百分比 / 路径 / 计数 */
    public static TextView data(Context c, String s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(Theme.data());
        return t;
    }

    public static TextView title(Context c, String s) {
        TextView t = text(c, s, Theme.T_HEAD, Theme.TEXT);
        t.setTypeface(Theme.display(), Typeface.BOLD);
        t.setLetterSpacing(-0.015f);
        return t;
    }

    /** 眉标：微号 + 大字距 + 全大写 */
    public static TextView eyebrow(Context c, String s) {
        TextView t = text(c, s, Theme.T_MICRO, Theme.DIM);
        t.setTypeface(Theme.micro());
        t.setAllCaps(true);
        t.setLetterSpacing(0.2f);
        return t;
    }

    public static TextView h2(Context c, String s) {
        TextView t = text(c, s, Theme.T_BODY_S, Theme.MUTED);
        t.setTypeface(Theme.micro());
        return t;
    }

    public static TextView note(Context c, String s) {
        TextView t = text(c, s, Theme.T_BODY_S, Theme.DIM);
        t.setLineSpacing(0, 1.45f);
        return t;
    }

    /** 分区标题：眉标风格，卡片之间的结构分隔 */
    public static View section(Context c, String s) {
        LinearLayout r = row(c);
        r.setPadding(Theme.dp(c, 6), Theme.dp(c, Theme.S5), 0, Theme.dp(c, Theme.S2));
        TextView t = eyebrow(c, s);
        t.setTextColor(Theme.ACCENT);
        r.addView(t);
        View line = new View(c);
        line.setBackgroundColor(Theme.HAIRLINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Math.max(1, Theme.dp(c, 0.8f)), 1f);
        lp.leftMargin = Theme.dp(c, Theme.S3);
        r.addView(line, lp);
        return r;
    }

/**
 * 富空状态：图标 + 主文案 + 副文案 + 可选操作按钮。
 * 用法：UI.emptyState(c, "🗂", "没有发现重复文件",
 *                       "试试放宽感知哈希阈值", "调整阈值", () -> {…});
 */
public static LinearLayout emptyState(Context c, String icon, String title,
                                       String subtitle, String actionLabel, final Runnable action) {
    LinearLayout col = col(c);
    col.setGravity(Gravity.CENTER);
    int ph = Theme.dp(c, Theme.S5);
    col.setPadding(0, ph, 0, ph);
    TextView iconT = text(c, icon, 48, Theme.DIM);
    iconT.setGravity(Gravity.CENTER);
    col.addView(iconT);
    col.addView(UI.spacer(c, Theme.S2));
    TextView titleT = text(c, title, Theme.T_HEAD, Theme.MUTED);
    titleT.setGravity(Gravity.CENTER);
    col.addView(titleT);
    if (subtitle != null && !subtitle.isEmpty()) {
        col.addView(UI.spacer(c, Theme.S1));
        TextView subT = text(c, subtitle, Theme.T_BODY_S, Theme.DIM);
        subT.setGravity(Gravity.CENTER);
        col.addView(subT);
    }
    if (actionLabel != null && action != null) {
        col.addView(UI.spacer(c, Theme.S3));
        Button b = secondary(c, actionLabel);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
        LinearLayout.LayoutParams bp = UI.lp(UI.WC, Theme.dp(c, UI.BTN_H));
        col.addView(b, bp);
    }
    return col;
}

/** 保留旧版简单空状态（兼容） */
public static TextView empty(Context c, String s) {
    TextView t = text(c, s, Theme.T_BODY_S, Theme.DIM);
    t.setGravity(Gravity.CENTER);
    t.setLineSpacing(0, 1.6f);
    t.setPadding(0, Theme.dp(c, Theme.S5), 0, Theme.dp(c, Theme.S5));
    return t;
}

    // ---------- 按钮 ----------

    private static Button baseBtn(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(Theme.T_BODY_S);
        b.setTypeface(Theme.micro());
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setPadding(0, 0, 0, 0);
        b.setLetterSpacing(0.02f);
        Anim.pressable(b);
        return b;
    }

    public static Button primary(Context c, String s) {
        Button b = baseBtn(c, s);
        b.setTextColor(Theme.light ? 0xFF06231B : 0xFF04140F);
        b.setBackground(Theme.primaryBtn(c));
        // 给按钮足够的水平 padding，让渐变填充和圆角都能完整呈现
        int ph = Theme.dp(c, 14);
        b.setPadding(ph, b.getPaddingTop(), ph, b.getPaddingBottom());
        return b;
    }

    public static Button danger(Context c, String s) {
        Button b = baseBtn(c, s);
        b.setTextColor(0xFFFFFFFF);
        b.setBackground(Theme.dangerBtn(c));
        b.setPadding(Theme.dp(c, 14), b.getPaddingTop(), Theme.dp(c, 14), b.getPaddingBottom());
        return b;
    }

    public static Button secondary(Context c, String s) {
        Button b = baseBtn(c, s);
        b.setTextColor(Theme.MUTED);
        b.setBackground(Theme.ghostBtn(c));
        b.setPadding(Theme.dp(c, 14), b.getPaddingTop(), Theme.dp(c, 14), b.getPaddingBottom());
        return b;
    }

    /**
     * 主行动按钮：文字居左，箭头包在自己的圆形容器里贴齐右内边距。
     * 按下时整体缩放，内圈同时向右上位移，制造内部动能。
     */
    public static LinearLayout actionButton(final Context c, String label, String glyph,
                                            final Runnable onClick) {
        final LinearLayout wrap = row(c);
        wrap.setBackground(Theme.primaryBtn(c));
        wrap.setGravity(Gravity.CENTER_VERTICAL);
        int ph = Theme.dp(c, 20);
        wrap.setPadding(ph, Theme.dp(c, 6), Theme.dp(c, 6), Theme.dp(c, 6));

        TextView t = text(c, label, Theme.T_BODY, Theme.light ? 0xFF06231B : 0xFF04140F);
        t.setTypeface(Theme.micro());
        t.setLetterSpacing(0.03f);
        wrap.addView(t, new LinearLayout.LayoutParams(0, WC, 1f));

        final TextView well = text(c, glyph, Theme.T_BODY, Theme.light ? 0xFF06231B : 0xFF04140F);
        well.setBackground(Theme.iconWell(c, true));
        well.setGravity(Gravity.CENTER);
        int s = Theme.dp(c, 32);
        wrap.addView(well, new LinearLayout.LayoutParams(s, s));

        wrap.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        wrap.animate().scaleX(0.975f).scaleY(0.975f)
                                .setDuration(110).setInterpolator(Theme.press()).start();
                        well.animate().translationX(Theme.dp(c, 2))
                                .translationY(-Theme.dp(c, 1)).scaleX(1.06f).scaleY(1.06f)
                                .setDuration(180).setInterpolator(Theme.press()).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        wrap.animate().scaleX(1f).scaleY(1f)
                                .setDuration(240).setInterpolator(Theme.spring()).start();
                        well.animate().translationX(0).translationY(0).scaleX(1f).scaleY(1f)
                                .setDuration(300).setInterpolator(Theme.spring()).start();
                        break;
                }
                return false;
            }
        });
        wrap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (onClick != null) onClick.run(); }
        });
        return wrap;
    }

    public static Button chip(Context c, String s, boolean active) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(Theme.T_MICRO + 1.5f);
        b.setTypeface(Theme.micro());
        b.setAllCaps(false);
        b.setPadding(Theme.dp(c, 12), 0, Theme.dp(c, 12), 0);
        b.setMinWidth(0); b.setMinimumWidth(0);
        b.setMinHeight(Theme.dp(c, 28)); b.setMinimumHeight(Theme.dp(c, 28));
        b.setStateListAnimator(null);
        b.setLetterSpacing(0.04f);
        Anim.pressable(b, 0.94f);
        setChipActive(c, b, active);
        return b;
    }

    public static void setChipActive(Context c, Button b, boolean active) {
        if (active) {
            b.setBackground(Theme.primaryBtn(c));
            b.setTextColor(Theme.light ? 0xFF06231B : 0xFF04140F);
        } else {
            b.setBackground(Theme.ghostBtn(c));
            // 玻璃模式下未选中文字用半透明，让玻璃质感贯穿
            b.setTextColor(Theme.glass > 0f ? Theme.alpha(Theme.TEXT, 0xB4) : Theme.MUTED);
        }
    }

    public static TextView badge(Context c, String s, int fg, int bg) {
        TextView t = text(c, s, Theme.T_MICRO, fg);
        t.setTypeface(Theme.micro());
        t.setAllCaps(true);
        t.setLetterSpacing(0.1f);
        t.setBackground(Theme.badge(c, bg));
        t.setPadding(Theme.dp(c, 8), Theme.dp(c, 3), Theme.dp(c, 8), Theme.dp(c, 3));
        return t;
    }

    public static CheckBox check(Context c, boolean on) {
        CheckBox cb = new CheckBox(c);
        cb.setChecked(on);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Theme.ACCENT));
        cb.setPadding(0, 0, 0, 0);
        cb.setMinWidth(0); cb.setMinimumWidth(0);
        return cb;
    }

    public static android.widget.Switch switchView(Context c, boolean on) {
        android.widget.Switch sw = new android.widget.Switch(c);
        sw.setChecked(on);
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(
                on ? Theme.ACCENT : Theme.MUTED));
        sw.setTrackTintList(android.content.res.ColorStateList.valueOf(
                Theme.alpha(on ? Theme.ACCENT : 0xFFFFFF, 0x40)));
        return sw;
    }

    public static android.widget.Switch smallSwitch(Context c, boolean on) {
        android.widget.Switch sw = switchView(c, on);
        sw.setScaleX(0.85f);
        sw.setScaleY(0.85f);
        return sw;
    }

    /**
     * 玻璃感强度滑杆 0~100。onChange 触发后通常需要 applyThemeAndRebuild() 重建。
     */
    public static LinearLayout glassSlider(final Context c, String label,
                                          int initial,
                                          final UI.Callback<Integer> onChange) {
        LinearLayout col = col(c);
        col.addView(eyebrow(c, label));
        LinearLayout row = row(c);
        row.setPadding(0, Theme.dp(c, 2), 0, Theme.dp(c, 2));
        android.widget.SeekBar bar = new android.widget.SeekBar(c);
        bar.setMax(100);
        bar.setProgress(initial);
        final android.widget.TextView val = data(c, initial + "%", Theme.T_DATA_S, Theme.ACCENT);
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar b, int p, boolean u) {
                val.setText(p + "%");
            }
            public void onStartTrackingTouch(android.widget.SeekBar b) {}
            public void onStopTrackingTouch(android.widget.SeekBar b) {
                if (onChange != null) onChange.call(b.getProgress());
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, Theme.dp(c, 30), 1f);
        sp.rightMargin = Theme.dp(c, Theme.S3);
        row.addView(bar, sp);
        row.addView(val);
        col.addView(row);
        return col;
    }

    /**
     * 边缘羽化半径 4 档
     */
    public static LinearLayout featherSegmented(final Context c, int current,
                                              final UI.Callback<Integer> onChange) {
        LinearLayout col = col(c);
        col.addView(eyebrow(c, "边缘羽化"));
        LinearLayout row = row(c);
        row.setPadding(0, Theme.dp(c, 2), 0, Theme.dp(c, 2));
        String[] labels = {"硬边 0dp", "微 4dp", "默认 8dp", "强 16dp"};
        int[] vals = {0, 4, 8, 16};
        int cur = 0;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) cur = i;
        for (int i = 0; i < vals.length; i++) {
            final int idx = i;
            Button b = chip(c, labels[i], i == cur);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (onChange != null) onChange.call(vals[idx]);
                }
            });
            Anim.pressable(b, 0.94f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            if (i > 0) lp.leftMargin = Theme.dp(c, Theme.S1);
            row.addView(b, lp);
        }
        col.addView(row);
        return col;
    }

    /** 开关行：标题 + 说明 + 右侧开关，颜色随状态过渡 */
    public static LinearLayout switchRow(final Context c, String title, String desc, boolean on,
                                        final android.widget.CompoundButton.OnCheckedChangeListener cb) {
        LinearLayout r = row(c);
        r.setPadding(0, Theme.dp(c, 7), 0, Theme.dp(c, 7));
        LinearLayout info = col(c);
        info.addView(text(c, title, Theme.T_BODY, Theme.TEXT));
        if (desc != null && !desc.isEmpty()) info.addView(note(c, desc));
        r.addView(info, new LinearLayout.LayoutParams(0, WC, 1f));

        final android.widget.Switch sw = switchView(c, on);
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean checked) {
                sw.setThumbTintList(android.content.res.ColorStateList.valueOf(
                        checked ? Theme.ACCENT : Theme.MUTED));
                sw.setTrackTintList(android.content.res.ColorStateList.valueOf(
                        Theme.alpha(checked ? Theme.ACCENT : 0xFFFFFF, 0x40)));
                if (cb != null) cb.onCheckedChanged(v, checked);
            }
        });
        r.addView(sw);
        return r;
    }

    // ---------- 输入 ----------

    public static EditText input(Context c, String hint, String value) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextSize(Theme.T_DATA_S);
        e.setTypeface(Theme.data());
        e.setTextColor(Theme.TEXT);
        e.setHintTextColor(Theme.DIM);
        e.setBackground(Theme.item(c, false));
        int p = Theme.dp(c, 12);
        e.setPadding(p, p - Theme.dp(c, 2), p, p - Theme.dp(c, 2));
        e.setSingleLine(true);
        e.setEllipsize(TextUtils.TruncateAt.END);
        return e;
    }

    public static EditText multiline(Context c, String hint, String value, int minLines) {
        EditText e = input(c, hint, value);
        e.setSingleLine(false);
        e.setMinLines(minLines);
        e.setGravity(Gravity.TOP | Gravity.START);
        return e;
    }

    /** 搜索框：带前缀符号 */
    public static EditText search(Context c, String hint) {
        EditText e = input(c, hint, "");
        e.setTypeface(Theme.body());
        e.setTextSize(Theme.T_BODY_S);
        return e;
    }

    // ---------- 间隔 ----------

    public static View spacer(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, Theme.dp(c, dp)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Theme.HAIRLINE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MP, Math.max(1, Theme.dp(c, 0.8f)));
        p.topMargin = p.bottomMargin = Theme.dp(c, Theme.S3);
        v.setLayoutParams(p);
        return v;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpm(Context c, int w, int h, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = Theme.dp(c, topDp);
        return p;
    }

    public static LinearLayout.LayoutParams weight(float w, int hDp, Context c) {
        return new LinearLayout.LayoutParams(0, Theme.dp(c, hDp), w);
    }

    public static LinearLayout btnRow(Context c, int heightDp, Button... bs) {
        LinearLayout r = row(c);
        for (int i = 0; i < bs.length; i++) {
            LinearLayout.LayoutParams p = weight(1f, heightDp, c);
            if (i > 0) p.leftMargin = Theme.dp(c, Theme.S2);
            r.addView(bs[i], p);
        }
        return r;
    }

    /** 文件项行：勾选 + 名称（等宽体积）+ 长按进度环 */
    public static LinearLayout fileRow(final Context c, final JunkItem it,
                                       final Runnable onChange, final Runnable onLongPress) {
        LinearLayout r = row(c);
        int pv = Theme.dp(c, 7);
        r.setPadding(Theme.dp(c, 6), pv, Theme.dp(c, 6), pv);

        final CheckBox cb = check(c, it.checked);
        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                it.checked = on;
                if (on) Anim.tick(v);
                if (onChange != null) onChange.run();
            }
        });
        r.addView(cb);

        TextView nm = text(c, it.name, Theme.T_BODY_S, Theme.MUTED);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, WC, 1f);
        np.leftMargin = Theme.dp(c, Theme.S2);
        np.rightMargin = Theme.dp(c, Theme.S2);
        r.addView(nm, np);

        r.addView(data(c, Util.fmtSize(it.size), Theme.T_DATA_S, Theme.DIM));

        if (onLongPress != null) {
            final LongPressRing ring = new LongPressRing(c);
            ring.setOnComplete(onLongPress);
            LinearLayout.LayoutParams rp = lp(WC, WC);
            rp.leftMargin = Theme.dp(c, Theme.S2);
            r.addView(ring, rp);
            // 落点在勾选框上时不启动长按环，否则勾选和加白名单会互相干扰
            final CheckBox cbRef = cb;
            r.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, android.view.MotionEvent e) {
                    if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN
                            && e.getX() <= cbRef.getRight() + Theme.dp(c, 6)) {
                        return false;
                    }
                    ring.handle(e);
                    return false;
                }
            });
        }
        return r;
    }

    // ================= 玻璃对话框 =================

    public static Object[] glassDialog(Context c, String title) {
        final android.app.Dialog d = new android.app.Dialog(c);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout wrap = col(c);
        wrap.setBackground(Theme.dialog(c));
        int p = Theme.dp(c, 22);
        wrap.setPadding(p, Theme.dp(c, 20), p, Theme.dp(c, 16));

        if (title != null && !title.isEmpty()) {
            TextView t = display(c, title, Theme.T_HEAD, Theme.TEXT);
            t.setTypeface(Theme.display(), Typeface.BOLD);
            wrap.addView(t);
        }

        LinearLayout body = col(c);
        wrap.addView(body, lpm(c, MP, WC, title == null ? 0 : Theme.S3));

        d.setContentView(wrap);
        android.view.Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0x00000000));
            w.setDimAmount(0.62f);
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = (int) (c.getResources().getDisplayMetrics().widthPixels * 0.88f);
            w.setAttributes(lp);
        }
        Anim.dialogIn(wrap);
        return new Object[]{d, body, wrap};
    }

    private static void dialogButtons(Context c, LinearLayout wrap, final android.app.Dialog d,
                                      String okText, final Runnable onOk, String cancelText) {
        Button ok = primary(c, okText);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                d.dismiss();
                if (onOk != null) onOk.run();
            }
        });
        if (cancelText == null) {
            wrap.addView(btnRow(c, BTN_H, ok), lpm(c, MP, WC, Theme.S4));
            return;
        }
        Button cancel = secondary(c, cancelText);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        wrap.addView(btnRow(c, BTN_H, cancel, ok), lpm(c, MP, WC, Theme.S4));
    }

    public static void confirm(Context c, String title, String msg, final Runnable onOk) {
        Object[] parts = glassDialog(c, title);
        android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];
        TextView m = text(c, msg, Theme.T_BODY_S, Theme.MUTED);
        m.setLineSpacing(0, 1.5f);
        body.addView(m);
        dialogButtons(c, wrap, d, "确定", onOk, "取消");
        d.show();
    }

    public static void pick(Context c, String title, final String[] labels, final int checked,
                            final android.content.DialogInterface.OnClickListener cb) {
        Object[] parts = glassDialog(c, title);
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            LinearLayout r = row(c);
            r.setBackground(Theme.item(c, false));
            int pd = Theme.dp(c, 14);
            r.setPadding(pd, Theme.dp(c, 12), pd, Theme.dp(c, 12));
            TextView t = text(c, labels[i], Theme.T_BODY,
                    idx == checked ? Theme.ACCENT : Theme.TEXT);
            r.addView(t, new LinearLayout.LayoutParams(0, WC, 1f));
            if (idx == checked) r.addView(text(c, "✓", Theme.T_BODY, Theme.ACCENT));
            Anim.pressable(r, 0.98f);
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (cb != null) cb.onClick(d, idx);
                    d.dismiss();
                }
            });
            body.addView(r, lpm(c, MP, WC, i == 0 ? 0 : Theme.S2));
        }
        dialogButtons(c, wrap, d, "关闭", null, null);
        d.show();
    }

    public static void info(Context c, String title, String bodyText) {
        Object[] parts = glassDialog(c, title);
        android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];
        TextView m = data(c, bodyText, Theme.T_DATA_S, Theme.MUTED);
        m.setLineSpacing(0, 1.45f);
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(m);
        int maxH = (int) (c.getResources().getDisplayMetrics().heightPixels * 0.5f);
        body.addView(sv, new LinearLayout.LayoutParams(MP, maxH));
        dialogButtons(c, wrap, d, "知道了", null, null);
        d.show();
    }

    public static void prompt(Context c, String title, String hint, String value,
                              final int minLines, final Callback<String> onOk) {
        Object[] parts = glassDialog(c, title);
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        final EditText e = minLines > 1 ? multiline(c, hint, value, minLines) : input(c, hint, value);
        body.addView(e, minLines > 1 ? lp(MP, WC) : lp(MP, Theme.dp(c, BTN_H + 4)));

        Button ok = primary(c, "确定");
        Button cancel = secondary(c, "取消");
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                d.dismiss();
                if (onOk != null) onOk.call(e.getText().toString());
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        wrap.addView(btnRow(c, BTN_H, cancel, ok), lpm(c, MP, WC, Theme.S4));
        d.show();
    }

    public static void triple(Context c, String title, String msg,
                              String leftText, final Runnable onLeft,
                              String rightText, final Runnable onRight) {
        Object[] parts = glassDialog(c, title);
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        TextView m = data(c, msg, Theme.T_DATA_S, Theme.MUTED);
        m.setLineSpacing(0, 1.45f);
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(m);
        int maxH = (int) (c.getResources().getDisplayMetrics().heightPixels * 0.42f);
        body.addView(sv, new LinearLayout.LayoutParams(MP, maxH));

        Button left = secondary(c, leftText);
        Button close = secondary(c, "关闭");
        Button right = primary(c, rightText);
        left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); if (onLeft != null) onLeft.run(); }
        });
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); if (onRight != null) onRight.run(); }
        });
        wrap.addView(btnRow(c, BTN_H, left, close, right), lpm(c, MP, WC, Theme.S4));
        d.show();
    }

    /** 进度对话框，返回 [dialog, 文本, 分段表] */
    public static Object[] progress(Context c, String title, String initial) {
        Object[] parts = glassDialog(c, title);
        android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        TextView m = data(c, initial, Theme.T_DATA, Theme.MUTED);
        body.addView(m);
        SegmentGauge g = new SegmentGauge(c, true);
        body.addView(g, lpm(c, MP, WC, Theme.S3));
        d.setCancelable(false);
        d.show();
        return new Object[]{d, m, g};
    }

    public interface Callback<T> {
        void call(T value);
    }
}
