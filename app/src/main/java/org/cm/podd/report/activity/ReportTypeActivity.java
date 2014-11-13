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
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.util.StyleUtil;

import java.util.ArrayList;

public class ReportTypeActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "ReportTypeActivity";
    private ArrayAdapter<ReportType> adapter;
    private ListView listView;
    private ProgressBar progressBar;
    private ReportTypeDataSource dataSource;
    private ReportDataSource reportDataSource;
    private ReportQueueDataSource reportQueueDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_type);
        listView = (ListView) findViewById(R.id.report_type_list_view);
        progressBar = (ProgressBar) findViewById(R.id.report_type_progress_bar);

        dataSource = new ReportTypeDataSource(this);
        reportDataSource = new ReportDataSource(this);
        reportQueueDataSource = new ReportQueueDataSource(this);

        ArrayList<ReportType> items = new ArrayList<ReportType>();
        items.add(new ReportType(0, "ปกติ"));
        items.addAll(dataSource.getAll());
        items.add(new ReportType(-99, "ดึงแบบฟอร์มใหม่"));
        adapter = new ArrayAdapter<ReportType>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_report_type));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);

        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.report_type, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        ReportType item = adapter.getItem(position);
        if (item.getId() == -99) {

            listView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

        } else if (item.getId() == 0) {
            long reportId = reportDataSource.createPositiveReport();

            // after save positive report, submit to queue right away
            reportQueueDataSource.addDataQueue(reportId);
            broadcastReportSubmission();

            finish();

        } else {
            Intent intent = new Intent(this, ReportActivity.class);
            intent.putExtra("reportType", item.getId());
            startActivity(intent);
        }
    }

    private void broadcastReportSubmission() {
        // Broadcasts the Intent to network receiver
        Intent networkIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(networkIntent);
    }
}
