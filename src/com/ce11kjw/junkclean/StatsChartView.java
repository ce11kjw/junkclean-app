package com.ce11kjw.junkclean;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import java.util.List;

/** 7 天清理柱状图：等宽日期标签 + 基线刻度 + 逐柱升起 */
public class StatsChartView extends View {

    private final Paint bar  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lbl  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r    = new RectF();

    private List<Object[]> data;   // [date, freed, count]
    private float grow = 0f;

    public StatsChartView(Context c) {
        super(c);
        lbl.setTextSize(Theme.dp(c, 8));
        lbl.setTypeface(Theme.data());
        lbl.setTextAlign(Paint.Align.CENTER);
        base.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Object[]> d) {
        this.data = d;
        grow = 0f;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(700);
        a.setInterpolator(Theme.standard());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator an) {
                grow = ((Float) an.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        a.start();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Theme.dp(getContext(), 108));
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (data == null || data.isEmpty()) return;
        int n = data.size();
        float w = getWidth(), h = getHeight();
        float labelH = Theme.dp(getContext(), 16);
        float chartH = h - labelH;
        float gap = Theme.dp(getContext(), 7);
        float bw = (w - gap * (n - 1)) / n;
        float rad = Theme.dp(getContext(), 5);

        long max = 1;
        for (Object[] d : data) max = Math.max(max, ((Long) d[1]).longValue());

        // 基线
        base.setColor(Theme.alpha(Theme.DIM, 0x30));
        cv.drawRect(0, chartH, w, chartH + Theme.dp(getContext(), 1), base);

        for (int i = 0; i < n; i++) {
            Object[] d = data.get(i);
            long v = ((Long) d[1]).longValue();
            float left = i * (bw + gap);

            float full = v <= 0 ? Theme.dp(getContext(), 3)
                    : Math.max(Theme.dp(getContext(), 4), chartH * v / (float) max);
            // 逐柱升起：越靠右延迟越大
            float local = Math.max(0f, Math.min(1f, grow * n - i * 0.55f));
            float bh = full * local;
            float top = chartH - bh;

            r.set(left, top, left + bw, chartH);
            if (v > 0) {
                bar.setShader(new LinearGradient(0, top, 0, chartH,
                        Theme.ACCENT_L, Theme.ACCENT_D, Shader.TileMode.CLAMP));
            } else {
                bar.setShader(null);
                bar.setColor(Theme.alpha(0xFFFFFF, Theme.light ? 0x14 : 0x0E));
            }
            cv.drawRoundRect(r, rad, rad, bar);

            String date = (String) d[0];
            lbl.setColor(v > 0 ? Theme.alpha(Theme.MUTED, 0xC8) : Theme.alpha(Theme.DIM, 0x8A));
            cv.drawText(date.length() >= 10 ? date.substring(8) : date,
                    left + bw / 2, h - Theme.dp(getContext(), 4), lbl);
        }
    }
}
