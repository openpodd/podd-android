package org.cm.podd.report.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.TouchHighlightImageButton;
import org.cm.podd.report.fragment.FeedAdapter;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by siriwat on 2/23/15.
 */
public class ReportViewActivity extends ActionBarActivity {

    private static final String TAG = "ReportViewActivity";

    private static final int[] flagColors = new int[]{
            R.drawable.flag_ignore,
            R.drawable.flag_ok,
            R.drawable.flag_contact,
            R.drawable.flag_follow,
            R.drawable.flag_case,
            R.drawable.blank,

    };

    private Long id;
    private Long currentFlag;
    private Long oldFlag;

    private ScrollView scrollView;
    private ProgressBar progressBar;
    private View contentWrapper;

    private ImageView flagImageView;
    private TextView flagView;
    private Spinner flagSpinnerView;
    private HintAdapter mFlagAdapter;
    private RelativeLayout caseDialog;
    private RadioGroup caseRadioGroup;
    private ProgressBar caseDialogProgressBar;

    private TextView dateView;
    private TextView incidentDateView;
    private TextView typeView;
    private TextView areaView;
    private TextView createdByView;
    private TextView createdByTelephoneView;
    private TextView createdByProjectTelephoneView;
    private TextView formDataExplanationView;

    private LinearLayout reportFollowUpList;
    private TextView reportFollowUpTitle;
    private TextView countFollowUpTextView;

    private LinearLayout imageListView;
    private LinearLayout sectionPostComment;
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;

    private RelativeLayout sectionComment;
    private LinearLayout alertComment;
    private RelativeLayout moveToCommentButton;

    private FragmentManager fragmentManager;
    private FollowUpItemAdapter followUpItemAdapter;

    private BroadcastReceiver mReceiver;
    private Bundle bundle;

    // quick fix.
    private Long selectedCaseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard_feed_view);

        FontUtil.overrideFonts(this, getWindow().getDecorView());

        scrollView = (ScrollView) findViewById(R.id.report_main_scrollview);
        progressBar = (ProgressBar) findViewById(R.id.loading_spinner);
        contentWrapper = findViewById(R.id.report_view_content);
        contentWrapper.setVisibility(View.GONE);
        // init views.

        // Flag.
        flagSpinnerView = (Spinner) findViewById(R.id.flag_spinner);
        caseDialog = (RelativeLayout) findViewById(R.id.case_dialog);
        caseRadioGroup = (RadioGroup) findViewById(R.id.case_radio);
        caseDialogProgressBar = (ProgressBar) findViewById(R.id.case_dialog_progressbar);
        // init flag element.
        caseDialog.setVisibility(View.GONE);

        dateView = (TextView) findViewById(R.id.report_view_report_date);
        incidentDateView = (TextView) findViewById(R.id.report_view_report_incidentDate);
        typeView = (TextView) findViewById(R.id.report_view_report_type);
        areaView = (TextView) findViewById(R.id.report_view_report_area);
        createdByView = (TextView) findViewById(R.id.report_view_report_created_by);
        createdByTelephoneView = (TextView) findViewById(R.id.report_view_reporter_telephone);
        createdByProjectTelephoneView = (TextView) findViewById(R.id.report_view_reporter_project_telephone);
        formDataExplanationView = (TextView) findViewById(R.id.report_view_form_data_explanation);
        // Comment.
        sectionComment = (RelativeLayout) findViewById(R.id.section_comment);
        alertComment = (LinearLayout) findViewById(R.id.alert_comment);
        moveToCommentButton = (RelativeLayout) findViewById(R.id.move_to_comment);
        // Follow-up
        reportFollowUpList = (LinearLayout) findViewById(R.id.report_follow_up_list);
        reportFollowUpTitle = (TextView) findViewById(R.id.report_follow_up_title);
        countFollowUpTextView = (TextView) findViewById(R.id.report_follow_up_count);
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

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                alertComment.setVisibility(View.INVISIBLE);
                return false;
            };
        });
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
        progressBar.setVisibility(View.GONE);
        contentWrapper.setVisibility(View.VISIBLE);
        caseDialog.setVisibility(View.GONE);

        try {
            setActivityTitleWithType(report.getString("reportTypeName"));

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

                    int widthRootView = getResources().getDisplayMetrics().widthPixels;
                    int widthMargin = StyleUtil.convertDpToPx(23.0F, getResources().getDisplayMetrics());

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            widthRootView - 2 * widthMargin,
                            StyleUtil.convertDpToPx(157.50F, getResources().getDisplayMetrics()));
                    layoutParams.setMargins(0, 0,
                            StyleUtil.convertDpToPx(10F, getResources().getDisplayMetrics()), 0);
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
                    HorizontalScrollView horizontalScrollView = (HorizontalScrollView) findViewById(R.id.report_image_list_scrollview);
                    horizontalScrollView.setVisibility(View.GONE);
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
            currentFlag = reportFlag;
            oldFlag = reportFlag;

            // Flag spinner.
            mFlagAdapter = new HintAdapter(getApplicationContext(),
                    getResources().getStringArray(R.array.flags_with_hint));
            flagSpinnerView.setAdapter(mFlagAdapter);
            if (reportFlag > 0) {
                flagSpinnerView.setSelection(reportFlag.intValue() - 1);
            } else {
                flagSpinnerView.setSelection(mFlagAdapter.getCount());
            }
            flagSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    final int flag = position + 1;
                    if (position != mFlagAdapter.getCount() && oldFlag.intValue() != flag) {
                        // Show case list dialog when flag = 4 (follow).
                        if (flag == 4) {
                            caseRadioGroup.removeAllViews();
                            caseDialog.setVisibility(View.VISIBLE);

                            try {
                                String query = "administrationArea:" + Long.toString(report.getLong("administrationAreaId")) +
                                                " AND date:last 70 days" +
                                                " AND flag:case";

                                final Long parentId = id;
                                Button okButton = (Button) findViewById(R.id.ok_button);
                                okButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        caseDialog.setVisibility(View.GONE);
                                        follow(Long.parseLong(Integer.toString(flag)), selectedCaseId);
                                    }
                                });
                                Button cancelButton = (Button) findViewById(R.id.cancel_button);
                                cancelButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        caseDialog.setVisibility(View.GONE);
                                        flagSpinnerView.setSelection(currentFlag.intValue() - 1);
                                    }
                                });

                                FilterService.FilterAsyncTask task = new FilterService.FilterAsyncTask() {
                                    @Override
                                    protected void onPostExecute(RequestDataUtil.ResponseObject responseObject) {
                                        caseDialogProgressBar.setVisibility(View.GONE);

                                        try {
                                            JSONObject result = new JSONObject(responseObject.getRawData());
                                            JSONArray items = result.getJSONArray("results");

                                            RadioButton radioChoiceTemplate = (RadioButton) findViewById(R.id.case_choice_template);

                                            for (int i = 0; i != items.length(); i++) {
                                                JSONObject item = items.getJSONObject(i);
                                                final Long caseId = item.getLong("id");

                                                if (caseId == parentId) {
                                                    continue;
                                                }

                                                RadioButton radioButton = new RadioButton(getApplicationContext());
                                                radioButton.setText(
                                                        getString(R.string.follow_up_report_item_template)
                                                                .replace(":id", Long.toString(item.getLong("id"))));
                                                radioButton.setLayoutParams(radioChoiceTemplate.getLayoutParams());
                                                radioButton.setTextColor(radioChoiceTemplate.getTextColors());
                                                radioButton.setTextSize(radioChoiceTemplate.getTextSize());
                                                radioButton.setBackgroundColor(radioChoiceTemplate.getDrawingCacheBackgroundColor());
                                                radioButton.setVisibility(View.VISIBLE);

                                                radioButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        selectedCaseId = caseId;
                                                    }
                                                });

                                                caseRadioGroup.addView(radioButton);
                                            }
                                        } catch (JSONException e) {
                                            // do nothing.
                                            Log.e(TAG, "Error parsing JSON data", e);
                                        }
                                    }
                                };
                                task.setContext(getApplicationContext());
                                task.execute(query, null);
                            } catch (JSONException e) {
                                // TODO: reverse choice.
                                Log.e(TAG, "Error parsing JSON data", e);
                            }

                        } else {
                            updateFlag(Long.parseLong(Integer.toString(flag)));
                        }
                    } else {
                        // do nothings.
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            // Add follow up if exists.
            if (reportFlag == 5) {
                fetchFollowUpReports(report.getLong("id"));
                // Start follow-up activity when click.
                reportFollowUpTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getApplicationContext(), ReportFollowUpActivity.class);
                        intent.putExtra("parentReportId", id);
                        startActivity(intent);
                    }
                });
            }

            Long parentId;
            try {
                parentId = report.getLong("parent");
            } catch (JSONException e) {
                parentId = 0L;
            }
            // In case this is a follow-up report which parent exists, then show only parent.
            if (parentId != 0) {
                reportFollowUpTitle.setText(R.string.follow_up_parent);
                countFollowUpTextView.setText("1");

                LayoutInflater inflater = LayoutInflater.from(this);
                View view = inflater.inflate(R.layout.list_item_follow_up_report, null, false);

                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText(
                        getString(R.string.follow_up_report_item_template)
                                .replace(":id", Long.toString(parentId)));

                final Long innerParentId = parentId;
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ReportViewActivity.this, ReportViewActivity.class);
                        intent.putExtra("id", innerParentId);
                        startActivity(intent);
                    }
                });

                reportFollowUpList.removeAllViews();
                reportFollowUpList.addView(view);
            }

            final Long reportId = Long.parseLong(report.getString("id"));
            moveToCommentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    commentText.setFocusableInTouchMode(true);
//                    commentText.setFocusable(true);
//
//                    commentText.requestFocus();
//                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    inputMethodManager.showSoftInput(commentText, InputMethodManager.SHOW_IMPLICIT);
                    Intent intent = new Intent(ReportViewActivity.this, ReportCommentActivity.class);
                    intent.putExtra("reportId", reportId);
                    startActivity(intent);
//                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });

            sectionComment.setVisibility(View.VISIBLE);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
        } catch (IllegalStateException e){
            Log.e(TAG, "Error IllegalStateException", e);
        }

    }

    private void follow(final Long flag, final Long parentId) {
        oldFlag = currentFlag;

        ReportService.FollowAsyncTask task = new ReportService.FollowAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                if (resp.getStatusCode() == 201 || resp.getStatusCode() == 200) {
                    currentFlag = flag;
                    oldFlag = flag;
                    // notify report data.
                    Intent intent = new Intent(ReportService.ACTION_FLAG_SET_DONE);
                    intent.putExtra("reportId", id);
                    intent.putExtra("flag", flag);
                    sendBroadcast(intent);
                } else {
                    // Do something.
                }
            }
        };

        task.setContext(getApplicationContext());
        task.execute(Long.toString(id), Long.toString(parentId));
    }

    private void updateFlag(final Long flag) {
        oldFlag = currentFlag;

        ReportService.FlagAsyncTask task = new ReportService.FlagAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                if (resp.getStatusCode() == 201) {
                    currentFlag = flag;
                    oldFlag = flag;
                    // notify report data.
                    Intent intent = new Intent(ReportService.ACTION_FLAG_SET_DONE);
                    intent.putExtra("reportId", id);
                    intent.putExtra("flag", flag);
                    sendBroadcast(intent);
                } else {
                    // Do something.
                }
            }
        };

        task.setContext(getApplicationContext());
        task.execute(Long.toString(id), Long.toString(flag));
    }

    public class HintAdapter extends ArrayAdapter<String> {

        public HintAdapter(Context context, String[] objects) {
            super(context, android.R.layout.simple_list_item_1, android.R.id.text1, objects);
        }

        @Override
        public int getCount() {
            // don't display last item. It is used as hint.
            int count = super.getCount();
            return count > 0 ? count - 1 : count;
        }

        public int getRealCount() {
            return super.getCount();
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.flag_spinner_item, parent, false);
            }

            ImageView iconView = (ImageView) view.findViewById(R.id.flag_icon);
            iconView.setImageResource(flagColors[position]);

            TextView textView = (TextView) view.findViewById(R.id.flag_name);
            textView.setText(getItem(position));

            return view;
        }

    }

    private void fetchFollowUpReports(Long reportId) {
        ReportService.FollowUpAsyncTask task = new ReportService.FollowUpAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                try {
                    JSONArray followUpReports = new JSONArray(resp.getRawData());
                    int followUpCount = followUpReports.length();

                    if (followUpCount > 0){
                        countFollowUpTextView.setText(Integer.toString(followUpCount));
                    }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_report));
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);
        return true;
    }

}
