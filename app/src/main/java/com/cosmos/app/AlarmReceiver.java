package com.cosmos.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver {

    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : "";

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            AlarmScheduler.rescheduleAll(context);
            return;
        }

        // Acquire WakeLock to turn on screen
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE, "Cosmos:AlarmWakeLock");
            wakeLock.acquire(60000); // 1 minute safety timeout
        }

        String alarmId = intent.getStringExtra("alarm_id");

        Intent ringIntent = new Intent(context, AlarmRingingActivity.class);
        ringIntent.putExtra("alarm_id", alarmId);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        context.startActivity(ringIntent);

        // Schedule next alarm
        AlarmScheduler.rescheduleAll(context);
    }

    public static void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {}
            wakeLock = null;
        }
    }
}
