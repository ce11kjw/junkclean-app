package com.ce11kjw.junkclean;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * 长按进度环：贴在条目右侧，长按时沿圆周绘制进度，满圈才触发。
 * 原来长按加白名单是「按住 700ms 无任何反馈」，用户不知道要按多久。
 */
public class LongPressRing extends View {

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float progress = 0f;
    private ValueAnimator anim;
    private Runnable onComplete;
    private static final long HOLD_MS = 620;

    public LongPressRing(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        setAlpha(0f);
    }

    public void setOnComplete(Runnable r) { this.onComplete = r; }

    /** 由宿主 View 的 onTouch 转发 */
    public void handle(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:  start(); break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_MOVE:
                if (e.getActionMasked() != MotionEvent.ACTION_MOVE) cancel();
                break;
        }
    }

    public void start() {
        cancel();
        animate().alpha(1f).setDuration(120).start();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(HOLD_MS);
        anim.setInterpolator(Theme.press());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                progress = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            public void onAnimationEnd(android.animation.Animator a) {
                if (progress >= 0.99f && onComplete != null) {
                    onComplete.run();
                    Anim.tick(LongPressRing.this);
                }
                animate().alpha(0f).setStartDelay(160).setDuration(200).start();
                progress = 0f;
            }
        });
        anim.start();
    }

    public void cancel() {
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
        progress = 0f;
        animate().alpha(0f).setDuration(140).start();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (anim != null) anim.cancel();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int s = Theme.dp(getContext(), 18);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (progress <= 0f) return;
        float sw = Theme.dp(getContext(), 2f);
        ring.setStrokeWidth(sw);
        float inset = sw / 2f + Theme.dp(getContext(), 1);
        box.set(inset, inset, getWidth() - inset, getHeight() - inset);

        ring.setColor(Theme.alpha(0xFFFFFF, 0x1E));
        cv.drawArc(box, 0, 360, false, ring);

        ring.setColor(Theme.ACCENT);
        cv.drawArc(box, -90, 360 * progress, false, ring);
    }
}
