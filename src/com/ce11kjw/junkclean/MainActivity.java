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
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Theme.BG);
        w.setNavigationBarColor(Theme.BG);

        store = new Store(this);

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

        switchTab(0);
        requestStorageIfNeeded();
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
        View v;
        if (idx == 0) v = home.view();
        else if (idx == 1) v = tools.view();
        else v = settings.view();
        content.addView(v, new FrameLayout.LayoutParams(UI.MP, UI.MP));
        if (idx == 2) settings.refresh();
    }

    /** 有 root 时不必申请；无 root 需要全文件访问才能扫 sdcard */
    private void requestStorageIfNeeded() {
        if (Shell.hasRoot()) return;
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
}
