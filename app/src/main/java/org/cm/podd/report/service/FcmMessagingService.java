package org.cm.podd.report.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.HomeActivity;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.util.SharedPrefUtil;

import java.util.Map;

public class FcmMessagingService extends FirebaseMessagingService {
    public static final String TAG = "FcmMessagingService";
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String from = remoteMessage.getFrom();
        Log.d(TAG, "fcm remoteMessage: " + from);

        Map bundle = remoteMessage.getData();

        if (!bundle.isEmpty()) {
            String payload = (String) bundle.get("message");
            String payloadType = (String) bundle.get("type");

            SharedPrefUtil pref = new SharedPrefUtil(getApplicationContext());
            if (pref.isUserLoggedIn() && payloadType != null) {
                if (payloadType.equals("news") || payloadType.equals("nearby")) {

                    String prefix = payloadType.equals("news") ? "แจ้งข่าว" : "รายงาน";

                    // Save notification
                    NotificationDataSource notificationDataSource = new NotificationDataSource(getApplicationContext());

                    String strippedHtml = payload.replaceAll("<script.*?</script>", "");
                    String payloadStr = android.text.Html.fromHtml(strippedHtml).toString();

                    int len = 30;
                    if (payloadStr.length() < len) {
                        len = payloadStr.length();
                    }
                    String title = prefix + ": " + payloadStr.substring(0, len) + "...";

                    long id = notificationDataSource.save(title, payload);
                    notificationDataSource.close();

                    // Post notification of received message.
                    sendNotification(id, title, payload);

                    // refresh notification list and drawer/actionbar counter
                    sendBroadcast(new Intent(HomeActivity.RECEIVE_MESSAGE_ACTION));

                } else if (payloadType.equals("followup")) {

                    // format => reportId@reportType@title@content
                    String[] arr = payload.split("@");
                    if (arr.length == 4) {
                        try {

                            long reportId = Long.parseLong(arr[0]);
                            long reportType = Long.parseLong(arr[1]);
                            String title = arr[2];
                            String message = arr[3];
                            FollowAlertService.notifyMessage(this, title, message, reportId, reportType);

                        } catch (NumberFormatException e) {
                            Log.e(TAG, String.format("Can't parse reportId or reporType [%s, %s]", arr[0], arr[1]));
                        }

                    } else {
                        Log.e(TAG, String.format("Argument mismatch: require 4 get %d", arr.length));
                    }


                } else if (payloadType.equals("updated_report_type")) {
                    ReportQueueDataSource dataSource = new ReportQueueDataSource(getApplicationContext());
                    dataSource.addUpdateTypeQueue();
                    dataSource.close();

                    // Broadcasts the Intent to network receiver
                    Intent updateIntent = new Intent(DataSubmitService.ACTION_REPORT_SUBMIT);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(updateIntent);
                } else {
                    Log.e(TAG, String.format("Unhandle gcm message: type = %s, message = %s", payloadType, payload));
                }
            }
        } else {
            Log.e(TAG, "bundle is empty");
        }

        // Check if message contains a notification payload.
//        if (remoteMessage.getNotification() != null) {
//            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
//        }
//
//        RemoteMessage.Notification noti = remoteMessage.getNotification();
//        sendNotification(remoteMessage.getSentTime(), noti.getTitle(), noti.getBody());
    }

    private void sendNotification(long id, String title, String content) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setAction("org.cm.podd.report.GCM_NOTIFICATION");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("content", content);
        bundle.putLong("id", id);
        intent.putExtras(bundle);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("PODD Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(title))
                        .setContentText(title)
                        .setSound(alarmSound)
                        .setAutoCancel(true);

        mBuilder.setContentIntent(contentIntent);

        String channelId = "fcm_notification_channel_id";

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);

            channel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            // add channel
            mBuilder.setChannelId(channelId);
        }

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}