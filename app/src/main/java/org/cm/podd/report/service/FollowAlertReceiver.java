package org.cm.podd.report.service;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import org.cm.podd.report.db.FollowAlertDataSource;
import org.cm.podd.report.db.ReportDataSource;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class FollowAlertReceiver extends WakefulBroadcastReceiver {
    public static final String TAG = "FollowAlertReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            FollowAlertDataSource followAlertDataSource = new FollowAlertDataSource(context);
            ReportDataSource reportDataSource = new ReportDataSource(context);
            Cursor triggers = followAlertDataSource.getAll();
            while (triggers.moveToNext()) {
                long date = triggers.getLong(triggers.getColumnIndex("date"));
                long reportId = triggers.getLong(triggers.getColumnIndex("report_id"));
                String message = triggers.getString(triggers.getColumnIndex("message"));
                long reportType = triggers.getLong(triggers.getColumnIndex("report_type"));
                int requestCode = triggers.getInt(triggers.getColumnIndex("request_code"));

                scheduleNotificationAlert(context, date, reportId, reportType, message, requestCode);
            }


        } else {
            ComponentName comp = new ComponentName(context.getPackageName(),
                    FollowAlertService.class.getName());
            // Start the service, keeping the device awake while it is launching.
            startWakefulService(context, (intent.setComponent(comp)));
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
