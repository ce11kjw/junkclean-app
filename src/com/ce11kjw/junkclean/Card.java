package com.ce11kjw.junkclean;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * 双层机加工外壳容器。
 *
 * 外层是「铝合金托盘」（半透明底 + 发丝边），内层是嵌进去的玻璃面板
 * （独立底色 + 顶部内高光）。同心圆角由外半径减内边距算出，
 * 随手给两个圆角会露馅。
 *
 * addView 自动转发到内芯，因此调用方按普通 LinearLayout 使用即可。
 */
public class Card extends LinearLayout {

    private final LinearLayout coreLayout;
    private boolean ready;

    public Card(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setBackground(Theme.shell(c));
        int p = Theme.dp(c, Theme.SHELL_PAD);
        setPadding(p, p, p, p);

        coreLayout = new LinearLayout(c);
        coreLayout.setOrientation(VERTICAL);
        coreLayout.setBackground(Theme.core(c));
        int cp = Theme.dp(c, 18);
        coreLayout.setPadding(cp, Theme.dp(c, 16), cp, Theme.dp(c, 16));
        super.addView(coreLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ready = true;
    }

    public LinearLayout core() { return coreLayout; }

    /** 所有 addView 重载最终都走这里，统一转发给内芯 */
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
}
