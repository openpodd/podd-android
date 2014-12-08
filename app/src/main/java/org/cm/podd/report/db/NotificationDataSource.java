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

import java.util.Date;

public class NotificationDataSource {

    private ReportDatabaseHelper dbHelper;

    public NotificationDataSource(Context context) {
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void close() {
        dbHelper.close();
    }

    public Cursor getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.query("notification", null, null, null, null, null, "_id desc");
    }

    public void save(String title, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("created_at", new Date().getTime());
        db.insert("notification", null, values);
        db.close();
    }
}
