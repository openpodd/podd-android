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
package org.cm.podd.report;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

import androidx.multidex.MultiDexApplication;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.database.FirebaseDatabase;

import org.cm.podd.report.util.SharedPrefUtil;

import java.util.HashMap;
import java.util.Locale;

import static org.cm.podd.report.service.FcmMessagingService.DEFAULT_CHANNEL_ID;

public class PoddApplication extends MultiDexApplication {

    // The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-57082327-1";
    private static Context context;

    public enum TrackerName {
        APP_TRACKER
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public PoddApplication() {
        super();
    }

    synchronized public Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // testing
            if (! BuildConfig.FLAVOR.equals("server_production")) {
                analytics.setDryRun(true);
                analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            }
            // --------------
            Tracker t = analytics.newTracker(PROPERTY_ID);

            // You only need to set User ID on a tracker once. By setting it on the tracker, the ID will be
            // sent with all subsequent hits.
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
            t.set("&uid", sharedPrefUtil.getUserName());

            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }

    public void setLanguage(String lang) {
        if (lang.equals("en")) {
            Configuration config = new Configuration(getResources().getConfiguration());
            config.locale = Locale.US;
            getResources().updateConfiguration(config,getResources().getDisplayMetrics());
        } else {
            Configuration config = new Configuration(getResources().getConfiguration());
            config.locale = new Locale("th", "TH") ;
            getResources().updateConfiguration(config,getResources().getDisplayMetrics());
        }
    }

    public void onCreate(){
        super.onCreate();
        PoddApplication.context = getApplicationContext();

        SharedPrefUtil prefUtil = new SharedPrefUtil(getApplicationContext());

        String lang = prefUtil.getLanguage();
        setLanguage(lang);

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        createMessageChannel();
    }

    public static Context getAppContext() {
        return PoddApplication.context;
    }


    public void createMessageChannel() {

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID,
                    "ข่าวสารจากผ่อดีดี",
                    NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager mNotificationManager = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);

            channel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        }
    }
}
