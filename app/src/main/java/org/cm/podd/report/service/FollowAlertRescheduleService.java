/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cm.podd.report.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;

import org.cm.podd.report.db.FollowAlertDataSource;

/**
 * Created by pphetra on 8/16/15 AD.
 */
public class FollowAlertRescheduleService extends Service {

    public static final String TAG = "FollowAlertReschedule";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        FollowAlertDataSource followAlertDataSource = new FollowAlertDataSource(context);
        Cursor triggers = followAlertDataSource.getUnDone();
        while (triggers.moveToNext()) {
            long date = triggers.getLong(triggers.getColumnIndex("date"));
            long reportId = triggers.getLong(triggers.getColumnIndex("report_id"));
            String message = triggers.getString(triggers.getColumnIndex("message"));
            long reportType = triggers.getLong(triggers.getColumnIndex("report_type"));
            int requestCode = triggers.getInt(triggers.getColumnIndex("request_code"));

            Log.d(TAG, String.format("schedule for reportId = %d, date = %d", reportId, date));

            FollowAlertReceiver.scheduleNotificationAlert(context, date, reportId, reportType, message, requestCode);
        }

        FollowAlertReceiver.completeWakefulIntent(intent);
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}