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

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.util.SharedPrefUtil;

import java.util.HashMap;

public class AnalyticsApp extends Application {

    // The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-57082327-1";

    public enum TrackerName {
        APP_TRACKER
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public AnalyticsApp() {
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
            t.set("&uid", SharedPrefUtil.getUserName());

            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
}
