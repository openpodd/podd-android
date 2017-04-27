package org.cm.podd.report.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.TouchHighlightImageButton;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.db.ReportStateDataSource;
import org.cm.podd.report.fragment.DashboardFeedFragment;
import org.cm.podd.report.fragment.NotificationListFragment;
import org.cm.podd.report.fragment.ReportFollowFragment;
import org.cm.podd.report.fragment.ReportInfoFragment;
import org.cm.podd.report.fragment.ReportListFragment;
import org.cm.podd.report.fragment.ReportStateFragment;
import org.cm.podd.report.model.FeedAdapter;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.model.ReportState;
import org.cm.podd.report.model.State;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard_feed_view);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        FontUtil.overrideFonts(this, findViewById(R.id.report_main_scrollview));

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        TabLayout.Tab tabInfo = tabLayout.newTab().setText(R.string.report_info);
        TabLayout.Tab tabState = tabLayout.newTab().setText(R.string.report_state);
        TabLayout.Tab tabFollow = tabLayout.newTab().setText(R.string.report_follow);

        tabLayout.addTab(tabInfo);
        tabLayout.addTab(tabState);
        tabLayout.addTab(tabFollow);

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

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

        try {
            setActivityTitleWithType(report.getString("reportTypeName"));

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
    public void onBackPressed() {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private void setActivityTitleWithType(String type) {
        String template = getString(R.string.report_activity_title_template);
        getSupportActionBar().setTitle(template.replace(":type", type));
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

}
