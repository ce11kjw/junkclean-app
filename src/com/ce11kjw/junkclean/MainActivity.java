package com.ce11kjw.junkclean;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String VERSION = "3.0.13";

    private FrameLayout content;
    private static final int TAB_N = 5;
    private final LinearLayout[] tabs = new LinearLayout[TAB_N];
    private HomePage home;
    private ToolsPage tools;
    private FilesPage files;
    private StatsPage stats;
    private SettingsPage settings;
    Store store;
    private int current = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        applyThemeFromStore();
        build();
        requestStorageIfNeeded();
        // 启动时按设置清理过期回收站项
        final int days = store.trashDays();
        if (days > 0) {
            new Thread(new Runnable() {
                public void run() { Trash.autoClean(days); }
            }).start();
        }
    }

    /** 构建整个界面（主题切换时重建） */
    private void build() {
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Theme.BG);
        w.setNavigationBarColor(Theme.BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View dv = w.getDecorView();
            int flags = dv.getSystemUiVisibility();
            if (Theme.light) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            dv.setSystemUiVisibility(flags);
        }

        LinearLayout root = UI.col(this);
        android.graphics.drawable.Drawable bg = Wallpaper.drawable(this);
        if (bg != null) root.setBackground(bg);
        else root.setBackgroundColor(Theme.BG);
        root.setFitsSystemWindows(true);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(UI.MP, 0, 1f));
        root.addView(buildTabBar());
        setContentView(root);

        home = new HomePage(this);
        tools = new ToolsPage(this);
        files = new FilesPage(this);
        stats = new StatsPage(this);
        settings = new SettingsPage(this);
        current = -1;
        switchTab(0);
    }

    /** 主题/壁纸切换重建时清掉旧的 Handler 队列，避免旧 Runnable 持有旧实例 */
    private void clearPendingCallbacks() {
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    /** 壁纸变更后重建界面 */
    void applyWallpaperAndRebuild() {
        clearPendingCallbacks();
        Wallpaper.invalidate();
        applyThemeFromStore();
        build();
        switchTab(4);
    }

    /** 从 Store 同步玻璃/主题应用到 Theme，无壁纸时关闭玻璃 */
    private void applyThemeFromStore() {
        Theme.apply(store.theme(), store.accent(), store.glass(), store.glassBlur());
        Theme.glass = Wallpaper.exists(this) ? store.glass() : 0f;
    }

    /** 主题变更后重建界面 */
    void applyThemeAndRebuild() {
        clearPendingCallbacks();
        applyThemeFromStore();
        Wallpaper.invalidate();
        build();
        switchTab(4);
        toast("外观已更新");
    }

    private static final String[] TAB_ICON = {"◉", "◈", "▤", "▦", "⚙"};
    private static final String[] TAB_NAME = {"首页", "工具", "文件", "统计", "设置"};

    /** 浮空玻璃药丸导航：脱离底边，激活项有药丸背景与文字标签 */
    private LinearLayout buildTabBar() {
        LinearLayout outer = UI.row(this);
        int m = Theme.dp(this, Theme.S3);
        outer.setPadding(m, 0, m, m);

        LinearLayout bar = UI.row(this);
        bar.setBackground(Theme.navBar(this));
        int pv = Theme.dp(this, 5);
        bar.setPadding(pv, pv, pv, pv);

        for (int i = 0; i < TAB_N; i++) {
            final int idx = i;
            LinearLayout tab = UI.col(this);
            tab.setGravity(android.view.Gravity.CENTER);
            int tp = Theme.dp(this, Theme.S2);
            tab.setPadding(tp, Theme.dp(this, 7), tp, Theme.dp(this, 6));

            TextView icon = UI.text(this, TAB_ICON[i], 15, Theme.DIM);
            icon.setGravity(android.view.Gravity.CENTER);
            tab.addView(icon);

            TextView label = UI.text(this, TAB_NAME[i], Theme.T_MICRO, Theme.DIM);
            label.setTypeface(Theme.micro());
            label.setLetterSpacing(0.08f);
            label.setGravity(android.view.Gravity.CENTER);
            tab.addView(label, UI.lpm(this, UI.WC, UI.WC, 3));

            tab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { switchTab(idx); }
            });
            Anim.pressable(tab, 0.94f);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            bar.addView(tab, p);
            tabs[i] = tab;
        }

        outer.addView(bar, new LinearLayout.LayoutParams(UI.MP, UI.WC));
        return outer;
    }

    /** 激活态：药丸背景 + 强调色，图标轻微上浮 */
    private void setTabActive(int i, boolean active) {
        LinearLayout tab = tabs[i];
        if (tab == null) return;
        tab.setBackground(active ? Theme.navPill(this) : null);
        int color = active ? Theme.ACCENT : Theme.DIM;
        for (int k = 0; k < tab.getChildCount(); k++) {
            View v = tab.getChildAt(k);
            if (v instanceof TextView) ((TextView) v).setTextColor(color);
        }
        View icon = tab.getChildAt(0);
        icon.animate().translationY(active ? -Theme.dp(this, 1) : 0)
                .scaleX(active ? 1.08f : 1f).scaleY(active ? 1.08f : 1f)
                .setDuration(260).setInterpolator(Theme.spring()).start();
    }

    void switchTab(int idx) {
        if (current == idx) return;
        current = idx;
        for (int i = 0; i < TAB_N; i++) setTabActive(i, i == idx);
        content.removeAllViews();
        View v;
        switch (idx) {
            case 1:  v = tools.view();    break;
            case 2:  v = files.view();    break;
            case 3:  v = stats.view();    stats.refresh();    break;
            case 4:  v = settings.view(); settings.refresh(); break;
            default: v = home.view();
                     home.refreshDisk(); home.refreshStat(); break;
        }
        content.addView(v, new FrameLayout.LayoutParams(UI.MP, UI.MP));
        v.scrollTo(0, 0);
        Anim.swapIn(v);
    }

    private void requestStorageIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                toast("需要「所有文件访问权限」才能扫描存储");
                try {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                } catch (Exception ignored) {}
            }
        } else {
            requestPermissions(new String[]{
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
        }
    }

    /** 是否已获得应用列表权限（QUERY_ALL_PACKAGES 在部分 ROM 需运行时确认） */
    boolean hasPackagePermission() {
        try {
            return getPackageManager().getInstalledApplications(0).size() > 12;
        } catch (Exception e) {
            return false;
        }
    }

    boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE")
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** 引导用户处理应用列表权限：可读则提示已可用，否则跳应用详情页 */
    void requestPackagePermission() {
        if (hasPackagePermission()) {
            toast("应用列表已可读取（共 " + safeAppCount() + " 个）");
            return;
        }
        toast("请在应用信息中允许读取应用列表");
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    private int safeAppCount() {
        try { return getPackageManager().getInstalledApplications(0).size(); }
        catch (Exception e) { return 0; }
    }

    void toast(String s) {
        Toast t = Toast.makeText(this, s, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, Theme.dp(this, 90));
        t.show();
    }

    HomePage homePage() { return home; }

    @Override
    public void onBackPressed() {
        if (current != 0) { switchTab(0); return; }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        clearPendingCallbacks();
        Wallpaper.invalidate();
        Util.clearSizeCache();
        super.onDestroy();
    }
}
