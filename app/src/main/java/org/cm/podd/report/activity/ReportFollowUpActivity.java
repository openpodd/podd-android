package org.cm.podd.report.activity;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.FeedAdapter;
import org.cm.podd.report.fragment.ReportAdapter;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReportFollowUpActivity extends ActionBarActivity implements ReportAdapter.OnItemClickListener {
    private final static String TAG = "ReportFollowUpAct";

    private Long parentReportId;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ReportAdapter mAdapter;
    protected ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_follow_up);
        FontUtil.overrideFonts(this, getWindow().getDecorView());

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mRecyclerView = (RecyclerView) findViewById(R.id.list_follow_up);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new ReportAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);

        parentReportId = getIntent().getLongExtra("parentReportId", 0);
        fetchFollowUpReports(parentReportId);
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
        ReportService.FollowUpAsyncTask task = new ReportService.FollowUpAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                try {
                    JSONArray items = new JSONArray(resp.getRawData());
                    ArrayList<FeedItem> feedItems = new ArrayList<FeedItem>();

                    try {
                        for (int i = 0; i != items.length(); ++i) {
                            JSONObject item = items.getJSONObject(i);

                            // Prepare FeedItem object.
                            FeedItem feedItem = new FeedItem();
                            feedItem.setItemId(item.getLong("id"));
                            feedItem.setType("report");

                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                                    Locale.getDefault());
                            feedItem.setDate(formatter.parse(item.getString("date")));
                            feedItem.setJsonString(item.toString());

                            feedItems.add(feedItem);
                        }

                        mAdapter.mDataSet = feedItems;
                        mAdapter.notifyDataSetChanged();

                        mProgressBar.setVisibility(View.GONE);
                    } catch (JSONException e) {
                        // DO NOTHING.
                        Log.e(TAG, "No results, skipping");
                    } catch (ParseException e) {
                        Log.e(TAG, "Date is not valid, skipping");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON data", e);
                }
            }
        };
        task.setContext(getApplicationContext());
        task.execute(Long.toString(reportId));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.follow_up_reports));
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);
        return true;
    }

}
