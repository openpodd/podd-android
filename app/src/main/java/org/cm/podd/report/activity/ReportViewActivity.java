package org.cm.podd.report.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.CommentFragment;
import org.cm.podd.report.fragment.FeedAdapter;
import org.cm.podd.report.fragment.VisualizationFragment;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

    private TextView flagView;
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

    private LinearLayout imageListView;

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
        flagView = (TextView) findViewById(R.id.report_flag);
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
        // image list view.
        imageListView = (LinearLayout) findViewById(R.id.report_image_list);

        // register receiver.
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Receiving action " + intent.getAction());

                try {
                    JSONObject report = new JSONObject(intent.getStringExtra("report"));
                    // First, check if we receive correct report data.
                    if (report.getLong("id") == id) {
                        viewReport(report);
                    }
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
        Long parentId;

        progressBar.setVisibility(View.GONE);
        contentWrapper.setVisibility(View.VISIBLE);

        try {
            setActivityTitleWithReportId(report.getLong("id"));

            Bundle bundle = new Bundle();
            bundle.putLong("reportId", Long.parseLong(report.getString("id")));

            Fragment commentFragment = new CommentFragment();
            commentFragment.setArguments(bundle);

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_comment, commentFragment, commentFragment.getClass().getSimpleName())
                    .commit();

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

            // Add image controller.
            JSONArray images;
            try {
                images = report.getJSONArray("images");

                View rootView = getWindow().getDecorView().getRootView();

                for (int i = 0; i != images.length(); ++i) {
                    JSONObject item = images.getJSONObject(i);
                    final ImageView imageView = new ImageView(getApplicationContext());

                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    int rootWidth = rootView.getWidth();
                    int imageWidth = rootWidth - 40;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            imageWidth, imageWidth);
                    layoutParams.setMargins(0, 0, 10, 0);
                    imageView.setLayoutParams(layoutParams);

                    imageListView.addView(imageView);

                    new RemoteImageAsyncTask() {
                        @Override
                        protected void onPostExecute(Drawable thumb) {
                            imageView.setImageDrawable(thumb);
                        }
                    }.execute(item.getString("thumbnailUrl"));
                }
            } catch (JSONException e) {
                // nothing.
            }


            // Add flag controller
            Long reportFlag;
            try {
                reportFlag = report.getLong("flag");
            } catch (JSONException e) {
                reportFlag = 0L;
            }
            flagView.setText(getResources().getStringArray(
                    R.array.flags_optional)[reportFlag.intValue()]);

            // Add follow up if exists.
            if (reportFlag == 5) {
                fetchFollowUpReports(report.getLong("id"));
            }
            try {
                parentId = report.getLong("parent");
            } catch (JSONException e) {
                parentId = 0L;
            }
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

    private void updateFlag(Long flag) {
        ReportService.FlagAsyncTask task = new ReportService.FlagAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                if (resp.getStatusCode() == 201) {
                    // Do something.
                } else {
                    // Do something.
                }
            }
        };

        task.setContext(getApplicationContext());
        task.execute(Long.toString(id), Long.toString(flag));
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

    private void setActivityTitleWithReportId(Long id) {
        String template = getString(R.string.report_activity_title_template);
        setTitle(template.replace(":id", Long.toString(id)));
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
}
