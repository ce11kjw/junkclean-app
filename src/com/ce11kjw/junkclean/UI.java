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
import android.widget.TextView;

/** 视图工厂：统一深空玻璃风格 */
public final class UI {
    private UI() {}

    public static final int MP = LinearLayout.LayoutParams.MATCH_PARENT;
    public static final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;

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

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(Theme.card(c, 18));
        int p = Theme.dp(c, 15);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static TextView title(Context c, String s) {
        TextView t = text(c, s, 15, Theme.TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView h2(Context c, String s) {
        TextView t = text(c, s, 12.5f, Theme.MUTED);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView note(Context c, String s) {
        TextView t = text(c, s, 11.5f, Theme.DIM);
        t.setLineSpacing(0, 1.35f);
        return t;
    }

    /** 空状态：✦ + 文案，居中 */
    public static TextView empty(Context c, String s) {
        TextView t = text(c, "✦\n" + s, 12, Theme.DIM);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(0, 1.6f);
        t.setPadding(0, Theme.dp(c, 18), 0, Theme.dp(c, 18));
        return t;
    }

    public static Button primary(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(12.5f);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(Theme.primaryBtn(c));
        b.setStateListAnimator(null);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    public static Button danger(Context c, String s) {
        Button b = primary(c, s);
        b.setBackground(Theme.dangerBtn(c));
        return b;
    }

    public static Button secondary(Context c, String s) {
        Button b = primary(c, s);
        b.setBackground(Theme.secondaryBtn(c));
        b.setTextColor(Theme.MUTED);
        return b;
    }

    public static Button chip(Context c, String s, boolean active) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(11f);
        b.setAllCaps(false);
        b.setPadding(Theme.dp(c, 10), 0, Theme.dp(c, 10), 0);
        b.setMinWidth(0); b.setMinimumWidth(0);
        b.setMinHeight(Theme.dp(c, 26)); b.setMinimumHeight(Theme.dp(c, 26));
        b.setStateListAnimator(null);
        setChipActive(c, b, active);
        return b;
    }

    public static void setChipActive(Context c, Button b, boolean active) {
        if (active) {
            b.setBackground(Theme.primaryBtn(c));
            b.setTextColor(0xFFFFFFFF);
        } else {
            b.setBackground(Theme.secondaryBtn(c));
            b.setTextColor(Theme.MUTED);
        }
    }

    public static TextView badge(Context c, String s, int fg, int bg) {
        TextView t = text(c, s, 10.5f, fg);
        t.setBackground(Theme.badge(c, bg));
        t.setPadding(Theme.dp(c, 8), Theme.dp(c, 2), Theme.dp(c, 8), Theme.dp(c, 2));
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

    /** 开关行：标题 + 说明 + 右侧 Switch */
    public static LinearLayout switchRow(final Context c, String title, String desc,
                                        boolean on, final android.widget.CompoundButton.OnCheckedChangeListener cb) {
        LinearLayout r = row(c);
        r.setPadding(0, Theme.dp(c, 6), 0, Theme.dp(c, 6));
        LinearLayout info = col(c);
        info.addView(text(c, title, 13, Theme.TEXT));
        if (desc != null && !desc.isEmpty()) info.addView(note(c, desc));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, WC, 1f);
        r.addView(info, ip);
        android.widget.Switch sw = new android.widget.Switch(c);
        sw.setChecked(on);
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(Theme.ACCENT));
        sw.setTrackTintList(android.content.res.ColorStateList.valueOf(Theme.LINE2));
        sw.setOnCheckedChangeListener(cb);
        r.addView(sw);
        return r;
    }

    /** 小号 Switch（整理规则等紧凑场景） */
    public static android.widget.Switch smallSwitch(Context c, boolean on) {
        android.widget.Switch sw = new android.widget.Switch(c);
        sw.setChecked(on);
        sw.setScaleX(0.85f);
        sw.setScaleY(0.85f);
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(Theme.ACCENT));
        sw.setTrackTintList(android.content.res.ColorStateList.valueOf(Theme.LINE2));
        return sw;
    }

    public static EditText input(Context c, String hint, String value) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextSize(12.5f);
        e.setTextColor(Theme.TEXT);
        e.setHintTextColor(Theme.DIM);
        e.setBackground(Theme.inner(c, 12));
        int p = Theme.dp(c, 10);
        e.setPadding(p + Theme.dp(c, 2), p, p, p);
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

    /** 分区标题：小号大写 + 强调色 */
    public static TextView section(Context c, String s) {
        TextView t = text(c, s, 11, Theme.ACCENT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(Theme.dp(c, 4), Theme.dp(c, 10), 0, Theme.dp(c, 4));
        return t;
    }

    public static View spacer(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, Theme.dp(c, dp)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Theme.LINE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MP, Math.max(1, Theme.dp(c, 0.6f)));
        p.topMargin = p.bottomMargin = Theme.dp(c, 8);
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

    /** 标准按钮高度（紧凑） */
    public static final int BTN_H = 36;
    public static final int BTN_H_MAIN = 44;

    /** 等宽按钮行，自动加间距 */
    public static LinearLayout btnRow(Context c, int heightDp, Button... bs) {
        LinearLayout r = row(c);
        for (int i = 0; i < bs.length; i++) {
            LinearLayout.LayoutParams p = weight(1f, heightDp, c);
            if (i > 0) p.leftMargin = Theme.dp(c, 6);
            r.addView(bs[i], p);
        }
        return r;
    }

    // ================= 拟态玻璃对话框 =================

    /** 构造玻璃风格对话框骨架，返回 [dialog, 内容容器] */
    private static Object[] glassDialog(Context c, String title) {
        final android.app.Dialog d = new android.app.Dialog(c);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout wrap = col(c);
        wrap.setBackground(Theme.dialog(c));
        int p = Theme.dp(c, 20);
        wrap.setPadding(p, Theme.dp(c, 18), p, Theme.dp(c, 14));

        if (title != null && !title.isEmpty()) {
            TextView t = text(c, title, 16, Theme.TEXT);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            wrap.addView(t);
        }

        LinearLayout body = col(c);
        wrap.addView(body, lpm(c, MP, WC, title == null ? 0 : 10));

        d.setContentView(wrap);
        android.view.Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0x00000000));
            w.setDimAmount(Theme.glass ? 0.45f : 0.6f);
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = (int) (c.getResources().getDisplayMetrics().widthPixels * 0.88f);
            w.setAttributes(lp);
        }
        return new Object[]{d, body, wrap};
    }

    /** 底部按钮行（对话框用） */
    private static void dialogButtons(Context c, LinearLayout wrap, final android.app.Dialog d,
                                      String okText, final Runnable onOk,
                                      String cancelText) {
        Button ok = primary(c, okText);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                d.dismiss();
                if (onOk != null) onOk.run();
            }
        });
        if (cancelText == null) {
            wrap.addView(btnRow(c, BTN_H, ok), lpm(c, MP, WC, 16));
            return;
        }
        Button cancel = secondary(c, cancelText);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        wrap.addView(btnRow(c, BTN_H, cancel, ok), lpm(c, MP, WC, 16));
    }

    /** 确认对话框 */
    public static void confirm(Context c, String title, String msg, final Runnable onOk) {
        Object[] parts = glassDialog(c, title);
        android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        TextView m = text(c, msg, 13, Theme.MUTED);
        m.setLineSpacing(0, 1.45f);
        body.addView(m);
        dialogButtons(c, wrap, d, "确定", onOk, "取消");
        d.show();
    }

    /** 单选对话框 */
    public static void pick(Context c, String title, final String[] labels, final int checked,
                            final android.content.DialogInterface.OnClickListener cb) {
        Object[] parts = glassDialog(c, title);
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            LinearLayout row = row(c);
            row.setBackground(Theme.inner(c, 12));
            int pd = Theme.dp(c, 12);
            row.setPadding(pd, Theme.dp(c, 10), pd, Theme.dp(c, 10));
            TextView t = text(c, labels[i], 13.5f, idx == checked ? Theme.ACCENT : Theme.TEXT);
            row.addView(t, new LinearLayout.LayoutParams(0, WC, 1f));
            if (idx == checked) row.addView(text(c, "✓", 14, Theme.ACCENT));
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (cb != null) cb.onClick(d, idx);
                    d.dismiss();
                }
            });
            body.addView(row, lpm(c, MP, WC, i == 0 ? 0 : 6));
        }
        dialogButtons(c, wrap, d, "关闭", null, null);
        d.show();
    }

    /** 长文本对话框（预览清单等），内容可滚动 */
    public static void info(Context c, String title, String bodyText) {
        Object[] parts = glassDialog(c, title);
        android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        TextView m = text(c, bodyText, 12, Theme.MUTED);
        m.setLineSpacing(0, 1.4f);
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(m);
        int maxH = (int) (c.getResources().getDisplayMetrics().heightPixels * 0.5f);
        body.addView(sv, new LinearLayout.LayoutParams(MP, maxH));
        dialogButtons(c, wrap, d, "知道了", null, null);
        d.show();
    }

    /** 带输入框的对话框 */
    public static void prompt(Context c, String title, String hint, String value,
                              final int minLines, final Callback<String> onOk) {
        Object[] parts = glassDialog(c, title);
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        final EditText e = minLines > 1 ? multiline(c, hint, value, minLines) : input(c, hint, value);
        body.addView(e, minLines > 1 ? lp(MP, WC) : lp(MP, Theme.dp(c, BTN_H)));

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
        wrap.addView(btnRow(c, BTN_H, cancel, ok), lpm(c, MP, WC, 16));
        d.show();
    }

    /** 三按钮对话框（如：清空 / 关闭 / 全部还原） */
    public static void triple(Context c, String title, String msg,
                              String leftText, final Runnable onLeft,
                              String rightText, final Runnable onRight) {
        Object[] parts = glassDialog(c, title);
        final android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        LinearLayout wrap = (LinearLayout) parts[2];

        TextView m = text(c, msg, 12, Theme.MUTED);
        m.setLineSpacing(0, 1.4f);
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
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
        wrap.addView(btnRow(c, BTN_H, left, close, right), lpm(c, MP, WC, 16));
        d.show();
    }

    /** 进度对话框，返回可更新文本的句柄 */
    public static Object[] progress(Context c, String title, String initial) {
        Object[] parts = glassDialog(c, title);
        android.app.Dialog d = (android.app.Dialog) parts[0];
        LinearLayout body = (LinearLayout) parts[1];
        TextView m = text(c, initial, 13, Theme.MUTED);
        body.addView(m);
        StorageBarView bar = new StorageBarView(c);
        body.addView(bar, lpm(c, MP, WC, 12));
        d.setCancelable(false);
        d.show();
        return new Object[]{d, m, bar};
    }

    /** 简单回调 */
    public interface Callback<T> {
        void call(T value);
    }

    /** 文件项行：勾选框 + 名称 + 体积，可选长按回调 */
    public static LinearLayout fileRow(final Context c, final JunkItem it,
                                       final Runnable onChange, final Runnable onLongPress) {
        LinearLayout r = row(c);
        r.setPadding(0, Theme.dp(c, 5), 0, Theme.dp(c, 5));
        final CheckBox cb = check(c, it.checked);
        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean on) {
                it.checked = on;
                if (onChange != null) onChange.run();
            }
        });
        r.addView(cb);
        TextView nm = text(c, it.name, 12, Theme.MUTED);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, WC, 1f);
        np.leftMargin = Theme.dp(c, 4);
        r.addView(nm, np);
        r.addView(text(c, Util.fmtSize(it.size), 11.5f, Theme.DIM));
        if (onLongPress != null) {
            r.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) { onLongPress.run(); return true; }
            });
        }
        return r;
    }
}
