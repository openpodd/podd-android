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
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationDataSource {

    private static final String TAG = "ConfigurationDataSource";
    private ReportDatabaseHelper dbHelper;

    Context context;

    public ConfigurationDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void initNewData(String area) {
        deleteAll();
        try {
            JSONArray jsonArr = new JSONArray(area);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Config config = new Config(
                        jsonObj.getString("system"),
                        jsonObj.getString("key"),
                        jsonObj.getString("value")
                );
                insert(config);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("configuration", null, null);
        db.close();
    }

    public long insert(Config config) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("system", config.getSystem());
        values.put("key", config.getKey());
        values.put("value", config.getValue());
        long id = db.insert("configuration", null, values);
        db.close();
        return id;
    }

    public List<Config> getAll() {
        ArrayList<Config> results = new ArrayList<Config>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from configuration order by system asc", null);
        while (cursor.moveToNext()) {
            Config config = new Config(
                    cursor.getString(cursor.getColumnIndex("system")),
                    cursor.getString(cursor.getColumnIndex("key")),
                    cursor.getString(cursor.getColumnIndex("value"))
            );
            results.add(config);
        }
        cursor.close();
        db.close();

        return results;
    }

    public void update(Config config) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("system", config.getSystem());
        values.put("key", config.getKey());
        values.put("value", config.getValue());
        db.update("configuration", values, "system = ? AND key = ?",
                new String[] {config.getValue(), config.getKey()});
        db.close();
    }

    public void close() {
        dbHelper.close();
    }

    public Config getConfigValue(String system, String key) {
        ArrayList<Config> results = new ArrayList<Config>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from configuration where system='"+ system +"' AND key='"+ key +"' order by system asc", null);
        while (cursor.moveToNext()) {
            Config config = new Config(
                    cursor.getString(cursor.getColumnIndex("system")),
                    cursor.getString(cursor.getColumnIndex("key")),
                    cursor.getString(cursor.getColumnIndex("value"))
            );
            results.add(config);
        }
        cursor.close();
        db.close();

        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
}
