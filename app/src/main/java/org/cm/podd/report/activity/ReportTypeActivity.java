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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.ReportType;

import java.util.ArrayList;

public class ReportTypeActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String TAG = "ReportTypeActivity";
    private ArrayAdapter<ReportType> adapter;
    private ListView listView;
    private ProgressBar progressBar;
    private ReportTypeDataSource dataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_type);
        listView = (ListView) findViewById(R.id.report_type_list_view);
        progressBar = (ProgressBar) findViewById(R.id.report_type_progress_bar);

        dataSource = new ReportTypeDataSource(this);

        ArrayList<ReportType> items = new ArrayList<ReportType>();
        items.add(new ReportType(0, "No report"));
        items.addAll(dataSource.getAll());
        items.add(new ReportType(-99, "Reload Type"));
        adapter = new ArrayAdapter<ReportType>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.report_type, menu);
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
        } else {
            Intent intent = new Intent(this, ReportActivity.class);
            intent.putExtra("reportType", item.getId());
            startActivity(intent);
        }
    }
}
