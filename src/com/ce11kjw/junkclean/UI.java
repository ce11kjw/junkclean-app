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

/** 视图工厂：统一深空玻璃风格的控件构造 */
public final class UI {
    private UI() {}

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

    /** 玻璃卡片容器 */
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
        TextView t = text(c, s, 13, Theme.MUTED);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setAllCaps(false);
        return t;
    }

    public static TextView note(Context c, String s) {
        TextView t = text(c, s, 11.5f, Theme.DIM);
        t.setLineSpacing(0, 1.35f);
        return t;
    }

    public static Button primary(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(Theme.primaryBtn(c));
        b.setStateListAnimator(null);
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

    /** 小胶囊按钮（Tab / 类型筛选用） */
    public static Button chip(Context c, String s, boolean active) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(11.5f);
        b.setAllCaps(false);
        b.setPadding(Theme.dp(c, 12), 0, Theme.dp(c, 12), 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(Theme.dp(c, 30));
        b.setMinimumHeight(Theme.dp(c, 30));
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
        return cb;
    }

    public static EditText input(Context c, String hint, String value) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextSize(13);
        e.setTextColor(Theme.TEXT);
        e.setHintTextColor(Theme.DIM);
        e.setBackground(Theme.card(c, 12));
        int p = Theme.dp(c, 10);
        e.setPadding(p + Theme.dp(c, 2), p, p, p);
        e.setSingleLine(true);
        e.setEllipsize(TextUtils.TruncateAt.END);
        return e;
    }

    public static View spacer(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, Theme.dp(c, dp)));
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

    public static final int MP = LinearLayout.LayoutParams.MATCH_PARENT;
    public static final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
}
