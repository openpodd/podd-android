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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.service.SyncReportTypeService;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.ArrayList;
import java.util.List;

public class ReportTypeActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "ReportTypeActivity";
    private ArrayAdapter<ReportType> adapter;
    private ListView listView;
    private ProgressBar progressBar;
    private ReportTypeDataSource dataSource;
    private ReportDataSource reportDataSource;
    private ReportQueueDataSource reportQueueDataSource;

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);

            ArrayList<ReportType> items = getAll();
            adapter = new ReportTypeAdapter(context, R.layout.list_item_report_type, items);
            listView.setAdapter(adapter);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_type);
        listView = (ListView) findViewById(R.id.report_type_list_view);
        progressBar = (ProgressBar) findViewById(R.id.report_type_progress_bar);

        dataSource = new ReportTypeDataSource(this);
        reportDataSource = new ReportDataSource(this);
        reportQueueDataSource = new ReportQueueDataSource(this);

        ArrayList<ReportType> items = getAll();
        adapter = new ReportTypeAdapter(this, R.layout.list_item_report_type, items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);

        registerReceiver(mSyncReceiver, new IntentFilter(SyncReportTypeService.SYNC));
    }

    private ArrayList<ReportType> getAll() {
        ArrayList<ReportType> items = new ArrayList<ReportType>();
        items.add(new ReportType(0, "ปกติ"));
        items.addAll(dataSource.getAll());
        items.add(new ReportType(-99, "ดึงแบบฟอร์มใหม่"));
        return items;
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

            if (RequestDataUtil.hasNetworkConnection(this)) {
                listView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);

                startSyncReportType();
            }
        } else {
            if (item.getId() == 0) {
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

            // send event hit
            Tracker tracker = ((PoddApplication) getApplication()).getTracker(
                    PoddApplication.TrackerName.APP_TRACKER);
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory("ReportType").setAction(item.getName())
                    .build());
        }
    }

    @Override
    protected void onDestroy() {
        dataSource.close();
        reportDataSource.close();
        reportQueueDataSource.close();

        unregisterReceiver(mSyncReceiver);
        // intent service stops by itself after handle intent
        super.onDestroy();
    }

    private void broadcastReportSubmission() {
        // Broadcasts the Intent to network receiver
        Intent networkIntent = new Intent(DataSubmitService.ACTION_REPORT_SUBMIT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(networkIntent);
    }

    private void startSyncReportType() {
        Intent intent = new Intent(this, SyncReportTypeService.class);
        startService(intent);
    }

    /**
     * Adapter
     */
    public class ReportTypeAdapter extends ArrayAdapter<ReportType> {

        Context context;
        int resource;
        Typeface face;

        public ReportTypeAdapter(Context context, int resource, List<ReportType> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            face = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(this.resource, parent, false);

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setTypeface(face);
            textView.setText(getItem(position).getName());

            int version = getItem(position).getVersion();
            if (version > 0) {
                TextView versionView = (TextView) view.findViewById(android.R.id.text2);
                versionView.setTypeface(face);
                versionView.setText("v" + version);
            }
            return view;
        }
    }
}
