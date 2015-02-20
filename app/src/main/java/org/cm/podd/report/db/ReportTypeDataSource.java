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
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.MultipleChoiceSelection;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;
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

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class ReportTypeDataSource {

    private static final String TAG = "ReportTypeDataSource";
    private ReportDatabaseHelper dbHelper;

    Context context;

    public ReportTypeDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
    }

    public void initNewData(String reportTypes) {
        deleteAll();
        try {
            JSONArray jsonArr = new JSONArray(reportTypes);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                ReportType reportType = new ReportType(
                        jsonObj.getLong("id"),
                        jsonObj.getString("name")
                );
                reportType.setVersion(jsonObj.getInt("version"));
                reportType.setDefinition(jsonObj.getString("definition"));

                insert(reportType);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("report_type", null, null);
        db.close();
    }

    public long insert(ReportType reportType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id", reportType.getId());
        values.put("name", reportType.getName());
        values.put("version", reportType.getVersion());
        values.put("definition", reportType.getDefinition());

        long id = db.insert("report_type", null, values);
        db.close();
        return id;
    }

    public Form getForm(long formId) {
//        if (formId == 100000) return parseForm("podd.json");
//        if (formId == 2) return parseForm("podd2.json");
//        if (formId == 3) return parseForm("podd3.json");
//        if (formId == 4) return parseForm("podd4.json");
//        if (formId == 6) return parseForm("podd5.json");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from report_type where _id = ?",
                new String[] {Long.toString(formId)});

        Form result = null;
        if (cursor.moveToFirst()) {
            String definition = cursor.getString(cursor.getColumnIndex("definition"));
            try {
                FormParser formParser = new FormParser();
                formParser.parse(new JSONObject(definition));

                result = formParser.getForm();
            } catch (Exception e) {
                Log.e(TAG, "error while parsing form", e);
            }

        }

        cursor.close();
        db.close();

        return result;
    }

    public List<ReportType> getAll() {
        ArrayList<ReportType> results = new ArrayList<ReportType>();
//        results.add(new ReportType(100000, "podd.json"));
//        results.add(new ReportType(2, "สัตว์ป่วย/ตาย 2"));
//        results.add(new ReportType(3, "สัตว์ป่วย/ตาย 3"));
//        results.add(new ReportType(4, "สัตว์ป่วย/ตาย 4"));
//        results.add(new ReportType(5, "ป่วยจากการสัมผัสสัตว์"));
//        results.add(new ReportType(7, "ป่วยจากการกินสัตว์"));
//        results.add(new ReportType(6, "สัตว์กัด"));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from report_type order by _id asc", null);
        while (cursor.moveToNext()) {
            ReportType reportType = new ReportType(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name"))
            );
            reportType.setVersion(cursor.getInt(cursor.getColumnIndex("version")));
            results.add(reportType);
        }
        cursor.close();
        db.close();

        return results;
    }

    private Form parseForm(String name) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream stream = assetManager.open(name);
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            FormParser formParser = new FormParser();
            formParser.parse(new JSONObject(builder.toString()));

            return formParser.getForm();
        } catch (Exception e) {
            Log.e(TAG, "error while parsing form", e);
        }
        return null;
    }

    private Form createSampleForm1() {
        Form form = new Form();
        form.setStartPageId(1);

        Question<Integer> q1 = new Question<Integer>();
        q1.setTitle("how old are your?");
        q1.setName("age");
        q1.setId(1);
        q1.addValidation(new RequireValidation<Integer>("Age is required"));

        q1.setDataType(DataType.INTEGER);
        form.addQuestion(q1);

        Question<String> q2 = new Question<String>();
        q2.setTitle("What is your name");
        q2.setName("name");
        q2.setId(2);
        q2.setDataType(DataType.STRING);
        form.addQuestion(q2);

        MultipleChoiceQuestion q3 = new MultipleChoiceQuestion(MultipleChoiceSelection.SINGLE);
        q3.setTitle("Type of animal");
        q3.setName("animal_type");
        q3.setDataType(DataType.STRING);
        q3.setId(3);
        q3.addItem("chicken", "chicken");
        q3.addItem("cow", "cow");
        q3.addItem("bird", "bird");
        form.addQuestion(q3);

        MultipleChoiceQuestion q4 = new MultipleChoiceQuestion(MultipleChoiceSelection.MULTIPLE);
        q4.setTitle("symptom");
        q4.setName("symptom");
        q4.setDataType(DataType.STRING);
        q4.setId(4);
        q4.addItem("cough", "cough");
        q4.addItem("fever", "fever");
        q4.addItem("pain", "pain");
        form.addQuestion(q4);

        Page p1 = new Page(1);
        p1.addQuestion(q1);
        form.addPage(p1);
        Page p2 = new Page(2);
        p2.addQuestion(q2);
        form.addPage(p2);
        Page p3 = new Page(3);
        p3.addQuestion(q3);
        form.addPage(p3);
        Page p4 = new Page(4);
        form.addPage(p4);
        p4.addQuestion(q4);

        form.addTransition(new Transition(1, 2, "true"));
        form.addTransition(new Transition(2, 3, "true"));
        form.addTransition(new Transition(3, 4, "true"));

        return form;
    }

    public void update(ReportType reportType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", reportType.getName());
        values.put("version", reportType.getVersion());
        values.put("definition", reportType.getDefinition());
        db.update("report_type", values, "_id = ?",
                new String[] {Long.toString(reportType.getId())});
        db.close();
    }

    public void close() {
        dbHelper.close();
    }

    public void removeReportType(Long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("report_type", "_id = ?", new String[] {id.toString()} );
        db.close();
    }

    public ReportType getReportTypeById(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = null;
        ReportType reportType = null;

        if (id != 0) {
            cursor = db.query("report_type", null, "_id = ?",
                    new String[]{ Long.toString(id) }, null, null, null, "1");
            if (cursor.moveToFirst()) {
                reportType = new ReportType(
                        cursor.getLong(cursor.getColumnIndex("_id")),
                        cursor.getString(cursor.getColumnIndex("name"))
                );
            } else {
                reportType = getNormalCaseReportType();
            }
        } else {
            reportType = getNormalCaseReportType();
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return reportType;
    }

    public ReportType getNormalCaseReportType() {
        return new ReportType(0, context.getString(R.string.normal_case));
    }
}
