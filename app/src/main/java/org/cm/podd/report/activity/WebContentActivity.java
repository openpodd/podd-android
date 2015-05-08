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

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.util.StyleUtil;
import org.cm.podd.report.util.WebContentUtil;

public class WebContentActivity extends ActionBarActivity {

    WebView webView;
    NotificationDataSource notificationDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_content);
        notificationDataSource = new NotificationDataSource(this);

        webView = (WebView) findViewById(R.id.webView);

        String title = getIntent().getStringExtra("title");
        String body = getIntent().getStringExtra("content");
        long id = getIntent().getLongExtra("id", 0);

        Log.d("WebContentActivity.body", body);

        if (title != null && body != null) {
            WebContentUtil.launch(webView, title, body);
            notificationDataSource.markAsSeen(id, Report.TRUE);
        }

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("WebContent");
        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationDataSource.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_web_content));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);

        return true;
    }

}
