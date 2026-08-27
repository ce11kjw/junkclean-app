package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/** 深空玻璃配色（可切换 dark/oled/light + 4 强调色） */
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

    private Theme() {}

    /** 应用主题 + 强调色（必须在构建视图前调用） */
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

    /** 是否使用半透明卡片（有壁纸时开启，营造玻璃质感） */
    public static boolean glass = false;

    public static GradientDrawable card(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setCornerRadius(dp(c, radiusDp));
        g.setColor(glass ? alpha(SURFACE, 0xD2) : SURFACE);
        g.setStroke(dp(c, 1), glass ? alpha(LINE2, 0xB4) : LINE);
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
        g.setColor(SURFACE2);
        g.setStroke(dp(c, 1), LINE2);
        return g;
    }

    public static GradientDrawable badge(Context c, int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 99));
        g.setColor(fill);
        return g;
    }
}
