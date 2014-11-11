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
package org.cm.podd.report.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.cm.podd.report.model.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SharedPrefUtil {

    private static final String APP_SHARED_PREFS = "podd_preferences";
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String USERNAME = "username";
    public static final String SERVER_ADDRESS = "server_address";
    public static final String FULLNAME = "full_name";
    public static final String AWS_SECRET_KEY = "aws_secret_key";
    public static final String AWS_ACCESS_KEY = "aws_access_key";
    public static final String ADMIN_AREA = "admin_area";

    private static SharedPreferences sharedPrefs;

    public static SharedPreferences getPrefs(Context context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Context.MODE_PRIVATE);
        }
        return sharedPrefs;
    }

    public static boolean isUserLoggedIn() {
        return getAccessToken() != null;
    }

    public static String getAccessToken() {
        return sharedPrefs.getString(ACCESS_TOKEN_KEY, null);
    }

    public static String getUserName() {
        return sharedPrefs.getString(USERNAME, null);
    }

    public static String getServerAddress() {
        return sharedPrefs.getString(SERVER_ADDRESS, "http://private-anon-d510f140d-poddapi.apiary-mock.com");
    }

    public static List<Region> getAllRegions() {
        List<Region> regions = new ArrayList<Region>();
        String jsonStr = sharedPrefs.getString(ADMIN_AREA, null);
        try {
            JSONArray jsonArr = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                regions.add(new Region(jsonObj.getLong("id"), jsonObj.getString("name")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return regions;
    }
}

