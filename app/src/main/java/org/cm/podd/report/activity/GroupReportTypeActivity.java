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

package org.cm.podd.report.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.GroupReportTypeAdapter;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.util.StyleUtil;

/**
 * Created by pphetra on 4/17/2017 AD.
 */

public class GroupReportTypeActivity extends AppCompatActivity {

    public static final String TAG = "GroupReportTypeActivity";
    private Typeface typeface;
    private ExpandableListView listView;
    private ReportTypeDataSource dataSource;
    private ReportDataSource reportDataSource;
    private ReportQueueDataSource reportQueueDataSource;
    private GroupReportTypeAdapter adapter;
    private CheckBox testCheckbox;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataSource = new ReportTypeDataSource(this);
        reportDataSource = new ReportDataSource(this);
        reportQueueDataSource = new ReportQueueDataSource(this);

        typeface = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);
        setContentView(R.layout.activity_group_report_type);
        testCheckbox = (CheckBox) findViewById(R.id.test_checkbox);
        testCheckbox.setTypeface(typeface);

        final LinearLayout testLayout = (LinearLayout) findViewById(R.id.test_section);
        testCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    testLayout.setBackgroundResource(R.color.report_test_indicator);
                    compoundButton.setBackgroundResource(R.color.report_test_indicator);
                } else {
                    testLayout.setBackgroundResource(R.color.white);
                    compoundButton.setBackgroundResource(R.color.white);
                }
            }
        });

        Toolbar topBar = (Toolbar) findViewById(R.id.toolbar_top);
        setSupportActionBar(topBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listView = (ExpandableListView) findViewById(R.id.report_type_list_view);
        adapter = new GroupReportTypeAdapter(this, dataSource.getAll());
        listView.setAdapter(adapter);
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupIndex, int childIndex, long id) {
                ReportType item = (ReportType) adapter.getChild(groupIndex, childIndex);
                Log.d(TAG, String.format("select report type = %d", item.getId(), testCheckbox.isChecked()));
                Intent intent = new Intent(GroupReportTypeActivity.this, ReportActivity.class);
                intent.putExtra("reportType", item.getId());
                intent.putExtra("test", testCheckbox.isChecked());
                startActivity(intent);
                return true;
            }
        });
        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long id) {
                Log.d(TAG, "group " + groupPosition);
                if (groupPosition == 0) {
                    long reportId = reportDataSource.createPositiveReport();

                    // after save positive report, submit to queue right away
                    reportQueueDataSource.addDataQueue(reportId);
                    broadcastReportSubmission();

                    finish();
                    return true;
                }
                return false;
            }
        });

    }

    private void broadcastReportSubmission() {
        // Broadcasts the Intent to network receiver
        Intent networkIntent = new Intent(DataSubmitService.ACTION_REPORT_SUBMIT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(networkIntent);
    }

    @Override
    protected void onDestroy() {
        dataSource.close();
        reportDataSource.close();
        reportQueueDataSource.close();

        super.onDestroy();
    }
}
