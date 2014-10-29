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

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.model.ReportImage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DataSubmitService extends IntentService {

    private static final String TAG = "DataSubmitService";

    public DataSubmitService() {
        super(null);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public DataSubmitService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        HttpURLConnection conn = null;
        Log.i(TAG, "submit data " + Long.toString(System.currentTimeMillis()));

        ReportQueueDataSource queueDataSource = new ReportQueueDataSource(getApplicationContext());
        Cursor cursor = queueDataSource.getAllQueues();
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("_id"));
            String type = cursor.getString(cursor.getColumnIndex("data_type"));
            long reportId = cursor.getLong(cursor.getColumnIndex("report_id"));
            Log.d(TAG, String.format("type= %s, report id= %d", type, reportId));

            // TODO
            // get report data
            // get image byte
            // submit to server
            // mark submit=1
            // remove queue
            // handle exception
        }
        cursor.close();

        Intent networkIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        networkIntent.putExtra("SubmitDone", true);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(networkIntent);

        /*try {
            URL url = new URL("http://outlook.com");
            conn = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(conn.getInputStream());
            Log.d("http resp", convertInputStream2String(in));
        } catch (Exception e ) {
            Log.e("dashboard_service", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }*/
    }

    public static String convertInputStream2String(InputStream in) {
        StringBuffer sb = new StringBuffer();
        if (in == null) {
            return sb.toString();
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in,
                    "utf-8"), 8);
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
