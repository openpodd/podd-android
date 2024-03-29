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
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class ReportTypeDataSource {

    private static final String TAG = "ReportTypeDataSource";
    private ReportDatabaseHelper dbHelper;
    SharedPrefUtil sharedPrefUtil;

    Context context;

    public ReportTypeDataSource(Context context) {
        this.context = context;
        dbHelper = new ReportDatabaseHelper(context);
        sharedPrefUtil = new SharedPrefUtil(context);
    }

    public void initNewData(String reportTypes) {
        deleteAll();
        HashMap<Long,String> categeryMap = new HashMap<>();
        try {
            JSONArray jsonArr = new JSONArray(reportTypes);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Log.d(TAG, String.format("insert report type with id = %d, name = %s", jsonObj.getLong("id"), jsonObj.getString("name")));
                Long id = jsonObj.getLong("id");
                ReportType reportType = new ReportType(
                        id,
                        jsonObj.getString("name")
                );
                reportType.setVersion(jsonObj.getInt("version"));
                reportType.setDefinition(jsonObj.getString("definition"));
                reportType.setWeight(jsonObj.optDouble("weight", 0.0));
                reportType.setWeight(jsonObj.optDouble("weight", 0.0));
                reportType.setFollowable(jsonObj.optBoolean("followable", false));
                reportType.setFollowDay(jsonObj.optInt("followDays", 0));
                reportType.setIsFollowAction(jsonObj.optBoolean("isFollowAction", false));

                String categoryCode = jsonObj.optString("categoryCode", "");
                categeryMap.put(id, categoryCode);

                insert(reportType);
            }

            sharedPrefUtil.setCategoryMap(categeryMap);

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
        values.put("weight", reportType.getWeight());
        values.put("followable", reportType.getFollowable());
        values.put("follow_days", reportType.getFollowDay());
        values.put("is_follow_action", reportType.getIsFollowAction());

        long id = db.insert("report_type", null, values);
        db.close();
        return id;
    }

    public Form getForm(long formId) {
//        if (formId == 100000) return parseForm("podd7.json");
//        if (formId == 2) return parseForm("podd2.json");
//        if (formId == 3) return parseForm("podd3.json");
//        if (formId == 4) return parseForm("podd4.json");
//        if (formId == 6) return parseForm("podd5.json");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from report_type where _id = ?", new String[] {Long.toString(formId)});

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
//        results.add(new ReportType(100000, "podd7.json"));
//        results.add(new ReportType(2, "สัตว์ป่วย/ตาย 2"));
//        results.add(new ReportType(3, "สัตว์ป่วย/ตาย 3"));
//        results.add(new ReportType(4, "สัตว์ป่วย/ตาย 4"));
//        results.add(new ReportType(5, "ป่วยจากการสัมผัสสัตว์"));
//        results.add(new ReportType(7, "ป่วยจากการกินสัตว์"));
//        results.add(new ReportType(6, "สัตว์กัด"));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from report_type order by weight, name asc", null);
        while (cursor.moveToNext()) {
            ReportType reportType = new ReportType(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("name"))
            );
            reportType.setDefinition(cursor.getString(cursor.getColumnIndex("definition")));
            reportType.setVersion(cursor.getInt(cursor.getColumnIndex("version")));
            reportType.setWeight(cursor.getDouble(cursor.getColumnIndex("weight")));
            reportType.setFollowable(cursor.getInt(cursor.getColumnIndex("followable")));
            reportType.setFollowDay(cursor.getInt(cursor.getColumnIndex("follow_days")));
            reportType.setIsFollowAction(cursor.getInt(cursor.getColumnIndex("is_follow_action")));
            results.add(reportType);
        }
        cursor.close();
        db.close();

        return results;
    }

    public List<ReportType> getAllWithNoFollowAction() {
        ArrayList<ReportType> results = new ArrayList<>();
        for (ReportType rt : getAll()) {
            if (! rt.isFollowAction()) {
                results.add(rt);
            }
        }
        return results;
    }
    public List<ReportType> getAllPinWithNoFollowAction() {
        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
        HashMap<Long, String> categoryMap = sharedPrefUtil.getCategoryMap();

        ArrayList<ReportType> results = new ArrayList<>();
        for (ReportType rt : getAll()) {
            String catCode = categoryMap.get(rt.getId());

            if (! rt.isFollowAction() && catCode.equals("pin")) {
                results.add(rt);
            }
        }
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
        values.put("weight", reportType.getWeight());
        values.put("followable", reportType.getFollowable());
        values.put("follow_days", reportType.getFollowDay());
        values.put("is_follow_action", reportType.getIsFollowAction());
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

    public ReportType getNormalCaseReportType() {
        return new ReportType(0, context.getString(R.string.normal_case));
    }

}
