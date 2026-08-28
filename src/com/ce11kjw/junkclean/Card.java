package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 双层机加工外壳容器（C 方案：模糊带）。
 *
 * 外层铝合金托盘几乎全透，主要靠 onDraw 里的「模糊带」承担面板感：
 * 边缘 6dp 轻微羽化的暗色矩形，让壁纸在卡片之间的空隙保持原画质清晰，
 * 而卡片中心区域被轻微柔化。
 *
 * 内芯玻璃面板：极低透明度（0x16 = 9%），让壁纸几乎全透进来，
 * 仅靠描边和模糊带建立视觉层次。
 *
 * 文本可读性：所有内芯子 TextView 自动加一层软阴影，亮色壁纸下也读得清。
 */
public class Card extends LinearLayout {

    private final LinearLayout coreLayout;
    private final Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean ready;
    private int rShell, pad;

    public Card(Context c) {
        super(c);
        setOrientation(VERTICAL);
        // 外壳：极低填充，仅留描边轮廓
        setBackground(Theme.shell(c));
        rShell = Theme.dp(c, Theme.R_SHELL);
        pad = Theme.dp(c, Theme.SHELL_PAD);
        setPadding(pad, pad, pad, pad);

        // 模糊带 alpha：玻璃感低（实色卡片）下保持低调；玻璃感高（真玻璃）
        // 下仍可见一缕暗色形成层次。亮主题略强，暗主题略弱。
        int blurA;
        if (Theme.glass >= 0.5f) {
            blurA = Theme.light ? 0x24 : 0x20;
        } else {
            int base = Theme.light ? 0x40 : 0x30;
            blurA = Math.max(0, (int) (base * Theme.glass));
        }
        blurPaint.setColor(Color.argb(blurA, 0, 0, 0));
        try {
            blurPaint.setMaskFilter(new BlurMaskFilter(
                    Theme.dp(c, Theme.glassBlur), BlurMaskFilter.Blur.NORMAL));
        } catch (Throwable ignored) {
            // 个别设备不支持 BlurMaskFilter，降级为无模糊
        }

        // 内芯外圈描边：随玻璃感增强（边框是真玻璃的视觉层次承担者）
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(Theme.dp(c, 1.0f));
        int edgeBase = Theme.light ? 0x80 : 0x40;
        edgePaint.setColor(Color.argb(Theme.gaEdge(edgeBase), 0xFF, 0xFF, 0xFF));
        edgePaint.setAntiAlias(true);

        coreLayout = new InnerCore(c);
        coreLayout.setOrientation(VERTICAL);
        coreLayout.setBackground(Theme.core(c));
        int cp = Theme.dp(c, 18);
        coreLayout.setPadding(cp, Theme.dp(c, 16), cp, Theme.dp(c, 16));
        super.addView(coreLayout,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ready = true;
    }

    /**
     * 内芯给所有子 TextView 自动加一层软阴影，亮色壁纸下也能读。
     * 不会覆盖用户已显式设置过的阴影。
     */
    private class InnerCore extends LinearLayout {
        InnerCore(Context c) { super(c); }

        @Override
        public void addView(View child, int index, ViewGroup.LayoutParams params) {
            if (child instanceof TextView) {
                TextView t = (TextView) child;
                float sz = t.getTextSize();
                t.setShadowLayer(sz * 0.06f, 0, sz * 0.03f,
                        Theme.light ? Color.argb(0x70, 0, 0, 0) : Color.argb(0xB0, 0, 0, 0));
            }
            super.addView(child, index, params);
        }
    }

    public LinearLayout core() { return coreLayout; }

    /** 所有 addView 重载最终都走这里，统一转发到内芯 */
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!ready) {
            super.addView(child, index, params);
            return;
        }
        coreLayout.addView(child, index, params);
    }

    @Override
    public void removeAllViews() {
        if (coreLayout != null) coreLayout.removeAllViews();
        else super.removeAllViews();
    }

    @Override
    protected void dispatchDraw(Canvas cv) {
        // 模糊带 + 描边画在子 View 之前。BlurMaskFilter 在部分 GPU 驱动
        // （Adreno/Mali 旧版）上配合硬件加速会抛 UnsupportedOperationException，
        // 直接导致闪退。这里 try/catch 兜底：一旦失败就跳过模糊带，
        // 只保留描边（描边是普通 drawRoundRect，无风险）。
        try {
            if (getWidth() > 0 && getHeight() > 0 && blurPaint != null) {
                cv.drawRoundRect(0, 0, getWidth(), getHeight(), rShell, rShell, blurPaint);
            }
        } catch (Throwable ignored) {
            // GPU 不支持模糊带：静默降级，不崩溃
        }
        super.dispatchDraw(cv);
        try {
            if (getWidth() > 0 && getHeight() > 0 && edgePaint != null) {
                float half = edgePaint.getStrokeWidth() / 2f;
                cv.drawRoundRect(half, half, getWidth() - half, getHeight() - half,
                        rShell, rShell, edgePaint);
            }
        } catch (Throwable ignored) {
            // 描边失败也不影响主内容
        }
    }
}
