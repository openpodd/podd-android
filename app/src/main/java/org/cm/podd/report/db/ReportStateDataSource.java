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
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.cm.podd.report.R;
import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.MultipleChoiceSelection;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.model.ReportState;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.model.Transition;
import org.cm.podd.report.model.parser.FormParser;
import org.cm.podd.report.model.validation.RequireValidation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ReportStateDataSource {

    private static final String TAG = "ReportStateDataSource";
    private ReportDatabaseHelper dbHelper;

    private static final String TABLE_NAME = "report_state";
    Context context;

    public ReportStateDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void initNewData(String reportStates) {
        deleteAll();
        try {
            JSONArray jsonArr = new JSONArray(reportStates);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Log.d(TAG, String.format("insert report state with id = %d, report_type %d, name = %s", jsonObj.getLong("id"), jsonObj.getInt("reportType"), jsonObj.getString("name")));
                ReportState reportState = new ReportState(
                        jsonObj.getLong("id"),
                        jsonObj.getInt("reportType"),
                        jsonObj.getString("name"),
                        jsonObj.getString("code"),
                        jsonObj.getString("description"),
                        (jsonObj.getBoolean("canEdit") == true? 1 :0)
                );

                insert(reportState);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("report_state", null, null);
        db.close();
    }

    public long insert(ReportState reportState) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", reportState.getId());
        values.put("report_type", reportState.getReportType());
        values.put("name", reportState.getName());
        values.put("code", reportState.getCode());
        values.put("description", reportState.getDescription());
        values.put("can_edit", reportState.getCanEdit());

        long id = db.insert("report_state", null, values);
        db.close();
        return id;
    }

    public List<ReportState> getAll() {
        ArrayList<ReportState> results = new ArrayList<ReportState>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from report_state order by report_type asc", null);

        while (cursor.moveToNext()) {
            ReportState reportState = new ReportState(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getInt(cursor.getColumnIndex("report_type")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("code")),
                    cursor.getString(cursor.getColumnIndex("description")),
                    cursor.getInt(cursor.getColumnIndex("can_edit"))
            );
            results.add(reportState);
        }
        cursor.close();
        db.close();

        return results;
    }

    public ReportState getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from report_state order by report_type asc", null);

        ReportState reportState = null;
        Cursor result = db.query(TABLE_NAME, null, "_id = ?",
                new String[]{Long.toString(id)}, null, null, null);

        try {
            if (result.moveToNext()) {
                reportState = new ReportState(
                        cursor.getLong(cursor.getColumnIndex("_id")),
                        cursor.getInt(cursor.getColumnIndex("report_type")),
                        cursor.getString(cursor.getColumnIndex("name")),
                        cursor.getString(cursor.getColumnIndex("code")),
                        cursor.getString(cursor.getColumnIndex("description")),
                        cursor.getInt(cursor.getColumnIndex("can_edit"))
                );
            }
        } finally {
            result.close();
            db.close();
        }

        return reportState;
    }

    public List<ReportState> getByReportType(int reportType) {
        ArrayList<ReportState> results = new ArrayList<ReportState>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "report_type = ?",
                new String[]{Long.toString(reportType)}, null, null, null);

        while (cursor.moveToNext()) {
            ReportState reportState = new ReportState(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getInt(cursor.getColumnIndex("report_type")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("code")),
                    cursor.getString(cursor.getColumnIndex("description")),
                    cursor.getInt(cursor.getColumnIndex("can_edit"))
            );
            results.add(reportState);
        }
        cursor.close();
        db.close();

        return results;
    }

    public Long getIdByReportTypeAndCode(int reportType, String code) {
        long _id = 0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "report_type = ? AND code = ? ",
                new String[]{Long.toString(reportType), code}, null, null, null);

        while (cursor.moveToNext()) {
            _id = cursor.getLong(cursor.getColumnIndex("_id"));
        }

        cursor.close();
        db.close();

        return _id;
    }

    public void update(ReportState reportState) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("report_type", reportState.getReportType());
        values.put("name", reportState.getName());
        values.put("code", reportState.getCode());
        values.put("description", reportState.getDescription());
        values.put("can_edit", reportState.getCanEdit());
        db.update("report_state", values, "_id = ?",
                new String[] {Long.toString(reportState.getId())});
        db.close();
    }

    public void close() {
        dbHelper.close();
    }

    public void removeReportState(Long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("report_state", "_id = ?", new String[] {id.toString()} );
        db.close();
    }


}
