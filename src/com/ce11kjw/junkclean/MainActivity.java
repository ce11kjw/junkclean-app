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
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String VERSION = "2.2.0";

    private FrameLayout content;
    private static final int TAB_N = 5;
    private final Button[] tabs = new Button[TAB_N];
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
        Theme.apply(store.theme(), store.accent());
        Theme.glass = Wallpaper.exists(this);
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

    /** 壁纸变更后重建界面 */
    void applyWallpaperAndRebuild() {
        Wallpaper.invalidate();
        Theme.glass = Wallpaper.exists(this);
        build();
        switchTab(4);
    }

    /** 主题变更后重建界面 */
    void applyThemeAndRebuild() {
        Theme.apply(store.theme(), store.accent());
        Theme.glass = Wallpaper.exists(this);
        Wallpaper.invalidate();
        build();
        switchTab(4);
        toast("外观已更新");
    }

    private LinearLayout buildTabBar() {
        LinearLayout bar = UI.row(this);
        bar.setBackground(Theme.navBar(this));
        int pv = Theme.dp(this, 6);
        bar.setPadding(Theme.dp(this, 10), pv, Theme.dp(this, 10), pv);

        String[] labels = {"🏠", "🧰", "📂", "📊", "⚙"};
        for (int i = 0; i < TAB_N; i++) {
            final int idx = i;
            Button t = new Button(this);
            t.setText(labels[i]);
            t.setTextSize(15f);
            t.setAllCaps(false);
            t.setStateListAnimator(null);
            t.setMinHeight(Theme.dp(this, 38));
            t.setMinimumHeight(Theme.dp(this, 38));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { switchTab(idx); }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            p.leftMargin = p.rightMargin = Theme.dp(this, 2);
            bar.addView(t, p);
            tabs[i] = t;
        }
        return bar;
    }

    void switchTab(int idx) {
        if (current == idx) return;
        current = idx;
        for (int i = 0; i < TAB_N; i++) UI.setChipActive(this, tabs[i], i == idx);
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
}
