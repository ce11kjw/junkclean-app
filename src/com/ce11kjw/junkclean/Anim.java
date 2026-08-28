package com.ce11kjw.junkclean;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** 动效编排：入场交错、按下物理反馈、数字滚动、转场 */
public final class Anim {

    private Anim() {}

    /** 元素入场：淡入 + 上移，可指定延迟做交错 */
    public static void enter(View v, long delayMs) {
        v.setAlpha(0f);
        v.setTranslationY(Theme.dp(v.getContext(), 20));
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(520)
                .setInterpolator(Theme.standard())
                .start();
    }

    /** 容器内所有直接子 View 依次入场，每个交错 stepMs */
    public static void stagger(ViewGroup group, long startDelay, long stepMs) {
        for (int i = 0; i < group.getChildCount(); i++) {
            enter(group.getChildAt(i), startDelay + i * stepMs);
        }
    }

    /** Tab 转场：旧页淡出下移，新页淡入上移 */
    public static void swapIn(View v) {
        v.setAlpha(0f);
        v.setTranslationY(Theme.dp(v.getContext(), 12));
        v.animate().alpha(1f).translationY(0f)
                .setDuration(280)
                .setInterpolator(Theme.decel())
                .start();
    }

    /**
     * 按下物理反馈：缩放 + 轻微压暗。
     * 所有可点元素都该有，否则点上去像点在图片上。
     */
    public static void pressable(final View v, final float scale) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(scale).scaleY(scale).alpha(0.9f)
                                .setDuration(110).setInterpolator(Theme.press()).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                                .setDuration(220).setInterpolator(Theme.spring()).start();
                        break;
                }
                return false;   // 不拦截，onClick 照常触发
            }
        });
    }

    public static void pressable(View v) {
        pressable(v, 0.97f);
    }

    /** 列表项按下：背景切换 + 轻缩放 */
    public static void pressableItem(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        view.setBackground(Theme.item(view.getContext(), true));
                        view.animate().scaleX(0.985f).scaleY(0.985f)
                                .setDuration(100).setInterpolator(Theme.press()).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.setBackground(null);
                        view.animate().scaleX(1f).scaleY(1f)
                                .setDuration(200).setInterpolator(Theme.spring()).start();
                        break;
                }
                return false;
            }
        });
    }

    /** 勾选弹性：0.8 → 1.05 → 1 */
    public static void tick(View v) {
        v.setScaleX(0.8f);
        v.setScaleY(0.8f);
        v.animate().scaleX(1f).scaleY(1f)
                .setDuration(260).setInterpolator(Theme.spring()).start();
    }

    /** 体积数字滚动。等宽字体下宽度不跳 */
    public static void countSize(final TextView tv, final long from, final long to) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(680);
        a.setInterpolator(Theme.standard());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator an) {
                float t = an.getAnimatedFraction();
                tv.setText(Util.fmtSize((long) (from + (to - from) * t)));
            }
        });
        a.start();
    }

    /** 百分比滚动，保留一位小数 */
    public static void countPercent(final TextView tv, final float from, final float to) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(680);
        a.setInterpolator(Theme.standard());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator an) {
                tv.setText(String.format(java.util.Locale.US, "%.1f%%",
                        ((Float) an.getAnimatedValue()).floatValue()));
            }
        });
        a.start();
    }

    /** 整数滚动 */
    public static void countInt(final TextView tv, final int from, final int to, final String suffix) {
        ValueAnimator a = ValueAnimator.ofInt(from, to);
        a.setDuration(560);
        a.setInterpolator(Theme.standard());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator an) {
                tv.setText(String.valueOf(an.getAnimatedValue()) + suffix);
            }
        });
        a.start();
    }

    /** 扫描中的边框呼吸：透明度往复 */
    public static ValueAnimator breathe(final View v) {
        ValueAnimator a = ValueAnimator.ofFloat(1f, 0.55f);
        a.setDuration(900);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(Theme.standard());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator an) {
                v.setAlpha(((Float) an.getAnimatedValue()).floatValue());
            }
        });
        a.start();
        return a;
    }

    /** 对话框入场：0.94 缩放 + 淡入 */
    public static void dialogIn(View v) {
        v.setScaleX(0.94f);
        v.setScaleY(0.94f);
        v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(320).setInterpolator(Theme.standard()).start();
    }
}
