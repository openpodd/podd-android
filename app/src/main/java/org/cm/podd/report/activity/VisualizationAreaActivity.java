package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
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
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class VisualizationAreaActivity extends ActionBarActivity {

    private static final String TAG = "VisualizationAreaActivity";

    private long  id;
    private String name;
    private String parentName;
    private PieChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization_area);

        id = getIntent().getLongExtra("id", -99);
        name = getIntent().getStringExtra("name");
        parentName = getIntent().getStringExtra("parentName");

        TextView textNameView = (TextView) findViewById(R.id.name);
        textNameView.setText(name);

        TextView textParentNameView = (TextView) findViewById(R.id.parent_name);
        textParentNameView.setText(parentName);

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
            return RequestDataUtil.get("/summary/areas/show-detail/?month=01/2015&administrationAreaId=" + id, null, accessToken);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    JSONObject obj = new JSONObject(resp.getRawData());

                    String grade = obj.optString("grade");
                    TextView textGradeView =  (TextView) findViewById(R.id.grade);
                    textGradeView.setText("ระดับการประเมิน :" + grade);

                    String totalReport = obj.optString("totalReport");
                    TextView textTotalReportView =  (TextView) findViewById(R.id.totalReport);
                    textTotalReportView.setText("จำนวนรายงานทั้งหมด : " + totalReport + "รายงาน");

                    int positiveReport = obj.optInt("positiveReport");
                    int negativeReport = obj.optInt("negativeReport");

                    mChart = (PieChart) findViewById(R.id.chart1);
                    int [] count = { positiveReport, negativeReport };
                    String [] name = { "Positive", "Negative" };

                    setData(name, count, positiveReport + negativeReport);
                    mChart.animateXY(500, 500);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(VisualizationAreaActivity.this, "Error on Server, please contact administration", Style.ALERT).show();
                } else {
                    Crouton.makeText(VisualizationAreaActivity.this, "Administration area is incorrect!", Style.ALERT).show();
                }

            }
            hideProgressDialog();

        }
    }

    private void setData(String[] name, int[] count, float range) {

        float mult = range;

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        for (int i = 0; i < count.length; i++) {
            yVals1.add(new Entry((float) (count[i]), i));
        }

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = 0; i < name.length; i++)
            xVals.add(name[i]);

        PieDataSet set1 = new PieDataSet(yVals1, "");
        set1.setSliceSpace(3f);


        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        set1.setColors(colors);

        PieData data = new PieData(xVals, set1);
        mChart.setData(data);

        // undo all highlights
        mChart.highlightValues(null);

        mChart.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_visualization_area, menu);
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
