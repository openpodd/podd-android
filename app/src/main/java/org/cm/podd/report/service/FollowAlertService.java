package org.cm.podd.report.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.HomeActivity;


public class FollowAlertService extends IntentService {
    public static final int NOTIFICATION_ID = 2;
    public static final String TAG = "FollowAlertService";
    public static final String ORG_CM_PODD_REPORT_FOLLOW = "org.cm.podd.report.REPORT_FOLLOW";

    private NotificationManager mNotificationManager;

    public FollowAlertService() {
        super(FollowAlertService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String message = intent.getStringExtra("message");
        long reportId = intent.getLongExtra("reportId", -1);
        long reportType = intent.getLongExtra("reportType", -1);

        if (message != null && reportId != -1 && reportType != -1)
            notifyMessage(message, reportId, reportType);

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void notifyMessage(String message, long reportId, long reportType) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setAction(ORG_CM_PODD_REPORT_FOLLOW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra("reportId", reportId);
        intent.putExtra("reportType", reportType);
        intent.putExtra("follow", true);

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
