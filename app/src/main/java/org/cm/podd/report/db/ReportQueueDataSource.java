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
package org.cm.podd.report.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Date;
import java.util.UUID;

public class ReportQueueDataSource {
    private static final String DATA_TYPE = "report_data";
    private static final String IMAGE_TYPE = "report_image";
    private static final String TAG = "ReportQueueDataSource";

    private ReportDatabaseHelper mDbHelper;
    private ReportDataSource mReportDataSource;

    public ReportQueueDataSource(Context context) {
        mDbHelper = new ReportDatabaseHelper(context);
        mReportDataSource = new ReportDataSource(context);
    }

    public void addQueue(long reportId) {
        Log.d(TAG, "add queue report id=" + reportId);
        String guid = String.valueOf(UUID.randomUUID());
        mReportDataSource.assignGuid(reportId, guid);
        insertQueue(reportId, DATA_TYPE, guid);
        insertQueue(reportId, IMAGE_TYPE, guid);
    }

    private void insertQueue(long reportId, String dataType, String guid) {
        Log.d(TAG, "insert queue type=" + dataType);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("report_id", reportId);
        values.put("data_type", dataType);
        values.put("created_at", new Date().getTime());
        values.put("guid", guid);
        db.insert("report_queue", null, values);
        db.close();
    }

    public Cursor getAllQueues() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        return db.rawQuery("select * from report_queue", null);
    }
}
