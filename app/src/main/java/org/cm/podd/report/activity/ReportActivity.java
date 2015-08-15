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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
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
import org.cm.podd.report.service.LocationBackgroundService;
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
        implements ReportNavigationInterface, ReportDataInterface, QuestionView.SoftKeyActionHandler {

    private static final String TAG = "ReportActivity";
    private Button prevBtn;
    private Button nextBtn;
    private View disableMaskView;
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
    private int startPageId;

    private double currentLatitude = 0.00;
    private double currentLongitude = 0.00;
    private String currentLocationProvider;

    private Date reportDate;
    private long reportRegionId;
    private String remark;
    private int reportSubmit;

    private View containerView;

    private SharedPrefUtil sharedPrefUtil;

    protected BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentLatitude = intent.getDoubleExtra("Latitude", 0.00);
            currentLongitude = intent.getDoubleExtra("Longitude", 0.00);
            currentLocationProvider = intent.getStringExtra("Provider");

            Log.d(TAG, "current location = " + currentLatitude + "," + currentLongitude);
            reportDataSource.updateLocation(reportId, currentLatitude, currentLongitude);
            stopLocationService();
        }
    };

    protected FollowAlertReceiver mAlertReceiver = new FollowAlertReceiver();
    private CameraInteractionListener cameraInteractionListener;
    private long startTime;

    private long parentReportId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mAlertReceiver,
                new IntentFilter(FollowAlertService.TAG));

        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());

        setContentView(R.layout.activity_report);
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
            Log.d(TAG, "onCreate from savedInstance, testFlag = " + testReport);

            currentLatitude = savedInstanceState.getDouble("currentLatitude");
            currentLongitude = savedInstanceState.getDouble("currentLongitude");
            if (currentLongitude == 0.00 && currentLatitude == 0.00) {
                startLocationService();
            }

        } else {
            Intent intent = getIntent();
            reportType = intent.getLongExtra("reportType", 0);          // mandatory
            reportId = intent.getLongExtra("reportId", -99);            // optional
            follow = intent.getBooleanExtra("follow", false);           // optional
            testReport = intent.getBooleanExtra("test", false);
            startPageId = intent.getIntExtra("startPageId", -1);
            Log.d(TAG, "onCreate, testFlag = " + testReport);

            if (follow) {
                parentReportId = reportId;
                reportId = reportDataSource.createFollowReport(reportId);
            }

            Form form = reportTypeDataSource.getForm(reportType);
            trigger = form.getTrigger();
            if (trigger != null) {
                Log.d(TAG, String.format("This report type contain a trigger with pattern:%s, pageId:%d, notificationText:%s", trigger.getPattern(), trigger.getPageId(), trigger.getNotificationText()));
            }
            if (intent.getAction() != null && intent.getAction().equals(FollowAlertService.ORG_CM_PODD_REPORT_GCM_NOTIFICATION)) {
                form.setStartWithTrigger(true);
            }

            formIterator = new FormIterator(form);

            if (reportId == -99) {
                reportId = reportDataSource.createDraftReport(reportType, testReport);
                startLocationService();
            } else {
                loadFormData();
            }

            nextScreen();
        }

        /* check softkeyboard visibility */
        final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        final View controlBar = findViewById(R.id.controlBar);
        containerView = findViewById(R.id.container);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // different devices' screens have normal height diff differently
                // eg, roughly 5.5" xxhdpi has 220px, 4.5" xhdpi has 110px, 4", 3.5" hdpi has 75px
                int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                int limitHeightPx = (int) (getResources().getDisplayMetrics().density * 100);
                Log.d(TAG, String.format("diff height=%d, limit height=%d", heightDiff, limitHeightPx));
//
//                if (heightDiff > limitHeightPx) {
//                    // if more than limitHeightPx, its probably a keyboard...
//                    // then hide a control bar (prev/next)
//                    controlBar.setVisibility(View.GONE);
//                } else {
//                    controlBar.setVisibility(View.VISIBLE);
//                }
            }
        });

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("Report-" + reportType);
        tracker.send(new HitBuilders.AppViewBuilder().build());

        startTime = System.currentTimeMillis();
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
        super.onSaveInstanceState(outState);
    }

    private void loadFormData() {

        Form form = formIterator.getForm();

        Report report = reportDataSource.getById(reportId);
        reportDate = report.getStartDate();
        reportRegionId = report.getRegionId();
        remark = report.getRemark();
        reportSubmit = report.getSubmit();

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
                            question.setData(name, question.getDataType().parseFromString(value));
                        }
                    } else {
                        Log.d(TAG, "Question not found. key= " + key);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, "error parsing form_data", e);
            }
        }

        if (report.getLatitude() == 0.00 && report.getLongitude() == 0.00) {
            startLocationService();
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
        List<ValidationResult> validateResults = formIterator.getCurrentPage().validate();
        if (validateResults.size() > 0) {
            StringBuffer buff = new StringBuffer();
            for (ValidationResult vr : validateResults) {
                buff.append(vr.getMessage()).append("\n");
            }
            final Crouton crouton = Crouton.makeText(this, buff.toString(), Style.ALERT);
            crouton.setConfiguration(new Configuration.Builder().setDuration(1000).build());
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

            if (startPageId != -1 && startPageId != formIterator.getCurrentPage().getId()) {
                nextScreen();
            }

        }

        Log.d("----", "current fragment = " + currentFragment);
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

                reportDataSource.updateReport(reportId, reportDate, reportRegionId, remark);

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
    public void setCameraInteractionListener(CameraInteractionListener listener) {
        this.cameraInteractionListener = listener;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
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

    public void startLocationService() {
        Log.i(TAG, "startLocationService");
        startService(new Intent(this, LocationBackgroundService.class));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(LocationBackgroundService.BROADCAST_ACTION));
    }

    public void stopLocationService() {
        Log.i(TAG, "stopLocationService");
        stopService(new Intent(this, LocationBackgroundService.class));
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
