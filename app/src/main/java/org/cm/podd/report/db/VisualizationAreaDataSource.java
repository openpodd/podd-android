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

import org.cm.podd.report.model.VisualizationAdministrationArea;
import org.cm.podd.report.model.VisualizationVolunteer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VisualizationAreaDataSource {

    private static final String TAG = "VisualVolunteerDataSource";
    private ReportDatabaseHelper dbHelper;

    Context context;

    public VisualizationAreaDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void initNewData(String areas) {
        deleteAll();
        try {
            JSONArray jsonArr = new JSONArray(areas);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                VisualizationAdministrationArea area = new VisualizationAdministrationArea(
                        jsonObj.getLong("id"),
                        jsonObj.getString("name"),
                        jsonObj.getString("parentName")
                );
                insert(area);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("visualization_area", null, null);
        db.close();
    }

    public long insert(VisualizationAdministrationArea area) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", area.getId());
        values.put("name", area.getName());
        values.put("parent_name", area.getName());
        long id = db.insert("visualization_area", null, values);
        db.close();
        return id;
    }

    public VisualizationAdministrationArea getAllFromVolunteerByMonth(Long id, Integer month, Integer year) {
        VisualizationAdministrationArea area = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from visualization_area where _id= ?"
                + " and month=? and year=? order by _id asc", new String[] {id.toString(), month.toString(), year.toString()});

        while (cursor.moveToNext()) {
            area = new VisualizationAdministrationArea(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("parent_name"))
            );
            area.setTotalReport(cursor.getInt(cursor.getColumnIndex("total_report")));
            area.setPositiveReport(cursor.getInt(cursor.getColumnIndex("position_report")));
            area.setNegativeReport(cursor.getInt(cursor.getColumnIndex("negative_report")));
            area.setVolunteers(cursor.getString(cursor.getColumnIndex("volunteers")));
            area.setAnimalType(cursor.getString(cursor.getColumnIndex("animal_type")));
            area.setTimeRanges(cursor.getString(cursor.getColumnIndex("time_ranges")));
            area.setGrade(cursor.getString(cursor.getColumnIndex("grade")));
        }
        cursor.close();
        db.close();

        return area;
    }

    public void update(VisualizationAdministrationArea area) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", area.getName());
        values.put("parent_name", area.getParentName());
        values.put("total_report", area.getTotalReport());
        values.put("positive_report", area.getPositiveReport());
        values.put("negative_report", area.getNegativeReport());
        values.put("volunteers", area.getVolunteers());
        values.put("animal_type", area.getAnimalType());
        values.put("time_ranges", area.getTimeRanges());
        values.put("grade", area.getGrade());
        db.update("visualization_area", values, "_id = ? and month=? and year=?",
                new String[] {Long.toString(area.getId()),
                        Integer.toString(area.getMonth()),
                        Integer.toString(area.getYear())});
        db.close();
    }

    public void close() {
        dbHelper.close();
    }

    public void removeAdministrationArea(Long id, Integer month, Integer year) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("visualization_area", "_id = ?"
                + " and month=? and year=? order by _id asc", new String[] {id.toString(), month.toString(), year.toString()});
        db.close();
    }
}
