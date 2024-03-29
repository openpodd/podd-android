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

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.squareup.picasso.Picasso;

import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.db.FollowAlertDataSource;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.db.RecordSpecDataSource;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.fragment.DashboardFeedFragment;
import org.cm.podd.report.fragment.NotificationInterface;
import org.cm.podd.report.fragment.NotificationListFragment;
import org.cm.podd.report.fragment.ReportListFragment;
import org.cm.podd.report.model.RecordSpec;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.ConnectivityChangeReceiver;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.service.FollowAlertScheduleService;
import org.cm.podd.report.service.FollowAlertService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.service.SyncAdministrationAreaService;
import org.cm.podd.report.service.SyncRecordSpecService;
import org.cm.podd.report.service.SyncAreaService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static android.content.Intent.ACTION_DEFAULT;


public class HomeActivity extends AppCompatActivity implements NotificationInterface {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static int MY_LOCATION_REQUEST_CODE = 9001;
    public static final String RECEIVE_MESSAGE_ACTION = "podd.receive_message_action";
    public static final String TAG = "HomeActivity";
    private static final String APP_TITLE = "ผ่อดีดี";

    Fragment mCurrentFragment;
    int mNotificationCount;

    private String[] mMenuTitles;
    private DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private int drawerPosition;

    NotificationDataSource notificationDataSource;
    AdministrationAreaDataSource administrationDataSource;
    FeedItemDataSource feedItemDataSource;
    RecordSpecDataSource recordSpecDataSource;

    private boolean sendScreenViewAnalytic = true;
    private SharedPrefUtil sharedPrefUtil;

    BroadcastReceiver networkStateBroadcastReceiver;

    private BroadcastReceiver recordSpecReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "update record spec menu");
            updateRecordMenu();
        }
    };

    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Receiving action " + intent.getAction());
            setNotificationCount();
            refreshDrawerMenu();
            supportInvalidateOptionsMenu();
            refreshNotificationListAdapter();
        }
    };
    private MenuItem settingMenuItem;
    private MenuItem badgeMenuItem;
    private MenuItem newEventMenuItem;
    private Button badgeCounterButton;
    private View notifCountView;
    private NavigationView navigationView;

    private TabLayout.Tab tabNewReport;
    private TabLayout.Tab tabFeed;
    private TabLayout.Tab tabNews;

    private int[] activeIcons;
    private int[] defaultIcons;
    private ConnectivityChangeReceiver localChangeConnectivityReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        updateUserStatus();

        final Context context = this;
        mMenuTitles = new String[] {
            getString(R.string.home_menu_reports),
            getString(R.string.home_menu_news),
            getString(R.string.home_menu_incidents)
        };

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // notification receiver from gcm intent service
        registerReceiver(mNotificationReceiver, new IntentFilter(RECEIVE_MESSAGE_ACTION));

        // initialize and create or upgrade db
        notificationDataSource = new NotificationDataSource(this);
        administrationDataSource = new AdministrationAreaDataSource(this);
        feedItemDataSource = new FeedItemDataSource(this);
        recordSpecDataSource = RecordSpecDataSource.Companion.getInstance(this);

        notifCountView = getLayoutInflater().inflate(R.layout.notif_count, null);

        // initialize prefs
        sharedPrefUtil = new SharedPrefUtil((getApplicationContext()));

        final TabLayout tabLayout = findViewById(R.id.tab_layout);

        activeIcons = new int []{R.drawable.ic_new_report_active, R.drawable.ic_news_active, R.drawable.ic_feed_active};
        defaultIcons = new int []{R.drawable.ic_new_report, R.drawable.ic_news, R.drawable.ic_feed};

        tabNewReport = tabLayout.newTab().setIcon(activeIcons[0]).setText(R.string.home_menu_reports);
        tabNews = tabLayout.newTab().setIcon(defaultIcons[1]).setText(R.string.home_menu_news);
        tabFeed = tabLayout.newTab().setIcon(defaultIcons[2]).setText(R.string.home_menu_incidents);

        tabLayout.addTab(tabNewReport);
        tabLayout.addTab(tabFeed);
        tabLayout.addTab(tabNews);

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                switch (tab.getPosition()) {
                    case 0:
                        if (mCurrentFragment instanceof  DashboardFeedFragment) {
                            ((DashboardFeedFragment) mCurrentFragment).onPauseRefresh();
                        }

                        mCurrentFragment = new ReportListFragment();
                        setTitle(getAppTitle());
                        drawerPosition = 0;
                        tabNewReport.setIcon(activeIcons[0]);
                        tabNews.setIcon(defaultIcons[1]);
                        tabFeed.setIcon(defaultIcons[2]);

                        break;
                    case 1:
                        mCurrentFragment = new DashboardFeedFragment();
                        ((DashboardFeedFragment) mCurrentFragment).onRefresh();

                        drawerPosition = 1;
                        tabNewReport.setIcon(defaultIcons[0]);
                        tabNews.setIcon(defaultIcons[1]);
                        tabFeed.setIcon(activeIcons[2]);
                        break;
                    case 2:
                        if (mCurrentFragment instanceof  DashboardFeedFragment) {
                            ((DashboardFeedFragment) mCurrentFragment).onPauseRefresh();
                        }

                        mCurrentFragment = new NotificationListFragment();
                        drawerPosition = 2;
                        tabNewReport.setIcon(defaultIcons[0]);
                        tabNews.setIcon(activeIcons[1]);
                        tabFeed.setIcon(defaultIcons[2]);
                        break;
                    case 3:
                        drawerPosition = 3;
                        tabNewReport.setIcon(defaultIcons[0]);
                        tabNews.setIcon(defaultIcons[1]);
                        tabFeed.setIcon(defaultIcons[2]);
                        break;

                }
                invalidateOptionsMenu();
                changeFragment();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);

                switch (item.getItemId()) {
                    case R.id.user_profile:
                        showSetting();
                        break;
                    case R.id.user_password:
                        showChangePassword();
                        break;
                    case R.id.app_version_code:
                        return true;
                    case R.id.app_version_name:
                        return true;
                    case R.id.logout:
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.confirm_logout_title)
                                .setMessage(getString(R.string.confirm_logout_text))
                                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        logout();
                                    }
                                })
                                .setNegativeButton(R.string.btn_not_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                })
                                .create()
                                .show();
                        break;
                    default:
                        int recordId = item.getItemId();
                        RecordSpec spec = recordSpecDataSource.get(recordId);
                        if (spec != null) {
                            showRecordActivity(recordId);
                        }

                }

                mDrawerLayout.closeDrawers();

                changeFragment();
                return true;
            }
        });
        mDrawerLayout = findViewById(R.id.drawer_layout);

        updateProfile();

        updateRecordMenu();
        // Set the adapter for the list view
        setNotificationCount();
        refreshDrawerMenu();

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, myToolbar, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        TextView appVersionCode = (TextView) navigationView.getMenu().findItem(R.id.app_version_code).getActionView();
        appVersionCode.setText(String.valueOf(BuildConfig.VERSION_CODE));

        TextView appVersionName = (TextView) navigationView.getMenu().findItem(R.id.app_version_name).getActionView();
        appVersionName.setText(String.valueOf(BuildConfig.VERSION_NAME));

        mDrawerToggle.setDrawerIndicatorEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        getSupportActionBar().setHomeButtonEnabled(true);

        View drawerHeader = findViewById(R.id.drawer_header);
        FontUtil.overrideFonts(this, drawerHeader);

        localChangeConnectivityReceiver = new ConnectivityChangeReceiver();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                localChangeConnectivityReceiver,
                new IntentFilter(DataSubmitService.ACTION_REPORT_SUBMIT));


        /* return to last position after recreate activity */
        if (savedInstanceState != null) {
            drawerPosition = savedInstanceState.getInt("drawerPosition");
        } else {
            drawerPosition = 0;
        }

        drawerPosition = 0;
        selectItem(drawerPosition);

        onNewIntent(getIntent());

        registerReceiver(recordSpecReceiver, new IntentFilter(SyncRecordSpecService.SYNC));
        startSyncRecordSpec();

        startSyncArea();

        Intent getAreaIntent = new Intent(this, SyncAdministrationAreaService.class);
        startService(getAreaIntent);

        registerConnectivityChanged();

    }

    private void registerConnectivityChanged() {
        networkStateBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent submitIntent = new Intent(context, DataSubmitService.class);
                DataSubmitService.enqueueWork(context, submitIntent);
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateBroadcastReceiver, intentFilter);
    }


    private void updateRecordMenu() {
        Menu menu = navigationView.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.getSubMenu().clear();

        int cnt = 0;
        for (RecordSpec record : recordSpecDataSource.findRootRecords()) {
            menuItem.getSubMenu().add(0, (int) record.id, cnt, record.name);
            cnt++;
        }
        if (cnt == 0) {
            menuItem.setVisible(false);
        } else {
            menuItem.setVisible(true);
        }

        navigationView.invalidate();
        mDrawerLayout.invalidate();

    }

    private void startSyncRecordSpec() {
        Intent intent = new Intent(this, SyncRecordSpecService.class);
        startService(intent);
    }

    private void startSyncArea() {
        Intent intent = new Intent(this, SyncAreaService.class);
        startService(intent);
    }

    private void updateProfile() {
        View header = navigationView.getHeaderView(0);
        ImageView profileImageView = header.findViewById(R.id.profile_image);
        String profileImageFilePath = sharedPrefUtil.getProfileImageFilePath();
        Bitmap profileBitmap;
        if (profileImageFilePath == null) {
            // Use default profile image if not setup
            profileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
            profileImageView.setImageBitmap(profileBitmap);
        } else {
            if (profileImageFilePath.matches("^https?://.*")) {
                Picasso.with(this)
                        .load(profileImageFilePath)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.avatar)
                        .into(profileImageView);
            } else {
                profileBitmap = BitmapFactory.decodeFile(Uri.parse(profileImageFilePath).getPath());
                // use default image, if user deleted an image somehow
                if (profileBitmap == null) {
                    profileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                }
                profileImageView.setImageBitmap(profileBitmap);
            }
        }

        ((TextView) header.findViewById(R.id.full_name)).setText(sharedPrefUtil.getFullName());
        ((TextView) header.findViewById(R.id.username)).setText(sharedPrefUtil.getUserName());
    }

    private void changeFragment() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                .commit();
    }

    public void setNotificationCount() {
        mNotificationCount = notificationDataSource.getUnseenCount();

        if (mNotificationCount > 0) {
            activeIcons[1] = R.drawable.ic_news_noti_active;
            defaultIcons[1] = R.drawable.ic_news_noti;
        } else {
            activeIcons[1] = R.drawable.ic_news_active;
            defaultIcons[1] = R.drawable.ic_news;
        }

        if (tabNews.isSelected()) {
            tabNews.setIcon(activeIcons[1]);
        } else {
            tabNews.setIcon(defaultIcons[1]);
        }
    }

    public void refreshDrawerMenu() {
    }

    public void refreshNotificationListAdapter() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(NotificationListFragment.class.getSimpleName());
        if (fragment != null && fragment == mCurrentFragment) {
            NotificationListFragment notificationFragment = (NotificationListFragment) fragment;
            notificationFragment.refreshAdapter();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("drawerPosition", drawerPosition);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private String getAppTitle() {
        String title = sharedPrefUtil.getCustomTitle();
        if (title == null) {
            title = getString(R.string.app_name);
        }
        return title;
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        drawerPosition = position;
        if (position == 0) {
            mCurrentFragment = new ReportListFragment();
            setTitle(getAppTitle());
            tabNewReport.select();
        } else if (position == 1) {
            mCurrentFragment = new DashboardFeedFragment();
            setTitle(mMenuTitles[position]);
            tabFeed.select();
        } else if (position == 2) {
            mCurrentFragment = new NotificationListFragment();
            setTitle(mMenuTitles[position]);
            tabNews.select();
        }else {
            mCurrentFragment = PlaceholderFragment.newInstance(position + 1);
            setTitle(null);
        }

        changeFragment();

        mDrawerLayout.closeDrawer(navigationView);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, mTitle.toString());

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);

        settingMenuItem = menu.findItem(R.id.action_settings);
        newEventMenuItem = menu.findItem(R.id.action_new_event);
        badgeMenuItem = menu.findItem(R.id.badge);

        MenuItemCompat.setActionView(badgeMenuItem, notifCountView);

        badgeCounterButton = (Button) MenuItemCompat.getActionView(badgeMenuItem);
        badgeCounterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectItem(1);
            }
        });


        return true;
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(navigationView);

        settingMenuItem.setVisible(!drawerOpen);
        newEventMenuItem.setVisible(!drawerOpen);
        if (drawerPosition > 0) {
            newEventMenuItem.setVisible(false);
        }

        badgeMenuItem.setVisible(false);
        badgeCounterButton.setText(String.valueOf(mNotificationCount));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSetting();
            return true;
        } else if (id == R.id.action_new_event) {
            newReport();
            return true;
        } else if (id == R.id.action_feed_report_filter) {
            showFeedReportType();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSetting() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivityForResult(intent, 0);
    }

    private void showFeedReportType() {
        Intent intent = new Intent(this, FeedFilterReportTypeActivity.class);
        startActivityForResult(intent, 1);
    }

    private void showChangePassword() {
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        startActivityForResult(intent, 0);
    }

    private void showRecordActivity(long recordId) {
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra("recordSpecId", recordId);
        startActivityForResult(intent, 0);
    }

    private void newReport() {
        Tracker tracker = ((PoddApplication) getApplication()).getTracker(
                PoddApplication.TrackerName.APP_TRACKER);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("newReport").setAction("FromTabBarButton")
                .build());

        Intent intent = new Intent(this, GroupReportTypeActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        sendScreenViewAnalytic = false;
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (!sharedPrefUtil.isUserLoggedIn()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sharedPrefUtil.isUserLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            if (sendScreenViewAnalytic) {
                // send screen view analytic
                Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
                tracker.setScreenName("ReportList");
                tracker.send(new HitBuilders.AppViewBuilder().build());
            }
            sendScreenViewAnalytic = true;

            // Check device for Play Services APK. If check succeeds, proceed with FCM registration.
            if (checkPlayServices()) {

                // Get current fcm token stored in device
                final String regId = sharedPrefUtil.getFCMRegId();
                Log.d(TAG, "stored fcm token: " + regId);

                // Get valid fcm token from firebase
                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {

                        if (task.isSuccessful()) {
                            String fcmRegId = task.getResult().getToken();
                            Log.d(TAG, "up-to-date fcm token: " + fcmRegId);

                            // Current token must be a valid token, otherwise register token with server
                            if (regId.isEmpty() || !regId.equals(fcmRegId)) {
                                Log.d(TAG, "Current token is not valid");
                                registerInBackground(fcmRegId);
                            }
                        } else {
                            Log.e(TAG, "get firebase instanceid fail!!!", task.getException());
                        }

                    }
                });
            } else {
                Log.i(TAG, "No valid Google Play Services APK found.");

            }

            updateProfile();
            setNotificationCount();
            refreshDrawerMenu();
            supportInvalidateOptionsMenu();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.location_not_grant)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothting
                            }
                        });
                builder.show();
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // handle intent result from notification
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("id")) {
                String title = extras.getString("title");
                String content = extras.getString("content");
                long id = extras.getLong("id");
                displayWebViewContent(id, title, content);
            }
        }

        if (intent != null) {
            // Alert when register or login by code success
            Boolean registerSuccess = intent.getStringExtra("register") != null && intent.getStringExtra("register").equalsIgnoreCase("success");
            Boolean forgetPasswordSuccess = intent.getStringExtra("forgetPassword") != null && intent.getStringExtra("forgetPassword").equalsIgnoreCase("success");

            if (registerSuccess) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(getString(R.string.title_activity_registration));
                alertDialogBuilder.setMessage(getString(R.string.register_success) + "\n\n"
                        + getString(R.string.username) + ": " + sharedPrefUtil.getUserName() + "\n"
                        + getString(R.string.password) + ": " + sharedPrefUtil.getDisplayPassword() + "\n\n"
                        + getString(R.string.register_warning_success) + "\n"
                );
                alertDialogBuilder.setPositiveButton(getString(R.string.start_app), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
                alertDialogBuilder.show();
            } else if (forgetPasswordSuccess) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(getString(R.string.login_title));
                alertDialogBuilder.setMessage(getString(R.string.login_success) + "\n\n"
                        + getString(R.string.username) + ": " + sharedPrefUtil.getUserName() + "\n"
                        + getString(R.string.password) + ": " + sharedPrefUtil.getDisplayPassword() + "\n");
                alertDialogBuilder.setPositiveButton(getString(R.string.start_app), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
                alertDialogBuilder.show();
            } else if (intent.getAction() != null && intent.getAction().equals(FollowAlertService.ORG_CM_PODD_REPORT_FOLLOW)) {
                Intent reportIntent = ReportActivity.followReportIntent(this,
                        intent.getLongExtra("reportId", -99),
                        intent.getLongExtra("reportType", 0));
                reportIntent.putExtra("follow", intent.getBooleanExtra("follow", true));
                intent.setAction(FollowAlertService.ORG_CM_PODD_REPORT_FOLLOW);
                startActivity(reportIntent);
            } else if (intent.getAction() != null && intent.getAction().equals(ACTION_DEFAULT)) {
                Uri uri = intent.getData();
                String lastPathSegment = uri.getLastPathSegment();
                /* direct android schema link */
                if (lastPathSegment.equals("sickdeath")) {
                    ReportTypeDataSource ds = new ReportTypeDataSource(this);
                    List<ReportType> all = ds.getAll();
                    long id = 0;
                    for (ReportType rt : all) {
                        if (rt.getName().contains("สัตว์ป่วย")) {
                            id = rt.getId();
                        }
                    }
                    ds.close();
                    if (id != 0) {
                        Intent newReportIntent = ReportActivity.newReportIntent(this, id, false);
                        startActivity(newReportIntent);
                    }
                } else {
                    // firebase deeplink
                    String host = intent.getData().getHost();
                    if (host.equals("podd.page.link")) {
                        FirebaseDynamicLinks.getInstance()
                            .getDynamicLink(intent)
                                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                                    @Override
                                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                                        // Get deep link from result (may be null if no link is found)
                                        Uri deepLink = null;
                                        if (pendingDynamicLinkData != null) {
                                            deepLink = pendingDynamicLinkData.getLink();
                                        }

                                        String reportType = deepLink.getQueryParameter("reportType");
                                        ReportTypeDataSource ds = new ReportTypeDataSource(HomeActivity.this);
                                        List<ReportType> all = ds.getAll();
                                        long id = 0;
                                        for (ReportType rt : all) {
                                            if (rt.getName().contains(reportType)) {
                                                id = rt.getId();
                                            }
                                        }
                                        ds.close();
                                        if (id != 0) {
                                            Intent newReportIntent = ReportActivity.newReportIntent(HomeActivity.this, id, false);
                                            startActivity(newReportIntent);
                                        }
                                    }
                                })
                                .addOnFailureListener(this, new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "getDynamicLink:onFailure", e);
                                    }
                                });

                    }
                }
            }
        }

    }

    private void displayWebViewContent(long id, String title, String content) {
        Intent intent = new Intent(this, WebContentActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    private void logout() {
        // clear access token
        sharedPrefUtil.clearAllData();

        clearAllPendingAlert();

        // clear all report data
        ReportDataSource db = new ReportDataSource(this);
        db.clearAllData();

        // Back to home, then redirect to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }

    private void clearAllPendingAlert() {
        FollowAlertDataSource followAlertDataSource = new FollowAlertDataSource(getApplicationContext());
        List<Map> requestCodes = followAlertDataSource.getUnDoneRequest();
        for (Map tmp : requestCodes) {
            FollowAlertScheduleService.cancelFollowAlert(getApplicationContext(),
                    (Long) tmp.get("reportId"),
                    (Integer) tmp.get("requestCode"),
                    (Long) tmp.get("reportType"),
                    (String) tmp.get("message")
            );
        }
    }

    @Override
    protected void onDestroy() {
        notificationDataSource.close();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localChangeConnectivityReceiver);
        unregisterReceiver(mNotificationReceiver);
        unregisterReceiver(recordSpecReceiver);
        unregisterReceiver(networkStateBroadcastReceiver);
        super.onDestroy();
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground(final String fcmRegId) {
        new RegisterTask().execute(new String[]{fcmRegId});
    }

    @Override
    public void refreshNotificationCount() {
        setNotificationCount();
        refreshDrawerMenu();
        // refresh actionbar menu
        supportInvalidateOptionsMenu();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public class DrawerAdapter extends ArrayAdapter<String> {

        Context context;
        int resource;
        int unseenNotificationCount;

        public DrawerAdapter(Context context, int resource, String[] titles, int unseenNotificationCount) {
            super(context, resource, titles);
            this.context = context;
            this.resource = resource;
            this.unseenNotificationCount = unseenNotificationCount;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View rootView = convertView;
            DrawerItemHolder holder;

            if (convertView == null) {
                rootView = getLayoutInflater().inflate(R.layout.drawer_list_item, parent, false);

                holder = new DrawerItemHolder();
                holder.titleView = rootView.findViewById(R.id.title);
                holder.counterView = rootView.findViewById(R.id.counter);
                holder.iconView = rootView.findViewById(R.id.icon);

                rootView.setTag(holder);
            } else {
                holder = (DrawerItemHolder) rootView.getTag();
            }

            holder.titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));

            holder.counterView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
            holder.counterView.setText(String.valueOf(unseenNotificationCount));


            if (position == 0) {
                holder.iconView.setImageResource(R.drawable.ic_action_view_as_list);
                holder.counterView.setVisibility(View.INVISIBLE);

            } else if (position == 1) {
                holder.iconView.setImageResource(R.drawable.ic_action_event);

                if (unseenNotificationCount > 0) {
                    holder.counterView.setVisibility(View.VISIBLE);
                } else {
                    holder.counterView.setVisibility(View.INVISIBLE);
                }
            } else if (position == 2) {
                holder.iconView.setImageResource(R.drawable.ic_cast_menu);
                holder.counterView.setVisibility(View.INVISIBLE);
            }
            else if (position == 3) {
                holder.iconView.setImageResource(R.drawable.ic_stat_menu);
                holder.counterView.setVisibility(View.INVISIBLE);
            }else {
                holder.counterView.setVisibility(View.INVISIBLE);
            }
            holder.titleView.setText(getItem(position));

            // Re-draw menu.
            invalidateOptionsMenu();

            return rootView;
        }

        private class DrawerItemHolder {
            TextView titleView;
            ImageView iconView;
            TextView counterView;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_home, container, false);
        }
    }


    /**
     * Post gcm register id
     */
    public class RegisterTask extends AsyncTask<String, Void, RequestDataUtil.ResponseObject> {
        String regId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            regId = params[0];
            // send registration token to server and authenticate with user access token
            return RequestDataUtil.registerDeviceId(regId, sharedPrefUtil.getAccessToken());
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            JSONObject obj = resp.getJsonObject();

            if (obj != null) {
                // Persist the regID - no need to register again.
                sharedPrefUtil.setFCMRegId(regId);
                Log.d(TAG, "Device registered, registration ID=" + regId);
            }
        }
    }

    private void updateUserStatus() {
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        ProfileAsyncTask task = new ProfileAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                if (resp.getStatusCode() == 200) {
                    try {
                        JSONObject result = new JSONObject(resp.getRawData());

                        if (result.getString("status").equals("VOLUNTEER")) {
                            sharedPrefUtil.setIsVolunteer(true);
                        } else {
                            sharedPrefUtil.setIsVolunteer(false);
                        }
                        sharedPrefUtil.setCanSetFlag(result.getBoolean("canSetFlag"));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON data", e);
                    }
                }
            }
        };
        task.setContext(getApplicationContext());
        task.execute();
    }

    public static class ProfileAsyncTask extends ReportService.ReportAsyncTask {
        private static final String ENDPOINT = "/users/profile/";

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
            String accessToken = sharedPrefUtil.getAccessToken();

            return RequestDataUtil.get(ENDPOINT, "", accessToken);
        }
    }

}
