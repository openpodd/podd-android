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
import android.graphics.BitmapFactory;
import android.provider.BaseColumns;
import android.util.Log;

import org.cm.podd.report.model.Report;
import org.cm.podd.report.model.ReportImage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class ReportDataSource {

    private static final String TAG = "ReportDataSource";
    private ReportDatabaseHelper reportDatabaseHelper;

    public ReportDataSource(Context context) {
        reportDatabaseHelper = new ReportDatabaseHelper(context);
    }

    public void close() {
        reportDatabaseHelper.close();
    }

    /**
     * create draft report
     * @return row id of new report
     */
    public long createDraftReport(long type, boolean test) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        int testReport = Report.FALSE;
        if (test) {
            testReport = Report.TRUE;
        }
        ContentValues values = new ContentValues();
        values.put("date", new Date().getTime());
        values.put("type", type);
        values.put("draft", 1);
        values.put("negative", 1);
        values.put("follow_date", Long.MAX_VALUE);
        values.put("test_report", testReport);
        values.put("guid", String.valueOf(UUID.randomUUID()));
        values.put("submit", 0);

        Cursor typeCursor = db.rawQuery("select followable, follow_days from report_type where _id = ?", new String[] {Long.toString(type)});
        if (typeCursor.moveToFirst()) {
            int followable = typeCursor.getInt(typeCursor.getColumnIndex("followable"));
            if (followable == Report.TRUE) {
                int followDays = typeCursor.getInt(typeCursor.getColumnIndex("follow_days"));
                Date today = new Date();
                long until = today.getTime() + (followDays * 24 * 60 * 60 * 1000L);
                Log.d(TAG, String.format("follow case value, followDays = %d until %d", followDays, until));
                values.put("follow_until", until);
            }
        }
        typeCursor.close();

        long id = db.insert("report", null, values);
        db.close();
        return id;
    }

    public long createFollowReport(long type, String parentGuid) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", new Date().getTime());
        values.put("type", type);
        values.put("draft", 1);
        values.put("negative", 1);
        values.put("follow_date", Long.MAX_VALUE);
        values.put("follow_flag", 1);
        values.put("follow_date", new Date().getTime());
        values.put("test_report", false);
        values.put("guid", String.valueOf(UUID.randomUUID()));
        values.put("parent_guid", parentGuid);
        values.put("submit", 0);

        long id = db.insert("report", null, values);
        db.close();
        return id;
    }

    public long createFollowReport(long sourceId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT r.* FROM report r where r._id = ?", new String[]{Long.toString(sourceId)});
        if (cursor.moveToFirst()) {

            long type = cursor.getLong(cursor.getColumnIndex("type"));
            Date date = new Date(cursor.getLong(cursor.getColumnIndex("date")));
            Date startDate = null;
            if (cursor.getInt(cursor.getColumnIndex("start_date")) != 0) {
                long t = cursor.getLong(cursor.getColumnIndex("start_date"));
                startDate = new Date(t);
            }
            long regionId = cursor.getLong(cursor.getColumnIndex("region_id"));
            String formData = cursor.getString(cursor.getColumnIndex("form_data"));
            String guid = cursor.getString(cursor.getColumnIndex("guid"));

            int follow_date = cursor.getInt(cursor.getColumnIndex("follow_date"));
            if (follow_date == 0) {
                db.execSQL("update report set follow_date = ? where _id = ?", new Object[] {Long.MAX_VALUE, sourceId });
            }

            Cursor formCursor = db.rawQuery("select form_data from report where parent_guid = ? order by _id desc limit 1", new String[]{guid});
            if (formCursor.moveToFirst()) {
                formData = formCursor.getString(formCursor.getColumnIndex("form_data"));
            }

            int testReport = cursor.getInt(cursor.getColumnIndex("test_report"));
            formCursor.close();

            ContentValues values = new ContentValues();
            values.put("date", date.getTime());
            values.put("type", type);
            values.put("draft", 1);
            values.put("negative", 1);
            values.put("submit", 0);
            values.put("follow_flag", 1);
            values.put("follow_date", new Date().getTime());
            values.put("form_data", formData);
            if (startDate != null) {
                values.put("start_date", startDate.getTime());
            }
            values.put("region_id", regionId);
            values.put("guid", String.valueOf(UUID.randomUUID()));
            values.put("parent_guid", guid);
            values.put("test_report", testReport);
            long id = db.insert("report", null, values);
            cursor.close();
            db.close();
            return id;
        } else { // parent report had been removed. (maybe from user logout scenario)
            cursor.close();
            db.close();
            return -99;
        }


    }

    /*
     * create report that has no incident;
     */
    public long createPositiveReport() {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", new Date().getTime());
        values.put("draft", 0);
        values.put("negative", 0);
        values.put("submit", 0);
        values.put("guid", String.valueOf(UUID.randomUUID()));
        long id = db.insert("report", null, values);
        db.close();
        return id;
    }

    public Cursor getAll() {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM report order by _id desc", null);
    }

    public Cursor getAllWithTypeName() {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        return db.rawQuery(
                "SELECT r._id, r.type, r.date, r.negative, r.draft, r.submit, rt.name as type_name, r.follow_flag, r.follow_date, r.follow_until, r.test_report, r.action_name FROM report r "
                + "left join report_type rt on r.type = rt._id order by r.date desc, r.follow_date desc", null);
    }

    public Report getById(long id) {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT r.*, rt.version FROM report r left outer join report_type rt on rt._id = r.type where r._id = ?", new String[]{Long.toString(id)});
        Report report = null;
        if (cursor.moveToFirst()) {
            report = createFromCursor(cursor);
        }
        cursor.close();
        db.close();
        return report;
    }

    public Report getByGUID(String guid) {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT r.*, rt.version FROM report r left outer join report_type rt on rt._id = r.type where r.guid = ?", new String[]{guid});
        Report report = null;
        if (cursor.moveToFirst()) {
            report = createFromCursor(cursor);
        }
        cursor.close();
        db.close();
        return report;
    }

    private Report createFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex("_id"));
        long type = cursor.getLong(cursor.getColumnIndex("type"));
        Date date = new Date(cursor.getLong(cursor.getColumnIndex("date")));
        int negative = cursor.getInt(cursor.getColumnIndex("negative"));
        int draft = cursor.getInt(cursor.getColumnIndex("draft"));
        int submit = cursor.getInt(cursor.getColumnIndex("submit"));
        double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
        double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
        Date startDate = null;
        if (cursor.getInt(cursor.getColumnIndex("start_date")) != 0) {
            long t = cursor.getLong(cursor.getColumnIndex("start_date"));
            startDate = new Date(t);
        }
        long regionId = cursor.getLong(cursor.getColumnIndex("region_id"));
        String remark = cursor.getString(cursor.getColumnIndex("remark"));
        String guid = cursor.getString(cursor.getColumnIndex("guid"));
        int testReport = cursor.getInt(cursor.getColumnIndex("test_report"));

        int followFlag = cursor.getInt(cursor.getColumnIndex("follow_flag"));
        Date followDate = new Date(cursor.getLong(cursor.getColumnIndex("follow_date")));
        String parentGuid = cursor.getString(cursor.getColumnIndex("parent_guid"));

        String actionName = cursor.getString(cursor.getColumnIndex("action_name"));

        Report report = new Report(id, type, date, negative, draft, submit);
        report.setFormData(cursor.getString(cursor.getColumnIndex("form_data")));
        report.setLatitude(latitude);
        report.setLongitude(longitude);
        report.setStartDate(startDate);
        report.setRegionId(regionId);
        report.setRemark(remark);
        report.setGuid(guid);
        report.setFollowDate(followDate);
        report.setFollowFlag(followFlag);
        report.setParentGuid(parentGuid);
        report.setTestReport(testReport);
        report.setActionName(actionName);

        report.setReportTypeVersion(cursor.getInt(cursor.getColumnIndex("version")));
        return report;
    }

    public void updateData(long reportId, String jsonStr, int draftFlag) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("form_data", jsonStr);
        values.put("draft", draftFlag);
        db.update("report", values, "_id = ?", new String[]{Long.toString(reportId)});
        db.close();
    }

    public void updateLocation(long reportId, double latitude, double longitude) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        db.update("report", values, "_id = ?", new String[]{Long.toString(reportId)});
        db.close();
    }

    public ReportImage saveImage(long reportId, String imageUri, byte[] bytes) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("report_id", reportId);
        values.put("image_uri", imageUri);
        values.put("image_thumbnail", bytes);
        values.put("submit", 0);
        long id = db.insert("report_image", null, values);
        db.close();

        ReportImage ret = new ReportImage(id, imageUri);
        ret.setThumbnail(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));

        return ret;
    }

    public void deleteReport(long reportId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        db.delete("report", BaseColumns._ID + "=?", new String[] { Long.toString(reportId) });
        db.close();
    }

    public void deleteImage(long imageId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        db.delete("report_image", BaseColumns._ID + "=?", new String[] { Long.toString(imageId) });
        db.close();
    }

    public void deleteImagesByReportId(long reportId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        db.delete("report_image", "report_id = ?", new String[] { Long.toString(reportId) });
        db.close();
    }

    public List<ReportImage> getAllImage(long reportId) {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        ArrayList<ReportImage> images = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * from report_image where report_id = ?", new String[]{Long.toString(reportId)});
        while (cursor.moveToNext()) {
            String uri = cursor.getString(cursor.getColumnIndex("image_uri"));
            long id = cursor.getLong(cursor.getColumnIndex("_id"));
            byte[] bytes = cursor.getBlob(cursor.getColumnIndex("image_thumbnail"));
            String note = cursor.getString(cursor.getColumnIndex("note"));
            String guid = cursor.getString(cursor.getColumnIndex("guid"));

            ReportImage image = new ReportImage(id, uri);
            image.setThumbnail(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            image.setNote(note);
            image.setGuid(guid);
            images.add(image);
        }
        cursor.close();
        db.close();
        return images;
    }

    /*
     * Return report images that are not in a submit queue
     */
    public List<ReportImage> getSubmitPendingImages(long reportId) {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        ArrayList<ReportImage> images = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT * from report_image where report_id = ? and submit != 1 and guid is null",
                new String[]{Long.toString(reportId)});

        while (cursor.moveToNext()) {
            String uri = cursor.getString(cursor.getColumnIndex("image_uri"));
            long id = cursor.getLong(cursor.getColumnIndex("_id"));
            byte[] bytes = cursor.getBlob(cursor.getColumnIndex("image_thumbnail"));
            String note = cursor.getString(cursor.getColumnIndex("note"));
            String guid = cursor.getString(cursor.getColumnIndex("guid"));

            ReportImage image = new ReportImage(id, uri);
            // TODO remove unused operation.
            image.setThumbnail(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            image.setNote(note);
            image.setGuid(guid);
            image.setReportId(reportId);
            images.add(image);
        }
        cursor.close();
        db.close();
        return images;
    }

    public ReportImage getImageById(long id) {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * from report_image where _id = ?",
                new String[]{Long.toString(id)});
        ReportImage image = null;

        if (cursor.moveToFirst()) {
            String uri = cursor.getString(cursor.getColumnIndex("image_uri"));
            byte[] bytes = cursor.getBlob(cursor.getColumnIndex("image_thumbnail"));
            String note = cursor.getString(cursor.getColumnIndex("note"));
            String guid = cursor.getString(cursor.getColumnIndex("guid"));
            long reportId = cursor.getLong(cursor.getColumnIndex("report_id"));

            image = new ReportImage(id, uri);
            image.setThumbnail(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            image.setNote(note);
            image.setGuid(guid);
            image.setReportId(reportId);
        }

        cursor.close();
        db.close();
        return image;
    }

    public void updateReport(long reportId, Date reportDate, long regionId, String remark, String followActionName) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("region_id", regionId);
        if (reportDate != null) {
            values.put("start_date", reportDate.getTime());
        }
        values.put("remark", remark);
        values.put("action_name", followActionName);
        db.update("report", values, "_id = ?", new String[]{Long.toString(reportId)});
        db.close();
    }

    public void updateToTestReport(long reportId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("test_report", Report.TRUE);
        db.update("report", values, "_id = ?", new String[] {Long.toString(reportId)});
        db.close();
    }

    public void saveNote(long id, String note) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("note", note);
        db.update("report_image", values, "_id = ?", new String[]{Long.toString(id)});
        db.close();
    }

    public void assignGuid(long id, String type, String guid) {
        Log.d(TAG, String.format("assign guid %s to %s with id %d", guid, type, id));
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("guid", guid);

        if (type.equals(ReportQueueDataSource.DATA_TYPE)) {
            db.update("report", values, "_id = ?", new String[]{Long.toString(id)});
        } else if (type.equals(ReportQueueDataSource.IMAGE_TYPE)) {
            db.update("report_image", values, "_id = ?", new String[] {Long.toString(id)});
        }
        db.close();
    }

    public void updateSubmit(long reportId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("submit", 1);
        db.update("report", values, "_id = ?", new String[] {Long.toString(reportId)});
        db.close();
    }

    /*
     * Caution!
     */
    public void clearAllData() {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        db.delete("report", null, null);
        db.delete("report_image", null, null);
        db.delete("report_queue", null, null);
        db.delete("report_type", null, null);
        db.delete("notification", null, null);
        db.delete("administration_area", null, null);
        db.delete("feed_item", null, null);
        db.delete("comment", null, null);
        db.delete("visualization_area", null, null);
        db.delete("visualization_volunteer", null, null);
        db.delete("follow_alert", null, null);
        db.delete("report_state", null, null);
        db.delete("record_spec", null, null);
        db.close();
    }

    public void updateImageSubmit(long imageId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("submit", 1);
        db.update("report_image", values, "_id = ?", new String[]{Long.toString(imageId)});
        db.close();
    }
}
