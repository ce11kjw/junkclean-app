package com.ce11kjw.junkclean;

import android.app.AlertDialog;
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
        int p = Theme.dp(c, 16);
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
        b.setTextSize(14);
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
        b.setTextSize(11.5f);
        b.setAllCaps(false);
        b.setPadding(Theme.dp(c, 12), 0, Theme.dp(c, 12), 0);
        b.setMinWidth(0); b.setMinimumWidth(0);
        b.setMinHeight(Theme.dp(c, 30)); b.setMinimumHeight(Theme.dp(c, 30));
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

    public static EditText input(Context c, String hint, String value) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextSize(12.5f);
        e.setTextColor(Theme.TEXT);
        e.setHintTextColor(Theme.DIM);
        e.setBackground(Theme.card(c, 12));
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

    /** 确认对话框 */
    public static void confirm(Context c, String title, String msg, final Runnable onOk) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) { onOk.run(); }
                })
                .show();
    }

    /** 单选对话框 */
    public static void pick(Context c, String title, final String[] labels, int checked,
                            final android.content.DialogInterface.OnClickListener cb) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setSingleChoiceItems(labels, checked, cb)
                .setNegativeButton("关闭", null)
                .show();
    }

    /** 纯文本对话框（用于预览清单） */
    public static void info(Context c, String title, String body) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(body)
                .setPositiveButton("知道了", null).show();
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
