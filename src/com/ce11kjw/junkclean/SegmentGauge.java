package com.ce11kjw.junkclean;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * 签名元素：分段式精密存储表。
 *
 * 40 个独立色块而非连续条，填充时逐段点亮，像 VU 表的电平柱。
 * 主刻度对齐 25/50/75%，次刻度每 10%。超过阈值时末段脉冲。
 * 同一套绘制逻辑通过 compact 模式复用到目录排行的小型条上。
 */
public class SegmentGauge extends View {

    private static final int SEGMENTS = 40;

    private final Paint segPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float target = 0f;      // 目标百分比
    private float shown  = 0f;      // 当前动画值
    private float pulse  = 1f;      // 超阈值脉冲透明度
    private boolean compact = false;
    private ValueAnimator anim, pulseAnim;

    public SegmentGauge(Context c) {
        this(c, false);
    }

    public SegmentGauge(Context c, boolean compact) {
        super(c);
        this.compact = compact;
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setPercent(float p) {
        float clamped = Math.max(0f, Math.min(100f, p));
        if (anim != null) anim.cancel();
        final float from = shown;
        target = clamped;
        anim = ValueAnimator.ofFloat(from, clamped);
        anim.setDuration(compact ? 520 : 760);
        anim.setInterpolator(Theme.standard());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                shown = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        anim.start();
        managePulse(clamped);
    }

    /** 无动画直接落位，用于列表复用场景 */
    public void setPercentImmediate(float p) {
        if (anim != null) anim.cancel();
        shown = target = Math.max(0f, Math.min(100f, p));
        managePulse(shown);
        invalidate();
    }

    private void managePulse(float p) {
        boolean need = p >= 90f && !compact;
        if (need && pulseAnim == null) {
            pulseAnim = ValueAnimator.ofFloat(1f, 0.45f);
            pulseAnim.setDuration(760);
            pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnim.setInterpolator(Theme.standard());
            pulseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    pulse = ((Float) a.getAnimatedValue()).floatValue();
                    invalidate();
                }
            });
            pulseAnim.start();
        } else if (!need && pulseAnim != null) {
            pulseAnim.cancel();
            pulseAnim = null;
            pulse = 1f;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (anim != null) anim.cancel();
        if (pulseAnim != null) pulseAnim.cancel();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, Theme.dp(getContext(), compact ? 10 : 30));
    }

    /** 按占用率取色：正常强调色 → 琥珀 → 红 */
    private int colorFor(float pct) {
        if (pct >= 90f) return Theme.DANGER;
        if (pct >= 75f) return Theme.WARN;
        return Theme.ACCENT;
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth();
        if (w <= 0) return;

        int segs = compact ? 20 : SEGMENTS;
        float barH = compact ? getHeight() : Theme.dp(getContext(), 18);
        float gap = Theme.dp(getContext(), compact ? 1.5f : 2f);
        float segW = (w - gap * (segs - 1)) / segs;
        float radius = Theme.dp(getContext(), compact ? 1.5f : 2.5f);

        float filledExact = shown / 100f * segs;
        int fullSegs = (int) filledExact;
        float partial = filledExact - fullSegs;

        int active = colorFor(target);
        int idle = Theme.alpha(Theme.light ? 0x000000 : 0xFFFFFF, Theme.light ? 0x14 : 0x12);

        for (int i = 0; i < segs; i++) {
            float left = i * (segW + gap);
            rect.set(left, 0, left + segW, barH);

            if (i < fullSegs) {
                // 已点亮段：沿长度做强调色到亮色的渐变，末段应用脉冲
                float t = segs > 1 ? i / (float) (segs - 1) : 0f;
                int col = Theme.mix(active, Theme.mix(active, 0xFFFFFFFF, 0.35f), t);
                int a = 0xFF;
                if (target >= 90f && i >= fullSegs - 3) a = (int) (0xFF * pulse);
                segPaint.setColor(Theme.alpha(col, a));
            } else if (i == fullSegs && partial > 0.05f) {
                segPaint.setColor(Theme.alpha(active, (int) (0xFF * partial)));
            } else {
                segPaint.setColor(idle);
            }
            cv.drawRoundRect(rect, radius, radius, segPaint);
        }

        if (compact) return;

        // 刻度：主刻度 25% 一档，次刻度 10% 一档
        float tickTop = barH + Theme.dp(getContext(), 5);
        for (int p = 0; p <= 100; p += 10) {
            boolean major = (p % 25 == 0);
            float x = w * p / 100f;
            if (x >= w) x = w - Theme.dp(getContext(), 0.75f);
            tickPaint.setColor(Theme.alpha(Theme.light ? 0x000000 : 0xFFFFFF,
                    major ? (Theme.light ? 0x38 : 0x40) : (Theme.light ? 0x1C : 0x20)));
            tickPaint.setStrokeWidth(Theme.dp(getContext(), major ? 1.4f : 1f));
            cv.drawLine(x, tickTop, x,
                    tickTop + Theme.dp(getContext(), major ? 7 : 4), tickPaint);
        }
    }
}
