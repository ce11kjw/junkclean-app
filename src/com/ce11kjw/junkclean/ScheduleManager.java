package com.ce11kjw.junkclean;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 定时清理调度：AlarmManager 周期触发 CleanReceiver
 *
 * Android 12+ (API 31) 默认不允许 SET_EXACT_ALARM，需要用户手动授权；
 * 这里用 setInexactRepeating 不需要权限，误差 ±几分钟不影响 30 分钟周期。
 *
 * 开机后由系统广播 BOOT_COMPLETED 触发重新调度（AndroidManifest 里注册）。
 */
public class ScheduleManager {

    private static final String ACTION = "com.ce11kjw.junkclean.SCHEDULED_CLEAN";

    public static void apply(Context c, Store s) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pi(c);

        if (!s.scheduleEnabled()) {
            am.cancel(pi);
            return;
        }

        long intervalMs = s.scheduleIntervalMin() * 60_000L;
        long triggerAt = System.currentTimeMillis() + intervalMs;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 不需要 SET_EXACT_ALARM 权限，误差 ±几分钟
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, intervalMs, pi);
        } else {
            am.setRepeating(AlarmManager.RTC_WAKEUP, triggerAt, intervalMs, pi);
        }
        s.setScheduleNextAt(triggerAt);
    }

    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi(c));
    }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(ACTION).setPackage(c.getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, 0, i, flags);
    }
}

