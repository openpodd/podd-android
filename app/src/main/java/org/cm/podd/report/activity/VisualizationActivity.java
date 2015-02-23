package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.VisualizationAreaFragment;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class VisualizationActivity extends ActionBarActivity {

    private static final String TAG = "VisualizationAreaActivity";

    private long  id;
    private String name;
    private String parentName;

    private int month;
    private int year;

    private PieChart mChart;
    private Bundle bundle;

    Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization);

        getWindow().setWindowAnimations(0);

        Intent intent = getIntent();
        bundle = intent.getExtras();

        id = intent.getLongExtra("id", -99);
        name = intent.getStringExtra("name");
        parentName = intent.getStringExtra("parentName");
        month = intent.getIntExtra("month", -99);
        year = intent.getIntExtra("year", -9999);

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

            String reqData = null;
            try {
                JSONObject json = new JSONObject();
                json.put("administrationAreaId", id);
                json.put("month", "01/2015");
                reqData = json.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
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

                    bundle.putString("grade", grade);
                    bundle.putInt("totalReport", totalReport);
                    bundle.putInt("positiveReport", positiveReport);
                    bundle.putInt("positiveReport", positiveReport);

                    mCurrentFragment = new VisualizationAreaFragment();
                    mCurrentFragment.setArguments(bundle);

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                            .commit();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(VisualizationActivity.this, "Error on Server, please contact administration", Style.ALERT).show();
                } else {
                    Crouton.makeText(VisualizationActivity.this, "Administration area is incorrect!", Style.ALERT).show();
                }

            }
            hideProgressDialog();

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_visualization_area));
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
