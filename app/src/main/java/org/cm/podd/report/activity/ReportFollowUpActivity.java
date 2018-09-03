package org.cm.podd.report.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.model.ReportAdapter;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReportFollowUpActivity extends AppCompatActivity implements ReportAdapter.OnItemClickListener {
    private final static String TAG = "ReportFollowUpAct";

    private Long parentReportId;
    private Long parentReportFlag;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ReportAdapter mAdapter;
    protected ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_follow_up);
        FontUtil.overrideFonts(this, getWindow().getDecorView());

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mRecyclerView = (RecyclerView) findViewById(R.id.list_follow_up);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new ReportAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);

        parentReportId = getIntent().getLongExtra("parentReportId", 0);
        parentReportFlag = getIntent().getLongExtra("parentReportFlag", 0);

        fetchFollowUpReports(parentReportId);

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("ReportFollowUp");
        tracker.send(new HitBuilders.AppViewBuilder().build());

        if (parentReportFlag == 4) {
            StyleUtil.setActionBarTitle(this, getString(R.string.follow_up_parent));
        } else {
            StyleUtil.setActionBarTitle(this, getString(R.string.follow_up_reports));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onClick(View view, int position) {
        selectItem(position);
    }

    private void selectItem(int position) {
        Log.d(TAG, "Clicked on position: " + Integer.toString(position));

        Intent intent = new Intent(this, ReportViewActivity.class);
        intent.putExtra("id", mAdapter.mDataSet.get(position).getItemId());
        startActivity(intent);
    }

    private void fetchFollowUpReports(Long reportId) {
        FeedItemDataSource feedItemDataSource = new FeedItemDataSource(this);
        FeedItem reportItem = feedItemDataSource.loadByItemId(reportId);
        try {
            if (reportItem != null & reportItem.getFollow() != null) {
                JSONArray items = new JSONArray(reportItem.getFollow());
                ArrayList<FeedItem> feedItems = new ArrayList<FeedItem>();


                for (int i = 0; i != items.length(); ++i) {
                    JSONObject item = items.getJSONObject(i);

                    String flagString = item.getString("flag");
                    int flag = flagString.equals("") ? 0 : Integer.parseInt(flagString);

                    FeedItem feedItem = new FeedItem();
                    feedItem.setItemId(item.getLong("id"));
                    feedItem.setType("report");

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.getDefault());
                    feedItem.setDate(formatter.parse(item.getString("date")));
                    feedItem.setExplanation(item.toString());

                    if (parentReportFlag == 4) {
                        if (flag == 5) {
                            feedItems.add(feedItem);
                        }
                    } else {
                        feedItems.add(feedItem);
                    }
                }

                mAdapter.mDataSet = feedItems;
                mAdapter.notifyDataSetChanged();

                mProgressBar.setVisibility(View.GONE);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Date is not valid, skipping");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == android.R.id.home){
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
