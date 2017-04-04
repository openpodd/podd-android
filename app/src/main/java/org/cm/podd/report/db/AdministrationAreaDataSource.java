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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdministrationAreaDataSource {

    private static final String TAG = "AdministrationAreaDataSource";
    private ReportDatabaseHelper dbHelper;

    Context context;

    public AdministrationAreaDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void initNewData(String areas) {
        deleteAll();
        try {
            JSONArray jsonArr = new JSONArray(areas);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                AdministrationArea administrationArea = new AdministrationArea(
                        jsonObj.getLong("id"),
                        jsonObj.getString("name"),
                        jsonObj.optString("parentName", ""),
                        jsonObj.optBoolean("isLeaf", true)? 1:0
                );
                insert(administrationArea);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("administration_area", null, null);
        db.close();
    }

    public long insert(AdministrationArea administrationArea) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", administrationArea.getId());
        values.put("name", administrationArea.getName());
        values.put("parent_name", administrationArea.getParentName());
        values.put("is_leaf", administrationArea.getIsLeaf());
        long id = db.insert("administration_area", null, values);
        db.close();
        return id;
    }

    public List<AdministrationArea> getAll() {
        ArrayList<AdministrationArea> results = new ArrayList<AdministrationArea>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from administration_area order by _id asc", null);
        String parent_name = "";
        while (cursor.moveToNext()) {
            AdministrationArea administrationArea = new AdministrationArea(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("parent_name")),
                    cursor.getInt(cursor.getColumnIndex("is_leaf"))
            );
            results.add(administrationArea);
        }

        cursor.close();
        db.close();

        return results;
    }

    public List<AdministrationArea> getLeafAll() {
        ArrayList<AdministrationArea> results = new ArrayList<AdministrationArea>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from administration_area where is_leaf=1 order by parent_name asc", null);
        String parent_name = "";
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("parent_name")).equals("null") ||
                    !parent_name.equals(cursor.getString(cursor.getColumnIndex("parent_name")))){

                parent_name = cursor.getString(cursor.getColumnIndex("parent_name"));
                if (!parent_name.equals("null")) {
                    AdministrationArea administrationArea = new AdministrationArea(
                            0,
                            cursor.getString(cursor.getColumnIndex("parent_name")),
                            cursor.getString(cursor.getColumnIndex("parent_name")),
                            0
                    );
                    results.add(administrationArea);
                }
            }

            AdministrationArea administrationArea = new AdministrationArea(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("parent_name")),
                    cursor.getInt(cursor.getColumnIndex("is_leaf"))
            );
            results.add(administrationArea);
        }

        cursor.close();
        db.close();

        return results;
    }

    public void update(AdministrationArea administrationArea) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", administrationArea.getName());
        values.put("parent_name", administrationArea.getParentName());
        values.put("is_leaf", administrationArea.getIsLeaf());
        db.update("administration_area", values, "_id = ?",
                new String[] {Long.toString(administrationArea.getId())});
        db.close();
    }

    public void close() {
        dbHelper.close();
    }

    public void removeAdministrationArea(Long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("administration_area", "_id = ?", new String[] {id.toString()} );
        db.close();
    }
}
