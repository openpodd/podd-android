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

import org.cm.podd.report.model.Report;

import java.util.Date;

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class ReportDataSource {

    private ReportDatabaseHelper reportDatabaseHelper;
    private ReportImageDatabaseHelper reportImageDatabaseHelper;

    private SQLiteDatabase readableDB;


    public ReportDataSource(Context context) {
        reportDatabaseHelper = new ReportDatabaseHelper(context);
        reportImageDatabaseHelper = new ReportImageDatabaseHelper(context);
    }

    /**
     * create draft report
     * @return row id of new report
     */
    public long createDraftReport(long type) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", new Date().getTime());
        values.put("type", type);
        values.put("draft", 1);
        values.put("negative", 1);
        values.put("submit", 0);
        return db.insert("report", null, values);
    }

    /*
     * create report that has no incident;
     */
    public void createPositiveReport() {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", new Date().getTime());
        values.put("draft", 0);
        values.put("negative", 0);
        values.put("submit", 0);
        db.insert("report", null, values);

        //TODO submit data to server
    }

    public Cursor getAll() {
        return getReadableDB().rawQuery("SELECT * FROM report order by _id desc", null);
    }

    public Report getById(long id) {
        Cursor cursor = getReadableDB().rawQuery("SELECT * FROM report where _id = ?", new String[] {Long.toString(id)});
        cursor.moveToFirst();

        long type = cursor.getLong(cursor.getColumnIndex("type"));
        Date date = new Date(cursor.getLong(cursor.getColumnIndex("date")));
        int negative = cursor.getInt(cursor.getColumnIndex("negative"));
        int draft = cursor.getInt(cursor.getColumnIndex("draft"));
        int submit = cursor.getInt(cursor.getColumnIndex("submit"));

        Report report = new Report(id, type, date, negative, draft, submit);
        report.setFormData(cursor.getString(cursor.getColumnIndex("form_data")));
        cursor.close();
        return report;
    }

    public void updateData(long reportId, String jsonStr, int draftFlag) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("form_data", jsonStr);
        values.put("draft", draftFlag);
        db.update("report", values, "_id = ?", new String[] {Long.toString(reportId)});
        db.close();
    }

    private SQLiteDatabase getReadableDB() {
        if (readableDB == null) {
            readableDB = reportDatabaseHelper.getReadableDatabase();
        }
        return readableDB;
    }

}
