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
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.StyleUtil;
import org.cm.podd.report.util.WebContentUtil;

public class WebContentActivity extends ActionBarActivity {

    public static final String URL_SCHEME = "podd";

    WebView webView;
    NotificationDataSource notificationDataSource;
    private WebSettings webSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_content);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        notificationDataSource = new NotificationDataSource(this);

        webView = (WebView) findViewById(R.id.webView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        webSettings = webView.getSettings();
        webSettings.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        /*
          I wanna use JavascriptInterface but I can't due to security exception here :
          https://labs.mwrinfosecurity.com/blog/webview-addjavascriptinterface-remote-code-execution/
         */
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (uri.getScheme().equals(URL_SCHEME)) {
                    switch (uri.getHost()) {
                        case "redirectTo":
                            String targetUrl = uri.getQueryParameter("url");
                            view.loadUrl(targetUrl);
                            break;
                    }
                    return true;
                }

                if (uri.getHost().equals("www.cmonehealth.org") && url.matches(".*dashboard/#/home\\?reportId=(\\d+).*")) {
                    Uri newUri = Uri.parse(url.replace("#/", ""));
                    String reportId = newUri.getQueryParameter("reportId");

                    Intent intent = new Intent(WebContentActivity.this, ReportViewActivity.class);
                    intent.putExtra(ReportService.PARAM_REPORT_ID, Long.valueOf(reportId));
                    startActivity(intent);

                    return true;
                }

                return false;
            }
        });

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationDataSource.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_web_content));
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setHomeAsUpIndicator(0);
//        actionBar.setLogo(R.drawable.arrow_left_with_pad);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        return true;
    }

}
