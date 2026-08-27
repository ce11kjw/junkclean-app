package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;

/** 深空玻璃：3 主题 × 4 强调色；壁纸开启时卡片转为真半透明玻璃 */
public final class Theme {

    public static int BG       = 0xFF050509;
    public static int BG2      = 0xFF0A0A13;
    public static int SURFACE  = 0xFF12121C;
    public static int SURFACE2 = 0xFF1A1A26;
    public static int LINE     = 0xFF22222E;
    public static int LINE2    = 0xFF2E2E3C;
    public static int TEXT     = 0xFFF2F2F7;
    public static int MUTED    = 0xFFB8B8C4;
    public static int DIM      = 0xFF6E6E7C;
    public static int ACCENT   = 0xFF2DD4A7;
    public static int ACCENT_D = 0xFF059669;
    public static int ACCENT2  = 0xFF7C5CFF;
    public static int WARN     = 0xFFFBBF24;
    public static int DANGER   = 0xFFFB7185;

    public static boolean light = false;
    /** 壁纸启用时为 true：卡片走半透明玻璃，让背景透出来 */
    public static boolean glass = false;

    private Theme() {}

    public static void apply(String theme, String accent) {
        if ("oled".equals(theme)) {
            BG = 0xFF000000; BG2 = 0xFF000000;
            SURFACE = 0xFF0B0B0F; SURFACE2 = 0xFF13131A;
            LINE = 0xFF1A1A22; LINE2 = 0xFF262630;
            TEXT = 0xFFF2F2F7; MUTED = 0xFFB0B0BC; DIM = 0xFF66666F;
            light = false;
        } else if ("light".equals(theme)) {
            BG = 0xFFF4F5F9; BG2 = 0xFFFFFFFF;
            SURFACE = 0xFFFFFFFF; SURFACE2 = 0xFFEDEEF3;
            LINE = 0xFFDFE0E8; LINE2 = 0xFFC9CAD4;
            TEXT = 0xFF15151C; MUTED = 0xFF5A5A66; DIM = 0xFF8E8E9A;
            light = true;
        } else {
            BG = 0xFF050509; BG2 = 0xFF0A0A13;
            SURFACE = 0xFF12121C; SURFACE2 = 0xFF1A1A26;
            LINE = 0xFF22222E; LINE2 = 0xFF2E2E3C;
            TEXT = 0xFFF2F2F7; MUTED = 0xFFB8B8C4; DIM = 0xFF6E6E7C;
            light = false;
        }

        if ("violet".equals(accent)) {
            ACCENT = 0xFF7C5CFF; ACCENT_D = 0xFF5C3EFF; ACCENT2 = 0xFFB57CFF;
        } else if ("blue".equals(accent)) {
            ACCENT = 0xFF4A9EFF; ACCENT_D = 0xFF2563EB; ACCENT2 = 0xFF7CC4FF;
        } else if ("pink".equals(accent)) {
            ACCENT = 0xFFFF7EB6; ACCENT_D = 0xFFFF5C8A; ACCENT2 = 0xFFFFB0D0;
        } else {
            ACCENT = light ? 0xFF14B88F : 0xFF2DD4A7;
            ACCENT_D = 0xFF059669; ACCENT2 = 0xFF7C5CFF;
        }
    }

    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    /**
     * 玻璃卡片：无壁纸时用实色 SURFACE；有壁纸时用低透明度 + 顶部高光渐变，
     * 让下层模糊壁纸透出来形成拟态玻璃。
     */
    public static Drawable card(Context c, float radiusDp) {
        float r = dp(c, radiusDp);
        if (!glass) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(r);
            g.setColor(SURFACE);
            g.setStroke(dp(c, 1), LINE);
            return g;
        }

        // 底层：极低不透明度的冷色填充。之前用 0x8A 的近黑色，叠在压暗的壁纸上
        // 就变成一片灰；降到 0x4E 并偏蓝，壁纸色彩才能透上来。
        GradientDrawable base = new GradientDrawable();
        base.setCornerRadius(r);
        base.setColor(light ? alpha(0xFFFFFF, 0x6E) : alpha(0x1C1C2E, 0x4E));
        base.setStroke(dp(c, 1), light ? alpha(0xFFFFFF, 0xC8) : alpha(0xFFFFFF, 0x38));

        // 上层：斜向高光，从左上白到右下透明，比纯垂直渐变更像玻璃
        GradientDrawable gloss = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{light ? alpha(0xFFFFFF, 0x78) : alpha(0xFFFFFF, 0x30),
                          alpha(0xFFFFFF, 0x08),
                          alpha(0xFFFFFF, 0x00)});
        gloss.setCornerRadius(r);

        return new LayerDrawable(new Drawable[]{base, gloss});
    }

    /** 内层小容器（列表项、规则块） */
    public static Drawable inner(Context c, float radiusDp) {
        float r = dp(c, radiusDp);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(r);
        if (glass) {
            g.setColor(light ? alpha(0xFFFFFF, 0x5A) : alpha(0xFFFFFF, 0x14));
            g.setStroke(dp(c, 1), light ? alpha(0x000000, 0x12) : alpha(0xFFFFFF, 0x22));
        } else {
            g.setColor(SURFACE2);
            g.setStroke(dp(c, 1), LINE);
        }
        return g;
    }

    public static GradientDrawable primaryBtn(Context c) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{ACCENT, ACCENT_D});
        g.setCornerRadius(dp(c, 99));
        return g;
    }

    public static GradientDrawable dangerBtn(Context c) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{DANGER, 0xFFE11D48});
        g.setCornerRadius(dp(c, 99));
        return g;
    }

    public static GradientDrawable secondaryBtn(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 99));
        if (glass) {
            g.setColor(light ? alpha(0xFFFFFF, 0x82) : alpha(0xFFFFFF, 0x1A));
            g.setStroke(dp(c, 1), light ? alpha(0x000000, 0x18) : alpha(0xFFFFFF, 0x3A));
        } else {
            g.setColor(SURFACE2);
            g.setStroke(dp(c, 1), LINE2);
        }
        return g;
    }

    public static GradientDrawable badge(Context c, int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 99));
        g.setColor(fill);
        return g;
    }

    /** 对话框容器：玻璃模式下半透明 + 高光，否则实色卡片 */
    public static Drawable dialog(Context c) {
        float r = dp(c, 22);
        if (!glass) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(r);
            g.setColor(light ? 0xFFFFFFFF : 0xFF15151F);
            g.setStroke(dp(c, 1), LINE2);
            return g;
        }
        GradientDrawable base = new GradientDrawable();
        base.setCornerRadius(r);
        base.setColor(light ? alpha(0xFFFFFF, 0xEE) : alpha(0x1A1A2C, 0xE6));
        base.setStroke(dp(c, 1), light ? alpha(0x000000, 0x1E) : alpha(0xFFFFFF, 0x3E));
        GradientDrawable gloss = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{light ? alpha(0xFFFFFF, 0x70) : alpha(0xFFFFFF, 0x22),
                          alpha(0xFFFFFF, 0x00)});
        gloss.setCornerRadius(r);
        return new LayerDrawable(new Drawable[]{base, gloss});
    }

    /** 底部导航容器 */
    public static Drawable navBar(Context c) {
        GradientDrawable g = new GradientDrawable();
        if (glass) {
            g.setColor(light ? alpha(0xFFFFFF, 0xCC) : alpha(0x0C0C18, 0xB4));
        } else {
            g.setColor(BG2);
        }
        return g;
    }
}
