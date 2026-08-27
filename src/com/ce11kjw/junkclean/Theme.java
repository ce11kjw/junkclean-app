package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/** 深空玻璃配色 + 尺寸工具（对应 WebUI 的 CSS 变量） */
public final class Theme {
    public static final int BG        = Color.parseColor("#050509");
    public static final int BG2       = Color.parseColor("#0A0A13");
    public static final int SURFACE   = Color.parseColor("#12121C");
    public static final int SURFACE2  = Color.parseColor("#1A1A26");
    public static final int LINE      = Color.parseColor("#22222E");
    public static final int LINE2     = Color.parseColor("#2E2E3C");
    public static final int TEXT      = Color.parseColor("#F2F2F7");
    public static final int MUTED     = Color.parseColor("#B8B8C4");
    public static final int DIM       = Color.parseColor("#6E6E7C");
    public static final int ACCENT    = Color.parseColor("#2DD4A7");
    public static final int ACCENT2   = Color.parseColor("#7C5CFF");
    public static final int WARN      = Color.parseColor("#FBBF24");
    public static final int DANGER    = Color.parseColor("#FB7185");

    private Theme() {}

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    /** 玻璃卡片背景：圆角 + 描边 + 微亮填充 */
    public static GradientDrawable card(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setCornerRadius(dp(c, radiusDp));
        g.setColor(SURFACE);
        g.setStroke(dp(c, 1), LINE);
        return g;
    }

    /** 主按钮：青绿渐变胶囊 */
    public static GradientDrawable primaryBtn(Context c) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ACCENT, Color.parseColor("#059669")});
        g.setCornerRadius(dp(c, 99));
        return g;
    }

    /** 危险按钮：粉红渐变胶囊 */
    public static GradientDrawable dangerBtn(Context c) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{DANGER, Color.parseColor("#E11D48")});
        g.setCornerRadius(dp(c, 99));
        return g;
    }

    /** 次级按钮：描边胶囊 */
    public static GradientDrawable secondaryBtn(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 99));
        g.setColor(SURFACE2);
        g.setStroke(dp(c, 1), LINE2);
        return g;
    }

    /** 徽章背景 */
    public static GradientDrawable badge(Context c, int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 99));
        g.setColor(fill);
        return g;
    }
}
