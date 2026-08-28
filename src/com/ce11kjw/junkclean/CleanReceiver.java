package com.ce11kjw.junkclean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * 定时清理触发：到点广播 → 后台扫描清理 → toast 报告。
 *
 * 不弹 Activity，只跑在后台进程。开机后由 BOOT_COMPLETED 重新调度。
 */
public class CleanReceiver extends BroadcastReceiver {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    @Override
    public void onReceive(final Context ctx, Intent intent) {
        // 用 goAsync() 让接收器在异步任务结束前不被回收
        final android.content.BroadcastReceiver.PendingResult pr = goAsync();

        new Thread(new Runnable() {
            public void run() {
                try {
                    Store s = new Store(ctx);
                    if (!s.scheduleEnabled()) return;
                    long t0 = System.currentTimeMillis();
                    ScanEngine eng = new ScanEngine(ctx, s);
                    final java.util.List<JunkCategory> cats =
                            eng.scan(true, new ScanEngine.Progress() {
                        public void onCategory(String name, int idx, int total,
                                               int items, long bytes) {}
                        public boolean cancelled() { return false; }
                    });
                    final CleanEngine.Result r = new CleanEngine(s.toTrash()).clean(cats);
                    s.addStat(r.freed, r.count);
                    long cost = (System.currentTimeMillis() - t0) / 1000;
                    HANDLER.post(new Runnable() {
                        public void run() {
                            String msg = "定时清理完成 · " + r.count + " 项 · "
                                    + Util.fmtSize(r.freed) + " · 耗时 " + cost + "s";
                            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    pr.finish();
                }
            }
        }).start();
    }

    /** 主类主动触发清理（手动运行） */
    public static void runOnce(Context ctx) {
        Intent i = new Intent(ctx, CleanReceiver.class);
        i.setAction("com.ce11kjw.junkclean.SCHEDULED_CLEAN");
        ctx.sendBroadcast(i);
    }
}
