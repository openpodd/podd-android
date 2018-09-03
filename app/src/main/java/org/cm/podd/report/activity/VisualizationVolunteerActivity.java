package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.VisualizationVolunteerDataSource;
import org.cm.podd.report.fragment.VisualizationFragment;
import org.cm.podd.report.model.VisualizationVolunteer;
import org.cm.podd.report.service.VisualizationVolunteerService;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class VisualizationVolunteerActivity extends AppCompatActivity {

    private long  id;
    private String name;
    private String parentName;

    private int month;
    private int year;

    private Bundle bundle;
    private Fragment mCurrentFragment;

    Context context;
    VisualizationVolunteerDataSource visualizationVolunteerDataSource;

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            VisualizationVolunteer item = visualizationVolunteerDataSource.getFromVolunteerFromMonth(id, month, year);

            String error = intent.getStringExtra("error");
            if (error != null && item == null){
                Crouton.makeText(VisualizationVolunteerActivity.this, getString(R.string.http_server_error), Style.ALERT).show();
                hideProgressDialog();
            }
            else {
                refreshData(item);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization_volunteer);

        getWindow().setWindowAnimations(0);
        context = this;

        Intent intent = getIntent();
        bundle = intent.getExtras();

        id = intent.getLongExtra("id", -99);
        name = intent.getStringExtra("name");
        parentName = intent.getStringExtra("parentName");

        month = intent.getIntExtra("month", -99);
        year = intent.getIntExtra("year", -9999);

        visualizationVolunteerDataSource = new VisualizationVolunteerDataSource(this);
        VisualizationVolunteer volunteer = visualizationVolunteerDataSource.getFromVolunteerFromMonth(id, month, year);

        registerReceiver(mSyncReceiver, new IntentFilter(VisualizationVolunteerService.SYNC));

        if (RequestDataUtil.hasNetworkConnection(this)) {
            if (volunteer == null)
                showProgressDialog();

            startSyncVisualizationVolunteerService(id, month, year);
        } else {
            refreshData(volunteer);
        }

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("VisualizationVolunteer");
        tracker.send(new HitBuilders.AppViewBuilder().build());
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void refreshData(VisualizationVolunteer volunteer){
        if (volunteer != null) {
            int totalReport = volunteer.getTotalReport();
            int positiveReport = volunteer.getPositiveReport();
            int negativeReport = volunteer.getNegativeReport();
            String animalTypes = volunteer.getAnimalType();
            String timeRanges = volunteer.getTimeRanges();
            String grade = volunteer.getGrade();

            bundle.putInt("totalReport", totalReport);
            bundle.putInt("positiveReport", positiveReport);
            bundle.putInt("negativeReport", negativeReport);
            bundle.putString("animalTypes", animalTypes);
            bundle.putString("timeRanges", timeRanges);
            bundle.putString("grade", grade);

            setTitle(name);

            mCurrentFragment = new VisualizationFragment();
            mCurrentFragment.setArguments(bundle);

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                    .commit();

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
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_visualization_volunteer));
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setHomeAsUpIndicator(0);
//        actionBar.setHomeButtonEnabled(true);
//        actionBar.setLogo(R.drawable.arrow_left_with_pad);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

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
        if (id == android.R.id.home){
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSyncReceiver);
    }

    private void startSyncVisualizationVolunteerService(long id, int month, int year) {
        Intent intent = new Intent(this, VisualizationVolunteerService.class);
        intent.putExtra("id", id);
        intent.putExtra("month", month);
        intent.putExtra("year", year);
        startService(intent);
    }
}
