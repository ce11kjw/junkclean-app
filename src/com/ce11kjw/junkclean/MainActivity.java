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

    public static final String VERSION = "2.0.0";

    private FrameLayout content;
    private final Button[] tabs = new Button[3];
    private HomePage home;
    private ToolsPage tools;
    private SettingsPage settings;
    Store store;
    private int current = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        Theme.apply(store.theme(), store.accent());
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
        root.setBackgroundColor(Theme.BG);
        root.setFitsSystemWindows(true);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(UI.MP, 0, 1f));
        root.addView(buildTabBar());
        setContentView(root);

        home = new HomePage(this);
        tools = new ToolsPage(this);
        settings = new SettingsPage(this);
        current = -1;
        switchTab(0);
    }

    /** 主题变更后重建界面 */
    void applyThemeAndRebuild() {
        Theme.apply(store.theme(), store.accent());
        build();
        switchTab(2);
        toast("外观已更新");
    }

    private LinearLayout buildTabBar() {
        LinearLayout bar = UI.row(this);
        bar.setBackgroundColor(Theme.BG2);
        int pv = Theme.dp(this, 6);
        bar.setPadding(Theme.dp(this, 10), pv, Theme.dp(this, 10), pv);

        String[] labels = {"🏠  首页", "🧰  工具箱", "⚙  设置"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Button t = new Button(this);
            t.setText(labels[i]);
            t.setTextSize(12.5f);
            t.setAllCaps(false);
            t.setStateListAnimator(null);
            t.setMinHeight(Theme.dp(this, 40));
            t.setMinimumHeight(Theme.dp(this, 40));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { switchTab(idx); }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, UI.WC, 1f);
            p.leftMargin = p.rightMargin = Theme.dp(this, 3);
            bar.addView(t, p);
            tabs[i] = t;
        }
        return bar;
    }

    void switchTab(int idx) {
        if (current == idx) return;
        current = idx;
        for (int i = 0; i < 3; i++) UI.setChipActive(this, tabs[i], i == idx);
        content.removeAllViews();
        View v = idx == 0 ? home.view() : idx == 1 ? tools.view() : settings.view();
        content.addView(v, new FrameLayout.LayoutParams(UI.MP, UI.MP));
        if (idx == 2) settings.refresh();
        if (idx == 0) { home.refreshDisk(); home.refreshStat(); }
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
