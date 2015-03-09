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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class ReportDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "podd";
    private static final int DATABASE_VERSION = 5;

    private static final String CREATE_TABLE = "create table report"
            + "("
            + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "  type INTEGER,"
            + "  latitude REAL default 0.0,"
            + "  longitude REAL default 0.0,"
            + "  date INTEGER,"
            + "  start_date INTEGER default 0,"
            + "  region_id INTEGER,"    // incident region in case of no latitude, longitude provided
            + "  remark TEXT,"
            + "  form_data TEXT,"
            + "  negative INTEGER,"   // 1 -> found incident, 0 -> no incident
            + "  draft INTEGER,"      // 1 -> save for edit, 0 -> ready to send to server
            + "  submit INTEGER,"     // already submit to server
            + "  guid TEXT"           // uuid that tells this report is added into submit queue list
            + ")";

    private static final String CREATE_TABLE_IMAGE = "create table report_image"
            + "("
            + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "  report_id INTEGER,"
            + "  image_uri TEXT,"
            + "  image_thumbnail BLOB,"
            + "  note TEXT,"
            + "  guid TEXT,"         // unique id of this image
            + "  submit INTEGER"     // 0 -> wait for submit, 1 -> submit already
            + ")";

    private static final String CREATE_TABLE_QUEUE = "create table report_queue"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
            + "  report_id INTEGER,"
            + "  image_id INTEGER,"
            + "  data_type TEXT,"       // report_data, report_image
            + "  created_at INTEGER"
            + ")";

    private static final String CREATE_TABLE_REPORT_TYPE = "create table report_type"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
            + "  name TEXT,"
            + "  version INTEGER,"
            + "  definition TEXT"       // json string
            + ")";

    private static final String CREATE_TABLE_NOTIFICATION = "create table if not exists notification"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
            + "  title TEXT,"
            + "  content TEXT,"          // html
            + "  seen INTEGER DEFAULT 0,"
            + "  created_at INTEGER"
            + ")";

    private static final String CREATE_TABLE_ADMINISTRATION_AREA = "create table if not exists administration_area"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
            + "  name TEXT,"
            + "  parent_name TEXT,"
            + "  is_leaf INTEGER DEFAULT 0"
            + ")";

    private static final String CREATE_TABLE_FEED_ITEM = "create table if not exists feed_item"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
            + "  item_id INTEGER,"
            + "  type TEXT,"
            + "  date INTEGER,"
            + "  json_string TEXT,"
            + "  created_at INTEGER,"
            + "  updated_at INTEGER"
            + ")";

    private static final String CREATE_TABLE_COMMENT = "create table if not exists comment"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
            + "  report_id INTEGER,"
            + "  message TEXT,"
            + "  file_url TEXT,"
            + "  avatar_created_by TEXT,"
            + "  created_by TEXT,"
            + "  created_at TEXT"
            + ")";

    private static final String CREATE_TABLE_VISUALIZATION_AREA = "create table if not exists visualization_area"
            + "("
            + "  _id INTEGER,"
            + "  name TEXT,"
            + "  parent_name TEXT,"
            + "  total_report INTEGER,"
            + "  positive_report INTEGER,"
            + "  negative_report INTEGER,"
            + "  volunteers TEXT," // json string
            + "  animal_type TEXT," // json string
            + "  time_ranges TEXT," // json string
            + "  grade TEXT,"
            + "  month INTEGER,"
            + "  year INTEGER"
            + ")";

    private static final String CREATE_TABLE_VISUALIZATION_VOLUNTEER = "create table if not exists visualization_volunteer"
            + "("
            + "  _id INTEGER,"
            + "  name TEXT,"
            + "  total_report INTEGER,"
            + "  positive_report INTEGER,"
            + "  negative_report INTEGER,"
            + "  animal_type TEXT," // json string
            + "  time_ranges TEXT," // json string
            + "  grade TEXT,"
            + "  month INTEGER,"
            + "  year INTEGER"
            + ")";

    private static final String DROP_TABLE = "drop table report";
    private static final String DROP_TABLE_IMAGE = "drop table report_image";
    private static final String DROP_TABLE_QUEUE = "drop table report_queue";
    private static final String DROP_TABLE_REPORT_TYPE = "drop table report_type";
    private static final String DROP_TABLE_NOTIFICATION = "drop table notification";
    private static final String DROP_TABLE_ADMINISTRATION_AREA = "drop table administration_area";
    private static final String DROP_TABLE_FEED_ITEM = "drop table feed_item";
    private static final String DROP_TABLE_COMMENT = "drop table comment";
    private static final String DROP_TABLE_VISUALIZATION_AREA = "drop table visualization_area";
    private static final String DROP_TABLE_VISUALIZATION_VOLUNTEER = "drop table visualization_volunteer";

    public ReportDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
        sqLiteDatabase.execSQL(CREATE_TABLE_IMAGE);
        sqLiteDatabase.execSQL(CREATE_TABLE_QUEUE);
        sqLiteDatabase.execSQL(CREATE_TABLE_REPORT_TYPE);
        sqLiteDatabase.execSQL(CREATE_TABLE_NOTIFICATION);
        sqLiteDatabase.execSQL(CREATE_TABLE_ADMINISTRATION_AREA);
        sqLiteDatabase.execSQL(CREATE_TABLE_FEED_ITEM);
        sqLiteDatabase.execSQL(CREATE_TABLE_COMMENT);
        sqLiteDatabase.execSQL(CREATE_TABLE_VISUALIZATION_AREA);
        sqLiteDatabase.execSQL(CREATE_TABLE_VISUALIZATION_VOLUNTEER);
        Log.i("DB", "on create");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("DB", String.format("version=%d", oldVersion));
        String sql = null;
        switch (oldVersion) {
            case 1:
                sql = CREATE_TABLE_NOTIFICATION;
                Log.i("DB", String.format(">> sql:\n%s", sql));
                db.execSQL(sql);
            case 2:
                sql = "ALTER TABLE notification ADD COLUMN seen INTEGER DEFAULT 0";
                Log.i("DB", String.format(">> sql:\n%s", sql));
                db.execSQL(sql);
            case 3:
                Log.i("DB", String.format(">> sql:\n%s", sql));
                db.execSQL(CREATE_TABLE_FEED_ITEM);
                db.execSQL(CREATE_TABLE_ADMINISTRATION_AREA);
                db.execSQL(CREATE_TABLE_COMMENT);
                db.execSQL(CREATE_TABLE_VISUALIZATION_AREA);
                db.execSQL(CREATE_TABLE_VISUALIZATION_VOLUNTEER);
            case 4:
                Log.i("DB", String.format(">> sql:\n%s", sql));
        }
    }

}

