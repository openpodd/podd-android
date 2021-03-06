package org.cm.podd.report.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.fragment.ReportFollowFragment;
import org.cm.podd.report.fragment.ReportInfoFragment;
import org.cm.podd.report.fragment.ReportStateFragment;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.service.SyncReportStateService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by siriwat on 2/23/15.
 */
public class ReportViewActivity extends AppCompatActivity implements ReportInfoFragment.OnFragmentInteractionListener,
        ReportStateFragment.OnFragmentInteractionListener,
        ReportFollowFragment.OnFragmentInteractionListener {

    private static final String TAG = "ReportViewActivity";

    private Long id;
    private ProgressBar progressBar;

    private BroadcastReceiver mReceiver;
    private Bundle bundle;


    Fragment mCurrentFragment;

    private FloatingActionButton fabComment;
    private String reportDetail;

    private String stateCode;

    Context context;

    TabLayout.Tab tabInfo;
    TabLayout.Tab tabState;
    TabLayout.Tab tabFollow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard_feed_view);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        FontUtil.overrideFonts(this, findViewById(R.id.report_main_scrollview));

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        tabInfo = tabLayout.newTab().setText(R.string.report_info);
        tabState = tabLayout.newTab().setText(R.string.report_state);
        tabFollow = tabLayout.newTab().setText(R.string.report_follow);

        tabLayout.addTab(tabInfo);
        tabLayout.addTab(tabState);
        tabLayout.addTab(tabFollow);

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (reportDetail == null) return;

                switch (tab.getPosition()) {
                    case 0:
                        bundle = new Bundle();
                        bundle.putString("report", reportDetail);
                        mCurrentFragment = new ReportInfoFragment();
                        mCurrentFragment.setArguments(bundle);

                        break;
                    case 1:
                        bundle = new Bundle();
                        bundle.putString("report", reportDetail);
                        mCurrentFragment = new ReportStateFragment();
                        mCurrentFragment.setArguments(bundle);
                        break;
                    case 2:
                        bundle = new Bundle();
                        bundle.putString("report", reportDetail);
                        mCurrentFragment = new ReportFollowFragment();
                        mCurrentFragment.setArguments(bundle);
                        break;
                }
                invalidateOptionsMenu();
                changeFragment();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        context = this;

        progressBar = (ProgressBar) findViewById(R.id.loading_spinner);


        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Receiving action " + intent.getAction());
                refreshViewRefresh();
            }
        };

        registerReceiver(mReceiver, new IntentFilter(ReportService.ACTION_FETCH_DONE));

        id = getIntent().getLongExtra(ReportService.PARAM_REPORT_ID, 0);

        if (RequestDataUtil.hasNetworkConnection(this)) {
            ReportService.doFetch(getApplicationContext(), id);
        } else {
            refreshViewRefresh();
        }

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("ReportView");
        tracker.send(new HitBuilders.AppViewBuilder().build());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        startSyncReportStateService();
    }

    private void refreshViewRefresh(){
        FeedItemDataSource feedItemDataSource = new FeedItemDataSource(this);
        FeedItem feedItem = feedItemDataSource.loadByItemId(id);
        try {
            if (feedItem !=null && feedItem.getDetail() != null ) {
                JSONObject report = new JSONObject(feedItem.getDetail());
                if (report.getLong("id") == id) {
                    reportDetail = feedItem.getDetail();
                    viewReport(report);
                }
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    private void changeFragment() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                .commit();
    }

    private void viewReport(JSONObject report) {

        if (!tabInfo.isSelected()) return;

        try {

            String title = report.getString("reportTypeName");
            if (!report.getString("parent").equals("null")) {
                title = getString(R.string.follow) + "" + title;
            }

            setActivityTitleWithType(title);

            Bundle bundle = new Bundle();
            bundle.putString("report", reportDetail);
            mCurrentFragment = new ReportInfoFragment();
            mCurrentFragment.setArguments(bundle);
            changeFragment();

            final Long reportId = Long.parseLong(report.getString("id"));

            fabComment = (FloatingActionButton) findViewById(R.id.fab_comment);
            fabComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, ReportCommentActivity.class);
                    intent.putExtra("reportId", reportId);
                    startActivityForResult(intent, 0);
                }
            });

            progressBar.setVisibility(View.GONE);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
        } catch (IllegalStateException e){
            Log.e(TAG, "Error IllegalStateException", e);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }
    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void setActivityTitleWithType(String type) {
        String template = getString(R.string.report_activity_title_template);
        getSupportActionBar().setTitle(template.replace(":type", type));
    }

    public String getReportState() {
        return stateCode;
    }

    public void changeReportState(String stateCode) {
        this.stateCode = stateCode;

        Intent intent = new Intent(ReportService.ACTION_STATE_SET_DONE);
        intent.putExtra("reportId", id);
        intent.putExtra("stateCode", stateCode);

        sendBroadcast(intent);

        ReportService.doFetch(context, id);

    }

    public static class RemoteImageAsyncTask extends AsyncTask<String, Void, Drawable> {
        @Override
        protected Drawable doInBackground(String... params) {
            String url = params[0];
            Drawable thumb_d = null;

            try {
                URL thumb_u = new URL(url);
                thumb_d = Drawable.createFromStream(thumb_u.openStream(), "src");
            } catch (MalformedURLException e) {
                Log.e(TAG, "Malformed URL", e);
            } catch (IOException e) {
                Log.e(TAG, "Cannot load image from :" + url, e);
            }

            return thumb_d;
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

    private void startSyncReportStateService() {
        Intent intent = new Intent(this, SyncReportStateService.class);
        startService(intent);
    }
}
