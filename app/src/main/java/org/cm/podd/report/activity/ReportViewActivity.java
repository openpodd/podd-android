package org.cm.podd.report.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.FeedAdapter;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by siriwat on 2/23/15.
 */
public class ReportViewActivity extends ActionBarActivity {

    private static final String TAG = "ReportViewActivity";

    private Long id;

    private ProgressBar progressBar;
    private View contentWrapper;

    private TextView dateView;
    private TextView incidentDateView;
    private TextView typeView;
    private TextView areaView;
    private TextView createdByView;
    private TextView createdByTelephoneView;
    private TextView createdByProjectTelephoneView;
    private TextView formDataExplanationView;
    private ListView followUpListView;
    private TextView emptyFollowUpListView;

    private FollowUpItemAdapter followUpItemAdapter;

    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard_feed_view);
        progressBar = (ProgressBar) findViewById(R.id.loading_spinner);
        contentWrapper = findViewById(R.id.report_view_content);
        contentWrapper.setVisibility(View.GONE);
        // init views.
        dateView = (TextView) findViewById(R.id.report_view_report_date);
        incidentDateView = (TextView) findViewById(R.id.report_view_report_incidentDate);
        typeView = (TextView) findViewById(R.id.report_view_report_type);
        areaView = (TextView) findViewById(R.id.report_view_report_area);
        createdByView = (TextView) findViewById(R.id.report_view_report_created_by);
        createdByTelephoneView = (TextView) findViewById(R.id.report_view_reporter_telephone);
        createdByProjectTelephoneView = (TextView) findViewById(R.id.report_view_reporter_project_telephone);
        formDataExplanationView = (TextView) findViewById(R.id.report_view_form_data_explanation);
        followUpListView = (ListView) findViewById(R.id.report_follow_up_list);
        followUpListView.setVisibility(View.GONE);
        emptyFollowUpListView = (TextView) findViewById(R.id.empty_follow_up_list_text);
        emptyFollowUpListView.setVisibility(View.VISIBLE);

        // register receiver.
        mReceiver = new BroadcastReceiver() {
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
        registerReceiver(mReceiver, new IntentFilter(ReportService.ACTION_FETCH_DONE));

        id = getIntent().getLongExtra(ReportService.PARAM_REPORT_ID, 0);
        ReportService.doFetch(getApplicationContext(), id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    private static Spanned linkify(String text, String uri) {
        return Html.fromHtml("<a href=\"" + uri + "\">" + text + "</a>");
    }

    private void viewReport(final JSONObject report) {
        Long parentId = null;

        progressBar.setVisibility(View.GONE);
        contentWrapper.setVisibility(View.VISIBLE);

        try {
            dateView.setText(DateUtil.convertToThaiDateTime(
                    DateUtil.fromJsonDateString(report.getString("date"))));
            incidentDateView.setText(DateUtil.convertToThaiDate(
                    DateUtil.fromJsonDateString(report.getString("incidentDate"))));
            typeView.setText(report.getString("reportTypeName"));
            areaView.setText(report.getString("administrationAreaAddress"));
            createdByView.setText(report.getString("createdBy"));

            String telephone = report.getString("createdByTelephone");
            createdByTelephoneView.setText(linkify(telephone, "tel:" + telephone));
            createdByTelephoneView.setMovementMethod(LinkMovementMethod.getInstance());

            String projectTelephone = report.getString("createdByProjectMobileNumber");
            createdByProjectTelephoneView.setText(linkify(projectTelephone, "tel:" + projectTelephone));
            createdByProjectTelephoneView.setMovementMethod(LinkMovementMethod.getInstance());

            formDataExplanationView.setText(FeedAdapter.stripHTMLTags(
                    report.getString("formDataExplanation")));

            // Add follow up if exists.
            if (report.getLong("flag") == 5) {
                fetchFollowUpReports(report.getLong("id"));
            }
            parentId = report.getLong("parent");
            if (parentId != 0) {
                TextView reportFollowUpTitle = (TextView) findViewById(R.id.report_follow_up_title);
                reportFollowUpTitle.setText(R.string.follow_up_parent);

                emptyFollowUpListView.setVisibility(View.GONE);

                ArrayList<String> textList = new ArrayList<String>();
                textList.add(Long.toString(report.getLong("parent")));

                followUpItemAdapter = new FollowUpItemAdapter(getApplicationContext(),
                        R.layout.list_item_follow_up_report, textList);
                followUpListView.setAdapter(followUpItemAdapter);

                followUpListView.setVisibility(View.VISIBLE);
                followUpListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Intent intent = new Intent(getApplicationContext(), ReportViewActivity.class);
                        intent.putExtra("id", Long.parseLong(followUpItemAdapter.getItem(position)));
                        startActivityForResult(intent, 0);
                    }
                });
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
        }
    }

    private void fetchFollowUpReports(Long reportId) {
        ReportService.FollowUpAsyncTask task = new ReportService.FollowUpAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                try {
                    JSONArray followUpReports = new JSONArray(resp.getRawData());
                    ArrayList<String> textList = new ArrayList<String>();

                    for (int i = 0; i != followUpReports.length(); ++i) {
                        JSONObject item = followUpReports.getJSONObject(i);
                        textList.add(item.getString("id"));

                        // Append text view to list view.
                    }

                    followUpItemAdapter = new FollowUpItemAdapter(getContext(),
                            R.layout.list_item_follow_up_report, textList);
                    followUpListView.setAdapter(followUpItemAdapter);

                    followUpListView.setVisibility(View.VISIBLE);
                    followUpListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Intent intent = new Intent(context, ReportViewActivity.class);
                            intent.putExtra("id", Long.parseLong(followUpItemAdapter.getItem(position)));
                            startActivityForResult(intent, 0);
                        }
                    });
                    emptyFollowUpListView.setVisibility(View.GONE);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON data", e);
                }
            }
        };
        task.setContext(getApplicationContext());
        task.execute(Long.toString(reportId));
    }

    private class FollowUpItemAdapter extends ArrayAdapter<String> {

        Context context;
        int resource;
        List<String> items;

        public FollowUpItemAdapter(Context context, int resource, List<String> items) {
            super(context, resource, items);
            this.context = context;
            this.resource = resource;
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(resource, parent, false);

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(
                    getString(R.string.follow_up_report_item_template)
                        .replace(":id", getItem(position)));

            return view;
        }

    }
}
