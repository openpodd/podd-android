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

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class ReportDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "podd";
    private static final int DATABASE_VERSION = 1;

    private static final String CREATE_TABLE = "create table report"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
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
            + "  _id INTEGER PRIMARY KEY,"
            + "  report_id INTEGER,"
            + "  image_uri TEXT,"
            + "  image_thumbnail BLOB,"
            + "  note TEXT,"
            + "  guid TEXT" // url or key returned from aws s3 image upload, also mark as in submit queue
            + ")";

    private static final String CREATE_TABLE_QUEUE = "create table report_queue"
            + "("
            + "  _id INTEGER PRIMARY KEY,"
            + "  report_id INTEGER,"
            + "  image_id INTEGER,"
            + "  data_type TEXT,"       // report_data, report_image
            + "  created_at INTEGER"
            + ")";

    private static final String DROP_TABLE = "drop table report";
    private static final String DROP_TABLE_IMAGE = "drop table report_image";
    private static final String DROP_TABLE_QUEUE = "drop table report_queue";

    public ReportDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(CREATE_TABLE);
        sqLiteDatabase.execSQL(CREATE_TABLE_IMAGE);
        sqLiteDatabase.execSQL(CREATE_TABLE_QUEUE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL(DROP_TABLE);
        sqLiteDatabase.execSQL(DROP_TABLE_IMAGE);
        sqLiteDatabase.execSQL(DROP_TABLE_QUEUE);
    }
}
