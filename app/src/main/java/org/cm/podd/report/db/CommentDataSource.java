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
import org.cm.podd.report.model.Comment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommentDataSource {

    private static final String TAG = "CommentDataSource";
    private ReportDatabaseHelper dbHelper;

    Context context;

    public CommentDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void initNewData(String area) {
        deleteAll();
        try {
            JSONArray jsonArr = new JSONArray(area);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                JSONObject createdBy = new JSONObject(jsonObj.getString("createdBy"));
                Comment comment = new Comment(
                        jsonObj.getLong("id"),
                        jsonObj.getLong("reportId"),
                        jsonObj.getString("message"),
                        jsonObj.getString("fileUrl"),
                        jsonObj.getString("avatarCreatedBy"),
                        createdBy.getString("firstName") + createdBy.getString("lastName"),
                        jsonObj.getString("createdAt")
                );
                insert(comment);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("comment", null, null);
        db.close();
    }

    public long insert(Comment comment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", comment.getId());
        values.put("report_id", comment.getReportId());
        values.put("message", comment.getMessage());
        values.put("file_url", comment.getFileUrl());
        values.put("avatar_created_by", comment.getAvatarCreatedBy());
        values.put("created_by", comment.getCreatedBy());
        values.put("created_at", comment.getCreatedAt());
        long id = db.insert("comment", null, values);
        db.close();
        return id;
    }

    public List<Comment> getAllFromReport(Long reportId) {
        ArrayList<Comment> results = new ArrayList<Comment>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from comment where report_id="+ reportId +" order by _id asc", null);
        while (cursor.moveToNext()) {
            Comment comment = new Comment(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getLong(cursor.getColumnIndex("report_id")),
                    cursor.getString(cursor.getColumnIndex("message")),
                    cursor.getString(cursor.getColumnIndex("file_url")),
                    cursor.getString(cursor.getColumnIndex("avatar_created_by")),
                    cursor.getString(cursor.getColumnIndex("created_by")),
                    cursor.getString(cursor.getColumnIndex("created_at"))
            );
            results.add(comment);
        }
        cursor.close();
        db.close();

        return results;
    }

    public void update(Comment comment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", comment.getId());
        values.put("report_id", comment.getReportId());
        values.put("message", comment.getMessage());
        values.put("file_url", comment.getFileUrl());
        values.put("avatar_created_by", comment.getAvatarCreatedBy());
        values.put("created_by", comment.getCreatedBy());
        values.put("created_at", comment.getCreatedAt());
        db.update("comment", values, "_id = ?",
                new String[] {Long.toString(comment.getId())});
        db.close();
    }

    public void close() {
        dbHelper.close();
    }

    public void removeAdministrationArea(Long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("comment", "_id = ?", new String[] {id.toString()} );
        db.close();
    }
}
