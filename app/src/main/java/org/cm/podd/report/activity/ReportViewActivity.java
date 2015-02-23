package org.cm.podd.report.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.cm.podd.report.R;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by siriwat on 2/23/15.
 */
public class ReportViewActivity extends ActionBarActivity {

    private static final String TAG = "ReportViewActivity";

    private Long id;

    private ProgressBar progressBar;
    private View contentWrapper;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Receiving action " + intent.getAction());

            try {
                viewReport(new JSONObject(intent.getStringExtra("report")));
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON data");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard_feed_view);
        progressBar = (ProgressBar) findViewById(R.id.loading_spinner);
        contentWrapper = findViewById(R.id.df_view_content);

        registerReceiver(mReceiver, new IntentFilter(ReportService.ACTION_FETCH_DONE));

        id = getIntent().getLongExtra(ReportService.PARAM_REPORT_ID, 0);
        ReportService.doFetch(getApplicationContext(), id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void viewReport(JSONObject report) {
        progressBar.setVisibility(View.GONE);
        contentWrapper.setVisibility(View.VISIBLE);
    }
}
