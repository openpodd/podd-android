package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.support.v4.app.FragmentTabHost;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.db.VisualizationAreaDataSource;
import org.cm.podd.report.fragment.VisualizationFragment;
import org.cm.podd.report.fragment.VisualizationListVolunteer;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.VisualizationAdministrationArea;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.VisualizationAreaService;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class VisualizationAreaActivity extends ActionBarActivity {

    private static final String TAG = "VisualAreaActivity";

    private long  id;
    private String name;
    private String parentName;

    private int month;
    private int year;

    private Bundle bundle;
    private FragmentTabHost mTabHost;

    Context context;
    VisualizationAreaDataSource visualizationAreaDataSource;

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            VisualizationAdministrationArea item = visualizationAreaDataSource.getFromAreaByMonth(id, month, year);

            String error = intent.getStringExtra("error");
            if (error != null && item == null) {
                Crouton.makeText(VisualizationAreaActivity.this, "Error on Server, please contact administration", Style.ALERT).show();
            } else {
                refreshData(item);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization_area);

        getWindow().setWindowAnimations(0);
        context = this;

        Intent intent = getIntent();
        bundle = intent.getExtras();

        id = intent.getLongExtra("id", -99);
        name = intent.getStringExtra("name");
        parentName = intent.getStringExtra("parentName");
        month = intent.getIntExtra("month", -99);
        year = intent.getIntExtra("year", -9999);

        mTabHost = (FragmentTabHost) findViewById (android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);
        mTabHost.addTab(
                mTabHost.newTabSpec("").setIndicator("", null),
                Fragment.class, null);

        mTabHost.setVisibility(View.INVISIBLE);

        visualizationAreaDataSource = new VisualizationAreaDataSource(this);
        VisualizationAdministrationArea area = visualizationAreaDataSource.getFromAreaByMonth(id, month, year);
        refreshData(area);

        registerReceiver(mSyncReceiver, new IntentFilter(VisualizationAreaService.SYNC));

        if (RequestDataUtil.hasNetworkConnection(this)) {
            if (area == null)
                showProgressDialog();

            startSyncVisualizationAreaService(id, month, year);
        }
    }

    ProgressDialog pd;

    public void showProgressDialog() {
        pd = new ProgressDialog(this);
        pd.setTitle("กำลังดึงข้อมูล");
        pd.setMessage("กรุณารอสักครู่");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }

    public void hideProgressDialog() {
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideProgressDialog();
    }

    private void refreshData(VisualizationAdministrationArea area){
        if (area != null) {
            int totalReport = area.getTotalReport();
            int positiveReport = area.getPositiveReport();
            int negativeReport = area.getNegativeReport();
            String volunteers = area.getVolunteers();
            String animalTypes = area.getAnimalType();
            String timeRanges = area.getTimeRanges();
            String grade = area.getGrade();

            bundle.putInt("totalReport", totalReport);
            bundle.putInt("positiveReport", positiveReport);
            bundle.putInt("negativeReport", negativeReport);
            bundle.putString("volunteers", volunteers);
            bundle.putString("animalTypes", animalTypes);
            bundle.putString("timeRanges", timeRanges);
            bundle.putString("grade", grade);

            mTabHost.getTabWidget().removeAllViews();
            mTabHost.getTabContentView().removeAllViews();
            mTabHost.clearAllTabs();

            mTabHost.addTab(
                    mTabHost.newTabSpec("area").setIndicator("พื้นที่", null),
                    VisualizationFragment.class, bundle);
            mTabHost.addTab(
                    mTabHost.newTabSpec("volunteer").setIndicator("อาสา", null),
                    VisualizationListVolunteer.class, bundle);

            Typeface face = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);

            for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
                final TextView textTitleView = (TextView) mTabHost.getTabWidget().getChildAt(i)
                        .findViewById(android.R.id.title);
                textTitleView.setTextSize(15);
                textTitleView.setTypeface(face, Typeface.BOLD);
            }

            mTabHost.setCurrentTab(0);
            mTabHost.setVisibility(View.VISIBLE);

            TextView emptyText = (TextView) findViewById(android.R.id.empty);
            emptyText.setVisibility(View.GONE);
        } else {
            TextView emptyText = (TextView) findViewById(android.R.id.empty);
            emptyText.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));
            emptyText.setVisibility(View.VISIBLE);
        }

        hideProgressDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_visualization));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);
        return true;
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
//        if (id == android.R.id.home){
//            this.finish();
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSyncReceiver);
    }

    private void startSyncVisualizationAreaService(long id, int month, int year) {
        Intent intent = new Intent(this, VisualizationAreaService.class);
        intent.putExtra("id", id);
        intent.putExtra("month", month);
        intent.putExtra("year", year);
        startService(intent);
    }

}
