package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import java.util.List;

/** 7 天清理柱状图（自绘，无依赖） */
public class StatsChartView extends View {

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private List<Object[]> data;   // [date, freed, count]

    public StatsChartView(Context c) {
        super(c);
        lbl.setTextSize(Theme.dp(c, 9));
        lbl.setColor(Theme.DIM);
        lbl.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Object[]> d) {
        this.data = d;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Theme.dp(getContext(), 92));
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (data == null || data.isEmpty()) return;
        int n = data.size();
        float w = getWidth(), h = getHeight();
        float labelH = Theme.dp(getContext(), 14);
        float chartH = h - labelH;
        float gap = Theme.dp(getContext(), 6);
        float bw = (w - gap * (n - 1)) / n;

        long max = 1;
        for (Object[] d : data) max = Math.max(max, (Long) d[1]);

        for (int i = 0; i < n; i++) {
            Object[] d = data.get(i);
            long v = (Long) d[1];
            float bh = v <= 0 ? Theme.dp(getContext(), 2) : Math.max(Theme.dp(getContext(), 3),
                    chartH * v / (float) max);
            float left = i * (bw + gap);
            float top = chartH - bh;
            r.set(left, top, left + bw, chartH);
            if (v > 0) {
                bar.setShader(new LinearGradient(0, top, 0, chartH,
                        Theme.ACCENT, Theme.ACCENT_D, Shader.TileMode.CLAMP));
            } else {
                bar.setShader(null);
                bar.setColor(Theme.LINE);
            }
            float rad = Theme.dp(getContext(), 4);
            cv.drawRoundRect(r, rad, rad, bar);

            String date = (String) d[0];
            String day = date.length() >= 10 ? date.substring(8) : date;
            cv.drawText(day, left + bw / 2, h - Theme.dp(getContext(), 3), lbl);
        }
    }
}
