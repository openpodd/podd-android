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

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.GroupReportTypeAdapter;
import org.cm.podd.report.model.PinReportTypeAdapter;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.service.SyncReportTypeService;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;

/**
 * Created by pphetra on 4/17/2017 AD.
 */

public class GroupReportTypeActivity extends AppCompatActivity {

    public static final String TAG = "GroupReportTypeActivity";
    private ExpandableListView listView;
    private ListView pinListView;
    private ReportTypeDataSource dataSource;
    private ReportDataSource reportDataSource;
    private ReportQueueDataSource reportQueueDataSource;
    private GroupReportTypeAdapter adapter;
    private PinReportTypeAdapter pinAdapter;
    private CheckBox testCheckbox;
    private SharedPrefUtil sharedPrefUtil;

    ProgressDialog progress;

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress.hide();
            adapter = new GroupReportTypeAdapter(GroupReportTypeActivity.this, dataSource.getAllWithNoFollowAction());
            listView.setAdapter(adapter);
            pinAdapter = new PinReportTypeAdapter(GroupReportTypeActivity.this, dataSource.getAllPinWithNoFollowAction());
            pinListView.setAdapter(pinAdapter);
        }
    };

    public int GetPixelFromDips(float pixels) {
        // Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefUtil = new SharedPrefUtil((getApplicationContext()));

        dataSource = new ReportTypeDataSource(this);
        reportDataSource = new ReportDataSource(this);
        reportQueueDataSource = new ReportQueueDataSource(this);

        Typeface typeface = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);
        setContentView(R.layout.activity_group_report_type);
        testCheckbox = findViewById(R.id.test_checkbox);
        testCheckbox.setTypeface(typeface);

        final LinearLayout testLayout = findViewById(R.id.test_section);
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

        Toolbar topBar = findViewById(R.id.toolbar_top);
        setSupportActionBar(topBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listView = findViewById(R.id.report_type_list_view);
        pinListView = findViewById(R.id.pin_report_type_list_view);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            listView.setIndicatorBounds(width - GetPixelFromDips(100), width - GetPixelFromDips(10));
        } else {
            listView.setIndicatorBoundsRelative(width - GetPixelFromDips(100), width - GetPixelFromDips(10));
        }

        adapter = new GroupReportTypeAdapter(this, dataSource.getAllWithNoFollowAction());
        listView.setAdapter(adapter);
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupIndex, int childIndex, long id) {
                ReportType item = (ReportType) adapter.getChild(groupIndex, childIndex);
                Log.d(TAG, String.format("select report type = %d", item.getId()));
                Intent intent = ReportActivity.newReportIntent(GroupReportTypeActivity.this, item.getId(), testCheckbox.isChecked());
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

        pinAdapter = new PinReportTypeAdapter(this, dataSource.getAllPinWithNoFollowAction());
        pinListView.setAdapter(pinAdapter);
        pinListView.setOnItemClickListener( new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ReportType item = (ReportType) pinAdapter.getItem(i);
                Log.d(TAG, String.format("select pin report type = %d", item.getId()));
                Intent intent = ReportActivity.newReportIntent(GroupReportTypeActivity.this, item.getId(), testCheckbox.isChecked());
                startActivity(intent);
            }
        });

        progress = new ProgressDialog(this);
        progress.setTitle(getString(R.string.update_report_type));
        registerReceiver(mSyncReceiver, new IntentFilter(SyncReportTypeService.SYNC));

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sharedPrefUtil.isUserLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void broadcastReportSubmission() {
        DataSubmitService.enqueueWork(this, new Intent(DataSubmitService.ACTION_REPORT_SUBMIT));
    }

    @Override
    protected void onDestroy() {
        dataSource.close();
        reportDataSource.close();
        reportQueueDataSource.close();

        unregisterReceiver(mSyncReceiver);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.group_report_type_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh_report_type:
                if (RequestDataUtil.hasNetworkConnection(this)) {
                    progress.show();
                    Intent intent = new Intent(this, SyncReportTypeService.class);
                    startService(intent);
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if ( progress != null && progress.isShowing() )
        {
            progress.cancel();
        }
    }
}
