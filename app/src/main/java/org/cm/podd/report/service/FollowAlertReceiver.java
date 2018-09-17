package org.cm.podd.report.service;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;

public class FollowAlertReceiver extends BroadcastReceiver {
    public static final String TAG = "FollowAlertReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive with intent = " + intent.getAction());
        if (action != null && action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "ACTION_BOOT_COMPLETED");
            Intent rescheduleIntent = new Intent(context, FollowAlertRescheduleService.class);
            FollowAlertRescheduleService.enqueueWork(context, rescheduleIntent);
        } else {
            ComponentName comp = new ComponentName(context.getPackageName(),
                    FollowAlertService.class.getName());
            FollowAlertService.enqueueWork(context, intent.setComponent(comp));
            setResultCode(Activity.RESULT_OK);
        }
    }

    public static void scheduleNotificationAlert(Context context, long date, Long reportId, Long reportType, String message, int requestCode) {
        Intent intent = new Intent(context, FollowAlertReceiver.class);
        intent.putExtra("reportId", reportId);
        intent.putExtra("reportType", reportType);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 0);

        Log.d(TAG, String.format("scheduleNotificationAlert: current time %d, schedule time %d with reportId %d and reportType %d, message %s", new Date().getTime(), date, reportId, reportType, message));

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);
    }
}
