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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.cm.podd.report.model.Queue;
import org.cm.podd.report.model.ReportImage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ReportQueueDataSource {
    public static final String DATA_TYPE = "report_data";
    public static final String IMAGE_TYPE = "report_image";
    public static final String UPDATE_REPORT_TYPE = "update_report_type";
    private static final String TAG = "ReportQueueDataSource";

    private ReportDatabaseHelper mDbHelper;
    private ReportDataSource mReportDataSource;

    public ReportQueueDataSource(Context context) {
        mDbHelper = new ReportDatabaseHelper(context);
        mReportDataSource = new ReportDataSource(context);
    }

    public void addDataQueue(long reportId) {
        Log.d(TAG, "add queue report id=" + reportId);
        String guid = String.valueOf(UUID.randomUUID());
        mReportDataSource.assignGuid(reportId, DATA_TYPE, guid);
        insertQueue(reportId, 0, DATA_TYPE);
    }

    public void addImageQueue(long reportId) {
        Log.d(TAG, "add image queue for report id=" + reportId);
        // loop thru all images that are not in a queue list
        List<ReportImage> images = mReportDataSource.getSubmitPendingImages(reportId);
        for (ReportImage image : images) {
            long imageId = image.getId();

            String guid = String.valueOf(UUID.randomUUID());
            mReportDataSource.assignGuid(imageId, IMAGE_TYPE, guid);

            insertQueue(reportId, imageId, IMAGE_TYPE);
        }
    }

    public void addUpdateTypeQueue() {
        insertQueue(0, 0, UPDATE_REPORT_TYPE);
    }

    private void insertQueue(long reportId, long imageId, String dataType) {
        Log.d(TAG, "insert queue type=" + dataType);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        StringBuilder sql = new StringBuilder("insert into report_queue ");
        sql.append("(report_id, image_id, data_type, created_at) ")
            .append("select ?, ?, ?, ? where not exists (select 1 from report_queue ");

        if (dataType.equals(IMAGE_TYPE)) {
            sql.append(" where image_id = ? and data_type = ?)");
            db.execSQL(sql.toString(), new Object[]{
                    reportId, imageId, dataType, new Date().getTime(), imageId, dataType
            });
        } else {
            sql.append(" where report_id = ? and data_type = ?)");
            db.execSQL(sql.toString(), new Object[]{
                    reportId, imageId, dataType, new Date().getTime(), reportId, dataType
            });
        }
        db.close();
    }

    public List<Queue> getAllQueues() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<Queue> queues = new ArrayList<Queue>();

        Cursor cursor = db.rawQuery("select * from report_queue order by created_at asc", null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("_id"));
            String type = cursor.getString(cursor.getColumnIndex("data_type"));
            long reportId = cursor.getLong(cursor.getColumnIndex("report_id"));
            long imageId = cursor.getLong(cursor.getColumnIndex("image_id"));
            long createdAt = cursor.getLong(cursor.getColumnIndex("created_at"));

            Queue q = new Queue(reportId, imageId, type, createdAt);
            q.setId(id);
            queues.add(q);
        }
        cursor.close();
        db.close();
        return queues;
    }

    public void remove(long id) {
        Log.d(TAG, "remove queue id=" + id);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete("report_queue", "_id = ?", new String[] {Long.toString(id)});
        db.close();
    }

    public void deleteByReportId(long id) {
        Log.d(TAG, "remove queue with report id=" + id);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete("report_queue", "report_id = ?", new String[] {Long.toString(id)});
        db.close();
    }

    public void close() {
        mDbHelper.close();
    }
}
