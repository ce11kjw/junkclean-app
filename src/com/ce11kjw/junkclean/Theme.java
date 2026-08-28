package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

/**
 * 设计令牌：Instrument（精密仪器）方向。
 *
 * 底色用带蓝紫底调的石墨而非纯黑 —— 纯黑会让半透明玻璃叠上去发灰，
 * 石墨底才有色彩可以透过来。强调色只用于数据读数与激活态，
 * 其余全部灰阶，让每一处彩色都带信息量。
 */
public final class Theme {

    // ---------- 色彩 ----------
    public static int VOID     = 0xFF08090F;   // 最底层
    public static int BG       = 0xFF0E1018;   // 页面底
    public static int PANEL    = 0xFF161A26;   // 卡片外壳
    public static int GLASS    = 0xFF1E2333;   // 卡片内芯
    public static int SURFACE2 = 0xFF252B3D;   // 列表项 / 次级按钮
    public static int HAIRLINE = 0x1AFFFFFF;   // 外壳发丝边
    public static int EDGE     = 0x38FFFFFF;   // 内芯高光边

    public static int TEXT     = 0xFFF4F5FA;
    public static int MUTED    = 0xFFA8AEC2;
    public static int DIM      = 0xFF6B7288;

    public static int ACCENT   = 0xFF2DD4A7;
    public static int ACCENT_D = 0xFF0F9E78;
    public static int ACCENT_L = 0xFF6FF0CB;
    public static int WARN     = 0xFFF5A524;
    public static int DANGER   = 0xFFF3576B;

    public static boolean light = false;

    /**
     * 玻璃感强度 0~1：
     *   0.0 = 实色卡片（深色/浅色主题本色）
     *   0.3 = 半透（默认），看得到壁纸色调
     *   1.0 = 真玻璃，几乎全透，只剩边框+羽化+阴影
     */
    public static float glass = 0.30f;

    /** 边缘羽化半径 dp：0 硬边，4 微，8 默认（玻璃感强），16 强玻璃 */
    public static int glassBlur = 8;

    /** ga：把基色 alpha 按玻璃感重新映射。0 玻璃感 → 满不透明；1 → 几乎全透 */
    /**
     * 玻璃感是「整体不透明度」控制杆：
     *   glass=0   → 完全不透明（0xFF），卡片是实色的
     *   glass=1   → 回到 baseAlpha（内芯 0x16 很透，对话框 0xF4 很实）
     *   中间线性渐变，0~50% 保持较高不透明度，用户拉到多少就看到多少
     *
     * 之前用 baseAlpha * glass，baseAlpha 小的层（内芯 22）在玻璃感 10% 时
     * 就被压到几乎透明——这就是「10% 就全透」的 bug。
     */
    public static int ga(int baseAlpha) {
        if (glass <= 0f) return 0xFF;
        if (glass >= 1f) return baseAlpha;
        // 从 0xFF 向 baseAlpha 线性过渡：glass=0.5 → (0xFF+baseAlpha)/2
        int a = baseAlpha + (int) ((0xFF - baseAlpha) * (1f - glass));
        return Math.max(0x08, Math.min(0xFF, a));
    }

    /** gaEdge：边框 alpha 随玻璃感**反向放大**，玻璃感越强边越亮（描出轮廓） */
    public static int gaEdge(int baseAlpha) {
        if (glass <= 0f) return baseAlpha;
        float boost = 1f + glass * 1.5f;
        return Math.max(0, Math.min(0xFF, (int) (baseAlpha * boost)));
    }

    // ---------- 字号（8 档，最大 32sp） ----------
    public static final float T_DISPLAY  = 32f;
    public static final float T_TITLE    = 21f;
    public static final float T_HEAD     = 16.5f;
    public static final float T_BODY     = 13.5f;
    public static final float T_BODY_S   = 12.5f;
    public static final float T_DATA     = 13f;
    public static final float T_DATA_S   = 11.5f;
    public static final float T_MICRO    = 9.5f;

    // ---------- 间距（4pt 基准） ----------
    public static final int S1 = 4,  S2 = 8,  S3 = 12, S4 = 16;
    public static final int S5 = 24, S6 = 32, S7 = 48, S8 = 64;

    // ---------- 圆角（同心） ----------
    public static final int R_SHELL = 28;
    public static final int R_CORE  = 22;   // R_SHELL - 内边距 6
    public static final int R_ITEM  = 14;
    public static final int R_PILL  = 999;
    public static final int SHELL_PAD = 6;

    // ---------- 动效曲线 ----------
    private static Interpolator STANDARD, PRESS, DECEL;
    private static Interpolator SPRING;

    public static Interpolator standard() {
        if (STANDARD == null) STANDARD = new PathInterpolator(0.32f, 0.72f, 0f, 1f);
        return STANDARD;
    }
    public static Interpolator press() {
        if (PRESS == null) PRESS = new PathInterpolator(0.2f, 0f, 0f, 1f);
        return PRESS;
    }
    public static Interpolator decel() {
        if (DECEL == null) DECEL = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
        return DECEL;
    }
    public static Interpolator spring() {
        if (SPRING == null) SPRING = new OvershootInterpolator(0.9f);
        return SPRING;
    }

    // ---------- 字型（4 个角色，全部系统族，零体积成本） ----------
    private static Typeface DISPLAY, DATA, BODY, MICRO;

    /** 大标题与数字读数：细体 + 负字距 */
    public static Typeface display() {
        if (DISPLAY == null) DISPLAY = Typeface.create("sans-serif-light", Typeface.NORMAL);
        return DISPLAY;
    }

    /**
     * OpenType 表格数字：让 0-9 等宽，排行列表的字节数对齐成列（API 26+）。
     * 在所有需要显示数值的 TextView 上调一次。
     */
    /** 所有字节数、百分比、路径、计数 —— 等宽保证多行数字垂直对齐 */
    public static Typeface data() {
        if (DATA == null) DATA = Typeface.create("monospace", Typeface.NORMAL);
        return DATA;
    }
    public static Typeface body() {
        if (BODY == null) BODY = Typeface.create("sans-serif", Typeface.NORMAL);
        return BODY;
    }
    /** 分区标签、徽章：中等字重 + 大字距 + 全大写 */
    public static Typeface micro() {
        if (MICRO == null) MICRO = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        return MICRO;
    }

    private Theme() {}

    /**
     * 应用主题/强调色/玻璃感/羽化
     * @param theme dark / oled / light — 与玻璃感正交
     * @param accent emerald / violet / blue / pink
     * @param glassIntensity 0..1，0=实色，1=真玻璃
     * @param blurDp 边缘羽化半径 dp
     */
    public static void apply(String theme, String accent,
                             float glassIntensity, int blurDp) {
        glass = Math.max(0f, Math.min(1f, glassIntensity));
        glassBlur = blurDp;
        applyTheme(theme, accent);
    }

    /** 旧调用兼容：按当前 glass 字段值应用 */
    public static void apply(String theme, String accent) {
        applyTheme(theme, accent);
    }

    /** 仅切换主题与强调色 */
    public static void applyTheme(String theme, String accent) {
        if ("oled".equals(theme)) {
            VOID = 0xFF000000; BG = 0xFF000000;
            PANEL = 0xFF0B0D14; GLASS = 0xFF12151F; SURFACE2 = 0xFF191D28;
            HAIRLINE = 0x14FFFFFF; EDGE = 0x2EFFFFFF;
            TEXT = 0xFFF4F5FA; MUTED = 0xFF9FA5B8; DIM = 0xFF62687C;
            light = false;
        } else if ("light".equals(theme)) {
            VOID = 0xFFEDEFF5; BG = 0xFFF2F3F7;
            PANEL = 0xFFE7E9F0; GLASS = 0xFFFFFFFF; SURFACE2 = 0xFFF0F1F6;
            HAIRLINE = 0x14000000; EDGE = 0x0F000000;
            TEXT = 0xFF12141C; MUTED = 0xFF565D70; DIM = 0xFF8A90A2;
            light = true;
        } else {
            VOID = 0xFF08090F; BG = 0xFF0E1018;
            PANEL = 0xFF161A26; GLASS = 0xFF1E2333; SURFACE2 = 0xFF252B3D;
            HAIRLINE = 0x1AFFFFFF; EDGE = 0x38FFFFFF;
            TEXT = 0xFFF4F5FA; MUTED = 0xFFA8AEC2; DIM = 0xFF6B7288;
            light = false;
        }

        if ("violet".equals(accent)) {
            ACCENT = 0xFF8B7CFF; ACCENT_D = 0xFF5B48E0; ACCENT_L = 0xFFB9AEFF;
        } else if ("blue".equals(accent)) {
            ACCENT = 0xFF4A9EFF; ACCENT_D = 0xFF1F6FD4; ACCENT_L = 0xFF8FC6FF;
        } else if ("pink".equals(accent)) {
            ACCENT = 0xFFFF7EA8; ACCENT_D = 0xFFE04E80; ACCENT_L = 0xFFFFB0C8;
        } else {
            ACCENT = light ? 0xFF0FA37E : 0xFF2DD4A7;
            ACCENT_D = 0xFF0F9E78; ACCENT_L = 0xFF6FF0CB;
        }
        if (light) {
            WARN = 0xFFC97A05; DANGER = 0xFFD93A50;
        } else {
            WARN = 0xFFF5A524; DANGER = 0xFFF3576B;
        }
    }

    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /**
     * 按当前 glassOpacity 缩放 alpha。
     * 0 → 卡片完全不透明（实色），1 → 按原始 a 透出来。
     * 防止穿透度滑到 1 时透明度溢出（>0xFF）。
     */

    /** 两色按比例混合，t=0 取 a，t=1 取 b */
    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    // ---------- 容器：双层机加工外壳 ----------

    /** 外壳：铝合金托盘。半透明底 + 发丝外边 */
    public static Drawable shell(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, R_SHELL));
        g.setColor(glass > 0f ? alpha(light ? 0xFFFFFF : 0x10131E, ga(light ? 0x5E : 0x6E)) : PANEL);
        g.setStroke(Math.max(1, dp(c, 0.8f)), glass > 0f ? alpha(0xFFFFFF, ga(light ? 0x9E : 0x22)) : HAIRLINE);
        return g;
    }

    /** 内芯：嵌进托盘的玻璃面板。顶部内高光是「嵌入感」的来源 */
    public static Drawable core(Context c) {
        float r = dp(c, R_CORE);
        GradientDrawable base = new GradientDrawable();
        base.setCornerRadius(r);
        if (glass > 0f) {
            base.setColor(alpha(light ? 0xFFFFFF : 0x14141E, ga(light ? 0x60 : 0x16)));
            base.setStroke(Math.max(1, dp(c, 0.8f)), alpha(0xFFFFFF, ga(light ? 0xD2 : 0x30)));
        } else {
            base.setColor(GLASS);
            base.setStroke(Math.max(1, dp(c, 0.8f)), EDGE);
        }
        GradientDrawable gloss = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{alpha(0xFFFFFF, ga(light ? 0x00 : 0x1C)),
                          alpha(0xFFFFFF, ga(0x06)),
                          alpha(0xFFFFFF, 0x00)});
        gloss.setCornerRadius(r);
        return new LayerDrawable(new Drawable[]{base, gloss});
    }

    /** 列表项 / 内层小容器 */
    public static Drawable item(Context c, boolean pressed) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, R_ITEM));
        if (pressed) {
            g.setColor(alpha(light ? 0x000000 : 0xFFFFFF, ga(light ? 0x0E : 0x16)));
        } else {
            g.setColor(glass > 0f ? alpha(0xFFFFFF, ga(light ? 0x8A : 0x0A)) : SURFACE2);
        }
        return g;
    }

    // ---------- 按钮 ----------

    public static Drawable primaryBtn(Context c) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{ACCENT, ACCENT_D});
        g.setCornerRadius(dp(c, R_PILL));
        return g;
    }

    public static Drawable dangerBtn(Context c) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{DANGER, mix(DANGER, 0xFF000000, 0.28f)});
        g.setCornerRadius(dp(c, R_PILL));
        return g;
    }

    public static Drawable ghostBtn(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, R_PILL));
        g.setColor(glass > 0f ? alpha(0xFFFFFF, ga(light ? 0x8A : 0x12)) : SURFACE2);
        g.setStroke(Math.max(1, dp(c, 0.8f)),
                glass > 0f ? alpha(0xFFFFFF, ga(light ? 0x00 : 0x2A)) : alpha(0xFFFFFF, ga(light ? 0x00 : 0x1E)));
        return g;
    }

    /** 按钮内嵌的圆形图标容器 */
    public static Drawable iconWell(Context c, boolean onAccent) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, R_PILL));
        g.setColor(onAccent ? alpha(0xFF000000, 0x24) : alpha(0xFFFFFF, 0x14));
        return g;
    }

    public static Drawable badge(Context c, int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, R_PILL));
        g.setColor(fill);
        return g;
    }

    /** 对话框：外壳同款但更实，避免文字压在壁纸上 */
    public static Drawable dialog(Context c) {
        float r = dp(c, R_SHELL);
        GradientDrawable base = new GradientDrawable();
        base.setCornerRadius(r);
        base.setColor(glass > 0f ? alpha(light ? 0xFFFFFF : 0x171B27, ga(light ? 0xF4 : 0xEE))
                            : (light ? 0xFFFFFFFF : 0xFF171B27));
        base.setStroke(Math.max(1, dp(c, 0.8f)), alpha(0xFFFFFF, ga(light ? 0x00 : 0x3A)));
        GradientDrawable gloss = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{alpha(0xFFFFFF, light ? 0x00 : 0x18), alpha(0xFFFFFF, 0x00)});
        gloss.setCornerRadius(r);
        return new LayerDrawable(new Drawable[]{base, gloss});
    }

    /** 底部导航：浮空玻璃条 */
    public static Drawable navBar(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, R_PILL));
        g.setColor(glass > 0f ? alpha(light ? 0xFFFFFF : 0x0C0F18, ga(light ? 0xE0 : 0xD8))
                         : (light ? 0xFFE7E9F0 : 0xFF141824));
        g.setStroke(Math.max(1, dp(c, 0.8f)), alpha(0xFFFFFF, ga(light ? 0x00 : 0x1E)));
        return g;
    }

    /** 导航激活态药丸 */
    public static Drawable navPill(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, R_PILL));
        g.setColor(alpha(ACCENT, 0x26));
        return g;
    }
}
