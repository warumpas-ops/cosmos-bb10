package com.cosmos.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

public class AlarmScheduler {

    public static void rescheduleAll(Context context) {
        AlarmStorage storage = new AlarmStorage(context);
        List<Alarm> alarms = storage.loadAlarms();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // Cancel previous pending alarm intent
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.cosmos.app.ALARM_TRIGGER");
        PendingIntent pi = PendingIntent.getBroadcast(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);

        // Find the soonest enabled alarm
        Alarm nextAlarm = null;
        long soonestMs = Long.MAX_VALUE;

        for (Alarm a : alarms) {
            if (a.enabled) {
                long trigger = a.calculateNextTriggerMs();
                if (trigger < soonestMs) {
                    soonestMs = trigger;
                    nextAlarm = a;
                }
            }
        }

        if (nextAlarm != null && soonestMs < Long.MAX_VALUE) {
            Intent triggerIntent = new Intent(context, AlarmReceiver.class);
            triggerIntent.setAction("com.cosmos.app.ALARM_TRIGGER");
            triggerIntent.putExtra("alarm_id", nextAlarm.id);
            PendingIntent triggerPi = PendingIntent.getBroadcast(context, 1001, triggerIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (Build.VERSION.SDK_INT >= 19) {
                am.setExact(AlarmManager.RTC_WAKEUP, soonestMs, triggerPi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, soonestMs, triggerPi);
            }
        }
    }
}
