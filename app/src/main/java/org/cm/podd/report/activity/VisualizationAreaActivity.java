package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.VisualizationFragment;
import org.cm.podd.report.fragment.VisualizationListVolunteer;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

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
        if (RequestDataUtil.hasNetworkConnection(this)) {
            new VisualizationAreaTask().execute((Void[]) null);
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

    /**
     * GET AREA
     */
    public class VisualizationAreaTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
            String accessToken = sharedPrefUtil.getAccessToken();
            return RequestDataUtil.get("/summary/areas/show-detail/?month=" + month + "/" + year + "&administrationAreaId=" + id, null, accessToken);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                Typeface face = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);

                try {
                    JSONObject obj = new JSONObject(resp.getRawData());

                    String grade = obj.optString("grade");
                    int totalReport = obj.optInt("totalReport");

                    int positiveReport = obj.optInt("positiveReport");
                    int negativeReport = obj.optInt("negativeReport");
                    String volunteers =  obj.optString("reporters");
                    String animalTypes =  obj.optString("animalTypes");
                    String timeRanges =  obj.optString("timeRanges");

                    bundle.putString("grade", grade);

                    bundle.putInt("totalReport", totalReport);
                    bundle.putInt("positiveReport", positiveReport);
                    bundle.putInt("negativeReport", negativeReport);

                    bundle.putString("volunteers", volunteers);
                    bundle.putString("animalTypes", animalTypes);
                    bundle.putString("timeRanges", timeRanges);

                    mTabHost.getTabWidget().removeAllViews();
                    mTabHost.getTabContentView().removeAllViews();
                    mTabHost.clearAllTabs();

                    mTabHost.addTab(
                            mTabHost.newTabSpec("area").setIndicator("พื้นที่", null),
                            VisualizationFragment.class, bundle);
                    mTabHost.addTab(
                            mTabHost.newTabSpec("volunteer").setIndicator("อาสา", null),
                            VisualizationListVolunteer.class, bundle);

                    mTabHost.setCurrentTab(0);
                    mTabHost.setVisibility(View.VISIBLE);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(VisualizationAreaActivity.this, "Error on Server, please contact administration", Style.ALERT).show();
                } else {
                    Crouton.makeText(VisualizationAreaActivity.this, "Administration area is incorrect!", Style.ALERT).show();
                    TextView emptyText = (TextView) findViewById (android.R.id.empty);
                    emptyText.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));
                    emptyText.setVisibility(View.VISIBLE);
                }

            }
            hideProgressDialog();

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_visualization));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
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

        return super.onOptionsItemSelected(item);
    }
}
