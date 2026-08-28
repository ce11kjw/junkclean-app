package com.ce11kjw.junkclean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 开机后自动恢复定时清理调度。
 * AlarmManager 会被关机清空，必须在 BOOT_COMPLETED 时重新注册。
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent i) {
        Store s = new Store(ctx);
        if (s.scheduleEnabled()) ScheduleManager.apply(ctx, s);
    }
}
