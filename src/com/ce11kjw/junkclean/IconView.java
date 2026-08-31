package com.ce11kjw.junkclean;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Lucide 矢量图标加载器。
 * 用法：IconView.of(ctx, "ic_cache", 18, color) → ImageView
 * 回退：未知名字时返回 null，调用方按文本处理。
 */
public class IconView {

    /** 按资源名加载（如 ic_cache），找不到返回 null */
    public static Drawable drawable(Context ctx, String name) {
        if (name == null || name.isEmpty()) return null;
        int id = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
        if (id == 0) return null;
        return ctx.getResources().getDrawable(id);
    }

    /** 创建 ImageView，大小与颜色可调。找不到 drawable 返回 null（调用方回退文本） */
    public static ImageView of(Context ctx, String name, int dp, int color) {
        Drawable d = drawable(ctx, name);
        if (d == null) return null;
        ImageView iv = new ImageView(ctx);
        int px = Theme.dp(ctx, dp);
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(px, px);
        lp.topMargin = Theme.dp(ctx, 2);
        lp.bottomMargin = Theme.dp(ctx, 2);
        iv.setLayoutParams(lp);
        d.setTint(color);
        iv.setImageDrawable(d);
        return iv;
    }

    /** 设置已有 ImageView 的图标 + 颜色 */
    public static void set(ImageView iv, Context ctx, String name, int color) {
        Drawable d = drawable(ctx, name);
        if (d != null) {
            d.setTint(color);
            iv.setImageDrawable(d);
        }
    }
}
