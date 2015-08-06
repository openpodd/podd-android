package org.cm.podd.report.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.cm.podd.report.R;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.FollowAlertDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class FollowAlertScheduleService {
    private static String TAG = "FollowAlertScheduleService";

    public static class SetFollowAlertScheduleTask extends AsyncTask<Void, Void, Void> {

        private Context context;
        private String pattern;
        private String notificationText;
        private long reportId;
        private long reportType;
        private int startPageId;

        public SetFollowAlertScheduleTask (Context context, String pattern, String notificationText, long reportId, long reportType, int startPageId){
            this.context = context;
            this.pattern = pattern;
            this.notificationText = notificationText;
            this.reportId = reportId;
            this.reportType = reportType;
            this.startPageId = startPageId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            FollowAlertDataSource followAlertDataSource = new FollowAlertDataSource(context);

            if (pattern == null)
                return null;

            for (int i = 0; i < pattern.length(); i++ ) {
                char ch = pattern.charAt(i);

                if (ch == '1') {

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, (i + 1));

                    int hour = Integer.parseInt(context.getString(R.string.start_alert_hour));
                    int frequency = Integer.parseInt(context.getString(R.string.frequency_alert_hour));
                    int end = (int) (24 / frequency);

                    for (int j = 0; j < end; j++) {

                        if (hour >= 24) {
                            cal.add(Calendar.DATE, 1);
                            hour = hour % 24;
                        }

                        cal.set(Calendar.HOUR_OF_DAY, hour);
                        cal.set(Calendar.MINUTE, 00);
                        cal.set(Calendar.SECOND, 00);

                        int triggerNo = (i + 1);

                        String message = notificationText;

                        long date = cal.getTimeInMillis();
                        int requestCode = setFollowAlert(date, reportId, reportType, startPageId, message);

                        Log.i(TAG, "alert @" + cal.get(Calendar.DATE) + "-" + hour);

                        followAlertDataSource.createFollowAlert(reportId, triggerNo, message, requestCode, date);
                        hour = hour + ( frequency );
                    }
                }
            }

            return null;
        }

        private int setFollowAlert(long date, Long reportId, Long reportType, int startPageId, String message) {

            int requestCode = (new Random()).nextInt();

            Intent intent = new Intent(context, FollowAlertReceiver.class);
            intent.putExtra("reportId", reportId);
            intent.putExtra("reportType", reportType);
            intent.putExtra("message", message);
            intent.putExtra("startPageId", startPageId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 0);

            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);

            return requestCode;
        }

    }

    public static class CancelFollowAlertScheduleTask  extends AsyncTask<Void, Void, Void> {

        private Context context;
        private long reportId;

        public CancelFollowAlertScheduleTask (Context context,long reportId){
            this.context = context;
            this.reportId = reportId;

        }

        @Override
        protected Void doInBackground(Void... params) {

            FollowAlertDataSource followAlertDataSource = new FollowAlertDataSource(context);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 1);

            int hour = Integer.parseInt(context.getString(R.string.start_alert_hour));
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, 00);
            cal.set(Calendar.SECOND, 00);

            int triggerNo = followAlertDataSource.getTriggerNoByNow1Day(reportId, cal.getTimeInMillis());

            List<Integer> requestCodes = followAlertDataSource.getRequestCodes(reportId, triggerNo);
            for (int i = 0; i < requestCodes.size(); i++ ) {
                cancelFollowAlert(requestCodes.get(i));
            }

            Log.i(TAG, "cancel alert:" + requestCodes.size());

            if (requestCodes.size() > 1)
                followAlertDataSource.updateStatusDone(reportId, triggerNo);
            return null;
        }

        private void cancelFollowAlert(int requestCode) {
            try{
                Intent intent = new Intent(context, FollowAlertReceiver.class);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 0);
                AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarm.cancel(pendingIntent);

            }catch (Exception e){
                Log.e(TAG, "error" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}

