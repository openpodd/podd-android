/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cm.podd.report.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.FollowAlertDataSource;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.fragment.ReportConfirmFragment;
import org.cm.podd.report.fragment.ReportDataInterface;
import org.cm.podd.report.fragment.ReportImageFragment;
import org.cm.podd.report.fragment.ReportLocationFragment;
import org.cm.podd.report.fragment.ReportNavigationChangeCallbackInterface;
import org.cm.podd.report.fragment.ReportNavigationInterface;
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.FormIterator;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.model.Trigger;
import org.cm.podd.report.model.validation.ValidationResult;
import org.cm.podd.report.model.view.PageView;
import org.cm.podd.report.model.view.QuestionView;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.service.FollowAlertReceiver;
import org.cm.podd.report.service.FollowAlertScheduleService;
import org.cm.podd.report.service.FollowAlertService;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ReportActivity extends ActionBarActivity
        implements ReportNavigationInterface, ReportDataInterface, QuestionView.SoftKeyActionHandler, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "ReportActivity";
    public static final int REQUEST_FOR_OPEN_LOCATION_SERVICE_DIALOG = 200;
    private Button prevBtn;
    private Button nextBtn;
    private View disableMaskView;
    private View formView;
    private View locationView;
    private TextView countdownTextView;
    private TextView textProgressLocationView;
    private boolean testReport = false;

    private String currentFragment;
    private ReportDataSource reportDataSource;
    private ReportTypeDataSource reportTypeDataSource;
    private ReportQueueDataSource reportQueueDataSource;

    private FollowAlertDataSource followAlertDataSource;

    private long reportId;
    private long reportType;
    private boolean follow;
    private FormIterator formIterator;
    private Trigger trigger;


    private double currentLatitude = 0.00;
    private double currentLongitude = 0.00;

    private Date reportDate;
    private long reportRegionId;
    private String remark;
    private int reportSubmit = 0;

    private SharedPrefUtil sharedPrefUtil;

    protected FollowAlertReceiver mAlertReceiver = new FollowAlertReceiver();
    private long startTime;

    private long parentReportId = -1;
    private GoogleApiClient mGoogleApiClient;
    private String followActionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mAlertReceiver,
                new IntentFilter(FollowAlertService.TAG));

        sharedPrefUtil = new SharedPrefUtil(this);

        setContentView(R.layout.activity_report);

        formView = findViewById(R.id.form);
        locationView = findViewById(R.id.location);

        textProgressLocationView = (TextView) findViewById(R.id.progress_location_text);
        textProgressLocationView.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));
        countdownTextView = (TextView) findViewById(R.id.countdownTextView);
        countdownTextView.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));

        prevBtn = (Button) findViewById(R.id.prevBtn);
        nextBtn = (Button) findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextScreen();
            }
        });
        nextBtn.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        prevBtn.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));

        disableMaskView = findViewById(R.id.disableMask);

        reportDataSource = new ReportDataSource(this);
        reportTypeDataSource = new ReportTypeDataSource(this);
        reportQueueDataSource = new ReportQueueDataSource(this);

        followAlertDataSource = new FollowAlertDataSource(this);

        if (savedInstanceState != null) {
            currentFragment = savedInstanceState.getString("currentFragment");
            reportId = savedInstanceState.getLong("reportId");
            reportType = savedInstanceState.getLong("reportType");
            follow = savedInstanceState.getBoolean("follow");
            testReport = savedInstanceState.getBoolean("testReport");
            formIterator = (FormIterator) savedInstanceState.getSerializable("formIterator");
            trigger = formIterator.getForm().getTrigger();
            reportSubmit = savedInstanceState.getInt("reportSubmit");
            followActionName = savedInstanceState.getString("followActionName");
            Log.d(TAG, "onCreate from savedInstance, testFlag = " + testReport);

            currentLatitude = savedInstanceState.getDouble("currentLatitude");
            currentLongitude = savedInstanceState.getDouble("currentLongitude");

        } else {
            Intent intent = getIntent();
            reportType = intent.getLongExtra("reportType", 0);          // mandatory
            reportId = intent.getLongExtra("reportId", -99);            // optional
            follow = intent.getBooleanExtra("follow", false);           // optional
            testReport = intent.getBooleanExtra("test", false);
            int startPageId = intent.getIntExtra("startPageId", -1);
            Log.d(TAG, "onCreate, testFlag = " + testReport);

            if (follow) {
                parentReportId = reportId;
                followActionName = intent.getStringExtra("followActionName");
                if (followActionName == null) {
                    followActionName = "follow";
                }
                reportId = reportDataSource.createFollowReport(reportId);
                Log.d(TAG, String.format("create follow report with reportId = %d", reportId));
            }

            Form form = reportTypeDataSource.getForm(reportType);
            trigger = form.getTrigger();
            if (trigger != null) {
                Log.d(TAG, String.format("This report type contain a trigger with pattern:%s, pageId:%d, notificationText:%s", trigger.getPattern(), trigger.getPageId(), trigger.getNotificationText()));
            }
            if (intent.getAction() != null && intent.getAction().equals(FollowAlertService.ORG_CM_PODD_REPORT_FOLLOW)) {
                form.setStartWithTrigger(true);
            }

            if (startPageId != -1) {
                form.setStartPageId(startPageId);
            }

            formIterator = new FormIterator(form);

            if (reportId == -99) {
                reportId = reportDataSource.createDraftReport(reportType, testReport);
            } else {
                loadFormData();
            }

            nextScreen();
        }

        // open location service only when
        // 1. Create a New report
        // 2. Edit a draft report which don't have any location attach.
        if ((reportSubmit == 0) && (currentLatitude == 0.00)) {
            buildGoogleApiClient();
            if (formIterator.getForm().isForceLocation()) {
                switchToProgressLocationMode();
            }
        }


        /* check softkeyboard visibility */
        final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // different devices' screens have normal height diff differently
                // eg, roughly 5.5" xxhdpi has 220px, 4.5" xhdpi has 110px, 4", 3.5" hdpi has 75px
                int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                int limitHeightPx = (int) (getResources().getDisplayMetrics().density * 100);
                Log.d(TAG, String.format("diff height=%d, limit height=%d", heightDiff, limitHeightPx));
            }
        });

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("Report-" + reportType);
        tracker.send(new HitBuilders.AppViewBuilder().build());

        startTime = System.currentTimeMillis();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("currentFragment", currentFragment);
        outState.putLong("reportId", reportId);
        outState.putLong("reportType", reportType);
        outState.putBoolean("follow", follow);
        outState.putSerializable("formIterator", formIterator);
        outState.putDouble("currentLatitude", currentLatitude);
        outState.putDouble("currentLongitude", currentLongitude);
        outState.putBoolean("testReport", testReport);
        outState.putInt("reportSubmit", reportSubmit);
        outState.putString("followActionName", followActionName);
        super.onSaveInstanceState(outState);
    }

    private void loadFormData() {

        Form form = formIterator.getForm();

        Report report = reportDataSource.getById(reportId);
        reportDate = report.getStartDate();
        reportRegionId = report.getRegionId();
        remark = report.getRemark();
        reportSubmit = report.getSubmit();
        currentLatitude = report.getLatitude();
        currentLongitude = report.getLongitude();

        String formDataStr = report.getFormData();
        Log.d(TAG, "form data = " + formDataStr);
        if (formDataStr != null) {
            try {
                JSONObject jsonObject = new JSONObject(formDataStr);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String[] ary = key.split("@@@");
                    int qid = Integer.parseInt(ary[0]);
                    String name = ary[1];

                    Question question = form.getQuestion(qid);
                    if (question != null) {
                        String value = jsonObject.getString(key);
                        if (value != null && !value.equals("null")) {
                            question.setData(name, question.getDataType().fromJson(value));
                        }
                    } else {
                        Log.d(TAG, "Question not found. key= " + key);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, "error parsing form_data", e);
            }
        }

    }

    @Override
    public void onBackPressed() {
        Fragment oldFragment = getVisibleFragment();
        if (oldFragment != null && oldFragment instanceof ReportNavigationChangeCallbackInterface) {
            ((ReportNavigationChangeCallbackInterface) oldFragment).onPrevious();
        }


        super.onBackPressed();
        Log.d(TAG, "from fragment = " + currentFragment);

        if (currentFragment != null) {
            if (currentFragment.equals(ReportLocationFragment.class.getName())) {
                currentFragment = "dynamicForm";
                showHideDisableMask(false); // delegate readonly function to dynamic form
            } else if (currentFragment.equals(ReportImageFragment.class.getName())) {
                currentFragment = null;
                showHideDisableMask(false);
            } else if (currentFragment.equals(ReportConfirmFragment.class.getName())) {
                currentFragment = ReportLocationFragment.class.getName();
                setNextVisible(true);
                setPrevVisible(true);
                showHideDisableMask(reportSubmit == 1);
            } else if (currentFragment.equals("dynamicForm")) {
                if (! formIterator.previousPage()) {
                    currentFragment = ReportImageFragment.class.getName();
                    showHideDisableMask(false);
                }
            }
        }
        Log.d(TAG, "back to fragment = " + currentFragment);
    }

    private boolean notifyValidationErrors() {
        List<ValidationResult> validateResults = formIterator.validatePageAndGetResult();
        if (validateResults.size() > 0) {
            StringBuffer buff = new StringBuffer();
            for (ValidationResult vr : validateResults) {
                buff.append(vr.getMessage()).append("\n");
            }
            final Crouton crouton = Crouton.makeText(this, buff.toString(), Style.ALERT);
            crouton.setConfiguration(new Configuration.Builder().setDuration(2500).build());
            crouton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Crouton.hide(crouton);
                }
            });
            crouton.show();
            return true;
        }
        return false;
    }

    private void nextScreen() {

        Fragment fragment = null;
        boolean isDynamicForm = false;

        hideKeyboard();

        Fragment oldFragment = getVisibleFragment();
        if (oldFragment != null && oldFragment instanceof ReportNavigationChangeCallbackInterface) {
            ((ReportNavigationChangeCallbackInterface) oldFragment).onNext();
        }

        if (currentFragment == null) { /* first screen */
            Log.d(TAG, "first screen");
            fragment = ReportImageFragment.newInstance(reportId);
            showHideDisableMask(false);
        } else {

            if (currentFragment.equals(ReportLocationFragment.class.getName())) {
                fragment = ReportConfirmFragment.newInstance(reportId);
                showHideDisableMask(false);

            }else if (currentFragment.equals(ReportConfirmFragment.class.getName())) {
                /* do nothing */

            } else {
                isDynamicForm = true;

                setNextVisible(true);
                setPrevVisible(true);
                setNextEnable(true);
                setPrevEnable(true);

                // case I
                // just come into this dynamic form
                // serving fragment(currentPage)
                // case II
                // we are not in first page
                // and not in last page
                // so we proceed to nextPage
                // case III
                // we are at last page
                // so we skip to ReportConfirmFragment
                //if (currentFragment.equals(ReportLocationFragment.class.getName())) {
                if (currentFragment.equals(ReportImageFragment.class.getName())) {
                    // no-op
                    fragment = getPageFragment(formIterator.getCurrentPage());
                    showHideDisableMask(false); // delegate readonly function to dynamic form

                } else if (formIterator.isAtLastPage()) {

                    boolean validatePass = formIterator.validatePage();
                    if (validatePass) {
                        fragment = ReportLocationFragment.newInstance(reportId);
                        isDynamicForm = false;
                        showHideDisableMask(reportSubmit == 1);
                    } else {
                        notifyValidationErrors();
                    }

                } else {
                    if (! formIterator.nextPage()) { // can't jump to next page

                        if (! notifyValidationErrors()) {
                            // no error and no page to go
                            fragment = ReportLocationFragment.newInstance(reportId);
                            isDynamicForm = false;
                            showHideDisableMask(isDoneSubmit());
                        }

                    } else {

                        fragment = getPageFragment(formIterator.getCurrentPage());
                        showHideDisableMask(false);

                    }
                }

            }
        }

        if (fragment != null) {

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
            if (currentFragment == null) {
                Log.d(TAG, "add fragment");
                transaction.add(R.id.container, fragment);
            } else {
                Log.d(TAG, "replace fragment");
                transaction.replace(R.id.container, fragment);
                transaction.addToBackStack(fragment.getClass().getName());
            }
            transaction.commit();

            if (isDynamicForm) {
                currentFragment = "dynamicForm";
            } else {
                currentFragment = fragment.getClass().getName();
            }
        }

        Log.d("----", "current fragment = " + currentFragment);
    }

    CountDownTimer ct;
    private void switchToProgressLocationMode() {
        locationView.setVisibility(View.VISIBLE);
        formView.setVisibility(View.INVISIBLE);
        countdownTextView.setText("30");

        ct = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                countdownTextView.setText(Long.toString(millisUntilFinished / 1000));
                if (millisUntilFinished < 15000) {
                    textProgressLocationView.setText(R.string.unavailable_location_signal_text);
                }
            }

            public void onFinish() {
                switchToFormMode();
            }
        }.start();
    }

    private void switchToFormMode() {
        if (ct != null) {
            ct.cancel();
        }

        if (locationView.getVisibility() != View.INVISIBLE) {
            locationView.setVisibility(View.INVISIBLE);
            formView.setVisibility(View.VISIBLE);
        }
    }

    private Fragment getPageFragment(Page page) {
        FormPageFragment fragment = new FormPageFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("page", formIterator.getCurrentPage());
        bundle.putBoolean("isSubmit", isDoneSubmit());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        String title = getString(R.string.title_activity_report);
        if (testReport) {
            title = getString(R.string.test_title) + title;
        }
        StyleUtil.setActionBarTitle(this, title);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (testReport) {
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.report_test_indicator));
        } else {
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.abc_ab_solid_light_holo));
        }

        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Log.d(TAG, "Menu item select id=" + item.getItemId());

        if (id == R.id.action_settings) {
            return true;

        } else if (id == android.R.id.home) {
            // confirm going back to report list home
            if (reportSubmit == 1) {

                finishReport(ReportDataInterface.CANCEL_ACTION);

            } else {
                new AlertDialog.Builder(this).setTitle(R.string.title_confirm_back_report_home)
                        .setMessage(R.string.message_confirm_back_report_home)
                        .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Save as draft and finish this activity
                                finishReport(ReportDataInterface.DRAFT_ACTION);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .create().show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setNextEnable(boolean flag) {
        nextBtn.setEnabled(flag);
    }

    @Override
    public void setPrevEnable(boolean flag) {
        prevBtn.setEnabled(flag);
    }

    @Override
    public void setNextVisible(boolean flag) {
        if (flag) {
            nextBtn.setVisibility(View.VISIBLE);
        } else {
            nextBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPrevVisible(boolean flag) {
//        if (flag) {
//            prevBtn.setVisibility(View.VISIBLE);
//        } else {
//            prevBtn.setVisibility(View.GONE);
//        }
    }


    @Override
    public void finishReport(int action) {
        // Reload report to get current submit status
        Report report = reportDataSource.getById(reportId);
        reportSubmit = report.getSubmit();

        if (action != ReportDataInterface.CANCEL_ACTION) {
            if (reportSubmit == 0) {

                reportDataSource.updateReport(reportId, reportDate, reportRegionId, remark, followActionName);

                if (action == ReportDataInterface.CONFIRM_ACTION) {
                    saveForm(0);
                    reportQueueDataSource.addDataQueue(reportId);
                    reportQueueDataSource.addImageQueue(reportId);


                    broadcastReportSubmission();

                    if (report.getTestReport() == Report.FALSE && !follow && trigger != null) {

                        new FollowAlertScheduleService.SetFollowAlertScheduleTask(this, trigger.getPattern(), trigger.getNotificationText(),
                                report.getId(), report.getType(), false
                        ).execute((Void[]) null);
                    }

                    if (follow && parentReportId != -1) {
                        new FollowAlertScheduleService.CancelFollowAlertScheduleTask(this, parentReportId).execute((Void[]) null);
                    }

                    if (report.getTestReport() == Report.TRUE && !follow && trigger != null) {
                        Log.d(TAG, "schedule test notification");
                        new FollowAlertScheduleService.SetFollowAlertScheduleTask(this,
                                trigger.getPattern(), trigger.getNotificationText(),
                                report.getId(), report.getType(),
                                true
                        ).execute((Void[]) null);
                    }

                } else if (action == ReportDataInterface.DRAFT_ACTION) {
                    // save as draft
                    saveForm(1);
                }
            }
        }

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAlertReceiver);
        reportDataSource.close();
        reportQueueDataSource.close();
        reportTypeDataSource.close();
        followAlertDataSource.close();

        if (! isDoneSubmit()) {
            // send timing hit
            long interval = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Timing interval milli=" + interval);

            Tracker tracker = ((PoddApplication) getApplication()).getTracker(
                    PoddApplication.TrackerName.APP_TRACKER);
            tracker.send(new HitBuilders.TimingBuilder()
                    .setCategory("ReportProcess").setValue(interval)
                    .setVariable("Overall").setLabel(sharedPrefUtil.getUserName())
                    .build());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOR_OPEN_LOCATION_SERVICE_DIALOG) {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        }
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    private void saveForm(int draft) {
        Map<String, Object> data = formIterator.getData(false);
        JSONObject jsonData = new JSONObject(data);

        if (trigger != null && trigger.isMerge()) {
            Report report = reportDataSource.getById(reportId);
            String formDataStr = report.getFormData();
            if (formDataStr != null) {
                try {
                    JSONObject originData = new JSONObject(formDataStr);
                    Iterator<String> keys = jsonData.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        originData.put(key, jsonData.get(key));
                    }

                    jsonData = originData;

                } catch (JSONException e) {
                    Log.e(TAG, "error parsing form_data", e);
                }
            }

        }
        Log.d(TAG, jsonData.toString());
        reportDataSource.updateData(reportId, jsonData.toString(), draft);
    }

    /*
     * Handle action from softkeyboard input (DONE) button
     */
    @Override
    public boolean onSoftKeyAction(TextView view, int actionId, KeyEvent event) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_NEXT:
                Log.d(TAG, "action NEXT");
                View nextTextView = view.focusSearch(View.FOCUS_DOWN);
                if (nextTextView != null) {
                    nextTextView.requestFocus();
                    if (!(nextTextView instanceof EditText)) {
                        hideKeyboard();
                    }
                    return true;
                }
            case EditorInfo.IME_ACTION_DONE:
                Log.d(TAG, "action DONE");
                nextScreen();
                hideKeyboard();
                return true;
        }
        return false;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            currentLatitude = mLastLocation.getLatitude();
            currentLongitude = mLastLocation.getLongitude();

            Log.d(TAG, "current location = " + currentLatitude + "," + currentLongitude);
            reportDataSource.updateLocation(reportId, currentLatitude, currentLongitude);

            switchToFormMode();
        } else {
            Log.d(TAG, "mLastLocation is null");
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.setAlwaysShow(true);
            builder.addLocationRequest(locationRequest);
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates states = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can initialize location
                            // requests here.

                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user
                            // a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        ReportActivity.this,
                                        REQUEST_FOR_OPEN_LOCATION_SERVICE_DIALOG);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            });

        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "can't connect to google play error:" + connectionResult.getErrorCode());

    }

    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        Log.d(TAG, "current location = " + currentLatitude + "," + currentLongitude);
        reportDataSource.updateLocation(reportId, currentLatitude, currentLongitude);

        stopLocationUpdates();
        switchToFormMode();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class FormPageFragment extends Fragment {

        private Page page;

        public FormPageFragment() {
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            Page page = (Page) arguments.get("page");
            boolean isSubmit = arguments.getBoolean("isSubmit");
            PageView pageView = new PageView(getActivity(), page, isSubmit);
            pageView.setQuestionActionListener((QuestionView.SoftKeyActionHandler) getActivity());
            pageView.askForFocus();
            return pageView;
        }
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    private Fragment getVisibleFragment(){
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments != null) {
            for(Fragment fragment : fragments){
                if(fragment != null && fragment.isVisible())
                    return fragment;
            }
        }

        return null;
    }

    @Override
    public Date getDate() {
        return reportDate;
    }

    @Override
    public void setDate(Date date) {
        reportDate = date;
        Log.d(TAG, "set date with " + date);
    }

    @Override
    public long getRegionId() {
        return reportRegionId;
    }

    @Override
    public void setRegionId(long regionId) {
        reportRegionId = regionId;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public boolean isDoneSubmit() {
        return this.reportSubmit == 1;
    }

    private void showHideDisableMask(boolean shown) {
        disableMaskView.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
    }

    private void broadcastReportSubmission() {
        // Broadcasts the Intent to network receiver
        Intent networkIntent = new Intent(DataSubmitService.ACTION_REPORT_SUBMIT);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(networkIntent);
    }

}
