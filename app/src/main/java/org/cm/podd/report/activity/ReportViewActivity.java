package org.cm.podd.report.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
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
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
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
import org.cm.podd.report.TouchHighlightImageButton;
import org.cm.podd.report.fragment.CommentFragment;
import org.cm.podd.report.fragment.FeedAdapter;
import org.cm.podd.report.fragment.VisualizationFragment;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;
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
    private ImageView flagImageView;
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
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;

    private FollowUpItemAdapter followUpItemAdapter;

    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard_feed_view);

        FontUtil.overrideFonts(this, getWindow().getDecorView());

        progressBar = (ProgressBar) findViewById(R.id.loading_spinner);
        contentWrapper = findViewById(R.id.report_view_content);
        contentWrapper.setVisibility(View.GONE);
        // init views.
        flagImageView = (ImageView) findViewById(R.id.flag_image_view);
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
        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

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
            setActivityTitleWithType(report.getString("reportTypeName"));

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

            if (projectTelephone.equals("null")){
                LinearLayout projectTelephoneLayout = (LinearLayout) findViewById(R.id.reporter_project_telephone_layout);
                projectTelephoneLayout.setVisibility(View.GONE);
            }

            formDataExplanationView.setText(FeedAdapter.stripHTMLTags(
                    report.getString("formDataExplanation")));

            // Add image controller.
            JSONArray images;
            try {
                images = report.getJSONArray("images");

                View rootView = getWindow().getDecorView().getRootView();

                for (int i = 0; i != images.length(); ++i) {
                    JSONObject item = images.getJSONObject(i);
                    final TouchHighlightImageButton imageView = new TouchHighlightImageButton(getApplicationContext());

                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    int rootWidth = rootView.getWidth();
                    int imageWidth = rootWidth - 40;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            imageWidth, imageWidth);
                    layoutParams.setMargins(0, 0, 10, 0);
                    imageView.setLayoutParams(layoutParams);

                    final String imageUrl = item.getString("imageUrl");
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // zoomImageFromThumb(imageView, imageUrl);
                            Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
                            intent.putExtra("imagePath", imageUrl);
                            startActivity(intent);
                        }
                    });

                    imageListView.addView(imageView);

                    new RemoteImageAsyncTask() {
                        @Override
                        protected void onPostExecute(Drawable thumb) {
                            imageView.setImageDrawable(thumb);
                        }
                    }.execute(item.getString("thumbnailUrl"));
                }

                if (imageListView.getChildCount() == 0){
                    imageListView.setVisibility(View.GONE);
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

            if(reportFlag.intValue() == 0){
                LinearLayout flagLayout = (LinearLayout) findViewById(R.id.flag_layout);
                flagLayout.setVisibility(View.GONE);
            }else{
                Uri flagUri = Uri.parse("android.resource://" + getPackageName() + "/" + FeedAdapter.flagColors[reportFlag.intValue()]);
                flagImageView.setImageURI(flagUri);
            }
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

    private void setActivityTitleWithType(String type) {
        String template = getString(R.string.report_activity_title_template);
        setTitle(template.replace(":type", type));
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

    /**
     * "Zooms" in a thumbnail view by assigning the high resolution image to a hidden "zoomed-in"
     * image view and animating its bounds to fit the entire activity content area. More
     * specifically:
     *
     * <ol>
     *   <li>Assign the high-res image to the hidden "zoomed-in" (expanded) image view.</li>
     *   <li>Calculate the starting and ending bounds for the expanded view.</li>
     *   <li>Animate each of four positioning/sizing properties (X, Y, SCALE_X, SCALE_Y)
     *       simultaneously, from the starting bounds to the ending bounds.</li>
     *   <li>Zoom back out by running the reverse animation on click.</li>
     * </ol>
     *
     * @param thumbView  The thumbnail view to zoom in.
     * @param imageUrl The high-resolution version of the image represented by the thumbnail.
     */
    private void zoomImageFromThumb(final View thumbView, String imageUrl) {
        // If there's an animation in progress, cancel it immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = (ImageView) findViewById(R.id.expanded_image);
        new RemoteImageAsyncTask() {
            @Override
            protected void onPostExecute(Drawable thumb) {
                expandedImageView.setImageDrawable(thumb);

                // Calculate the starting and ending bounds for the zoomed-in image. This step
                // involves lots of math. Yay, math.
                final Rect startBounds = new Rect();
                final Rect finalBounds = new Rect();
                final Point globalOffset = new Point();

                // The start bounds are the global visible rectangle of the thumbnail, and the
                // final bounds are the global visible rectangle of the container view. Also
                // set the container view's offset as the origin for the bounds, since that's
                // the origin for the positioning animation properties (X, Y).
                thumbView.getGlobalVisibleRect(startBounds);
                contentWrapper.getGlobalVisibleRect(finalBounds, globalOffset);
                startBounds.offset(-globalOffset.x, -globalOffset.y);
                finalBounds.offset(-globalOffset.x, -globalOffset.y);

                // Adjust the start bounds to be the same aspect ratio as the final bounds using the
                // "center crop" technique. This prevents undesirable stretching during the animation.
                // Also calculate the start scaling factor (the end scaling factor is always 1.0).
                float startScale;
                if ((float) finalBounds.width() / finalBounds.height()
                        > (float) startBounds.width() / startBounds.height()) {
                    // Extend start bounds horizontally
                    startScale = (float) startBounds.height() / finalBounds.height();
                    float startWidth = startScale * finalBounds.width();
                    float deltaWidth = (startWidth - startBounds.width()) / 2;
                    startBounds.left -= deltaWidth;
                    startBounds.right += deltaWidth;
                } else {
                    // Extend start bounds vertically
                    startScale = (float) startBounds.width() / finalBounds.width();
                    float startHeight = startScale * finalBounds.height();
                    float deltaHeight = (startHeight - startBounds.height()) / 2;
                    startBounds.top -= deltaHeight;
                    startBounds.bottom += deltaHeight;
                }

                // Hide the thumbnail and show the zoomed-in view. When the animation begins,
                // it will position the zoomed-in view in the place of the thumbnail.
                // thumbView.setAlpha(0f);
                expandedImageView.setVisibility(View.VISIBLE);

                // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of
                // the zoomed-in view (the default is the center of the view).
                expandedImageView.setPivotX(0f);
                expandedImageView.setPivotY(0f);

                // Construct and run the parallel animation of the four translation and scale properties
                // (X, Y, SCALE_X, and SCALE_Y).
                AnimatorSet set = new AnimatorSet();
                set
                        .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left,
                                finalBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top,
                                finalBounds.top))
                        .with(ObjectAnimator.ofInt(expandedImageView, "backgroundColor", 0x00000000,
                                0xDD000000))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;

                // Upon clicking the zoomed-in image, it should zoom back down to the original bounds
                // and show the thumbnail instead of the expanded image.
                final float startScaleFinal = startScale;
                expandedImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mCurrentAnimator != null) {
                            mCurrentAnimator.cancel();
                        }

                        // Animate the four positioning/sizing properties in parallel, back to their
                        // original values.
                        AnimatorSet set = new AnimatorSet();
                        set
                                .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                                .with(ObjectAnimator
                                        .ofFloat(expandedImageView, View.SCALE_X, startScaleFinal))
                                .with(ObjectAnimator
                                        .ofFloat(expandedImageView, View.SCALE_Y, startScaleFinal));
                        set.setDuration(mShortAnimationDuration);
                        set.setInterpolator(new DecelerateInterpolator());
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                thumbView.setAlpha(1f);
                                expandedImageView.setVisibility(View.GONE);
                                mCurrentAnimator = null;
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                thumbView.setAlpha(1f);
                                expandedImageView.setVisibility(View.GONE);
                                mCurrentAnimator = null;
                            }
                        });
                        set.start();
                        mCurrentAnimator = set;
                    }
                });
            }
        }.execute(imageUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_report));
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);
        return true;
    }

}
