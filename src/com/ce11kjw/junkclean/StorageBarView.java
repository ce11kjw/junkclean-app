package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** 条形存储进度条：凹陷 track + 渐变 fill + 动画 */
public class StorageBarView extends View {

    private final Paint trackP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillP  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect   = new RectF();
    private float percent = 0f;      // 当前显示
    private float target  = 0f;      // 目标
    private long animStart;
    private float animFrom;

    public StorageBarView(Context c) {
        super(c);
        trackP.setColor(Theme.BG2);
        fillP.setColor(Theme.ACCENT);
    }

    public void setPercent(float p) {
        animFrom = percent;
        target = Math.max(0f, Math.min(100f, p));
        animStart = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, Theme.dp(getContext(), 14));
    }

    @Override
    protected void onDraw(Canvas cv) {
        float h = getHeight(), w = getWidth();
        float r = h / 2f;

        // 动画插值（ease-out cubic，600ms）
        long dt = System.currentTimeMillis() - animStart;
        if (dt < 600) {
            float t = dt / 600f;
            float e = 1f - (float) Math.pow(1 - t, 3);
            percent = animFrom + (target - animFrom) * e;
            postInvalidateOnAnimation();
        } else {
            percent = target;
        }

        // track
        rect.set(0, 0, w, h);
        cv.drawRoundRect(rect, r, r, trackP);

        // fill
        float fw = w * percent / 100f;
        if (fw > 1) {
            int c1 = Theme.ACCENT, c2 = Theme.ACCENT2;
            if (percent >= 90) { c1 = Theme.DANGER; c2 = 0xFFE11D48; }
            else if (percent >= 75) { c1 = Theme.WARN; c2 = 0xFFF59E0B; }
            fillP.setShader(new LinearGradient(0, 0, fw, 0, c1, c2, Shader.TileMode.CLAMP));
            rect.set(0, 0, Math.max(fw, h), h);
            cv.drawRoundRect(rect, r, r, fillP);
        }
    }
}
