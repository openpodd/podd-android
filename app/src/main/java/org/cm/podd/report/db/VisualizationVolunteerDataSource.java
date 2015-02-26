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

import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.VisualizationVolunteer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VisualizationVolunteerDataSource {

    private static final String TAG = "VisualVolunteerDataSource";
    private ReportDatabaseHelper dbHelper;

    Context context;

    public VisualizationVolunteerDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void initNewData(String volunteers) {
        deleteAll();
        try {
            JSONArray jsonArr = new JSONArray(volunteers);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                VisualizationVolunteer volunteer = new VisualizationVolunteer(
                        jsonObj.getLong("id"),
                        jsonObj.getString("name")
                );
                insert(volunteer);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("visualization_volunteer", null, null);
        db.close();
    }

    public long insert(VisualizationVolunteer volunteer) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", volunteer.getId());
        values.put("name", volunteer.getName());
        values.put("total_report", volunteer.getTotalReport());
        values.put("positive_report", volunteer.getPositiveReport());
        values.put("negative_report", volunteer.getNegativeReport());
        values.put("animal_type", volunteer.getAnimalType());
        values.put("time_ranges", volunteer.getTimeRanges());
        values.put("grade", volunteer.getGrade());
        values.put("month", volunteer.getMonth());
        values.put("year", volunteer.getYear());
        long id = db.insert("visualization_volunteer", null, values);
        db.close();
        return id;
    }

    public VisualizationVolunteer getFromVolunteerFromMonth(Long id, Integer month, Integer year) {
        VisualizationVolunteer volunteer = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from visualization_volunteer where _id= ?"
                + " and month=? and year=? order by _id asc", new String[] {id.toString(), month.toString(), year.toString()});

        while (cursor.moveToNext()) {
            volunteer = new VisualizationVolunteer(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name"))
            );
            volunteer.setTotalReport(cursor.getInt(cursor.getColumnIndex("total_report")));
            volunteer.setPositiveReport(cursor.getInt(cursor.getColumnIndex("positive_report")));
            volunteer.setNegativeReport(cursor.getInt(cursor.getColumnIndex("negative_report")));
            volunteer.setAnimalType(cursor.getString(cursor.getColumnIndex("animal_type")));
            volunteer.setTimeRanges(cursor.getString(cursor.getColumnIndex("time_ranges")));
            volunteer.setGrade(cursor.getString(cursor.getColumnIndex("grade")));
        }
        cursor.close();
        db.close();

        return volunteer;
    }

    public void update(VisualizationVolunteer volunteer) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", volunteer.getName());
        values.put("total_report", volunteer.getTotalReport());
        values.put("positive_report", volunteer.getPositiveReport());
        values.put("negative_report", volunteer.getNegativeReport());
        values.put("animal_type", volunteer.getAnimalType());
        values.put("time_ranges", volunteer.getTimeRanges());
        values.put("grade", volunteer.getGrade());
        values.put("month", volunteer.getMonth());
        values.put("year", volunteer.getYear());
        db.update("visualization_volunteer", values, "_id = ? and month=? and year=?",
                new String[] {Long.toString(volunteer.getId()),
                        Integer.toString(volunteer.getMonth()),
                        Integer.toString(volunteer.getYear())});
        db.close();
    }

    public void close() {
        dbHelper.close();
    }

    public void removeAdministrationArea(Long id, Integer month, Integer year) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("visualization_volunteer", "_id = ?"
                + " and month=? and year=? order by _id asc", new String[] {id.toString(), month.toString(), year.toString()});
        db.close();
    }
}
