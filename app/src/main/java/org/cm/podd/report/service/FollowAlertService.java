package org.cm.podd.report.service;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.HomeActivity;
import org.cm.podd.report.activity.ReportActivity;
import org.cm.podd.report.activity.ReportTypeActivity;
import org.cm.podd.report.model.ReportType;


public class FollowAlertService extends IntentService {
    public static final int NOTIFICATION_ID = 2;
    public static final String ACTION_CALL = "FollowAlertService.call";
    public static final String TAG = "FollowAlertService";

    private NotificationManager mNotificationManager;

    public FollowAlertService() {
        super(FollowAlertService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String message = intent.getStringExtra("message");
        long reportId = intent.getLongExtra("reportId", -1);
        long reportType = intent.getLongExtra("reportType", -1);
        int startPageId = intent.getIntExtra("startPageId", 0);

        if (message != null && reportId != -1 && reportType != -1)
            sendFollowAlert(message, reportId, reportType, startPageId);

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendFollowAlert(String message, long reportId, long reportType, int startPageId) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, ReportActivity.class);
        intent.setAction("org.cm.podd.report.GCM_NOTIFICATION");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra("reportType", reportType);
        intent.putExtra("reportId", reportId);
        intent.putExtra("follow", true);
        intent.putExtra("startPageId", startPageId);

        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        intent.putExtras(bundle);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("PODD Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentText(message)
                        .setSound(alarmSound)
                        .setAutoCancel(true);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
