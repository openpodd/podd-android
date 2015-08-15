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
import android.util.Log;

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
    public static final String DISPLAY_PASSWORD = "display_password";
    public static final String FULLNAME = "full_name";
    public static final String AWS_SECRET_KEY = "aws_secret_key";
    public static final String AWS_ACCESS_KEY = "aws_access_key";
    public static final String ADMIN_AREA = "admin_area";
    public static final String GCM_REGISTRATION_ID = "gcm_registration_id";
    public static final String GCM_APP_VERSION = "gcm_app_version";
    public static final String PROFILE_IMAGE_FILE_PATH = "profile_image_file_path";
    public static final String IS_VOLUNTEER = "is_volunteer";
    public static final String CAN_SET_FLAG = "can_set_flag";
    private static final String TAG = "SharedPrefUtil";

    private SharedPreferences sharedPrefs;

    public SharedPrefUtil (Context context) {
        sharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Context.MODE_PRIVATE);
    }

    public boolean isUserLoggedIn() {
        return getAccessToken() != null;
    }

    public String getAccessToken() {
        return sharedPrefs.getString(ACCESS_TOKEN_KEY, null);
    }

    public String getUserName() {
        return sharedPrefs.getString(USERNAME, null);
    }
    public String getDisplayPassword() {
        return sharedPrefs.getString(DISPLAY_PASSWORD, null);
    }

    public List<Region> getAllRegions() {
        List<Region> regions = new ArrayList<Region>();
        String jsonStr = sharedPrefs.getString(ADMIN_AREA, null);
        if (jsonStr != null) {
            try {
                JSONArray jsonArr = new JSONArray(jsonStr);
                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject jsonObj = jsonArr.getJSONObject(i);
                    regions.add(new Region(jsonObj.getLong("id"), jsonObj.getString("name")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return regions;
    }

    public String getAwsSecretKey() {
        return sharedPrefs.getString(AWS_SECRET_KEY, "");
    }

    public String getAwsAccessKey() {
        return sharedPrefs.getString(AWS_ACCESS_KEY, "");
    }

    public String getFullName() {
        return sharedPrefs.getString(FULLNAME, "");
    }

    public String getGCMRegId() { return sharedPrefs.getString(GCM_REGISTRATION_ID, ""); }

    public int getGCMVersion() { return sharedPrefs.getInt(GCM_APP_VERSION, 0); }

    public void setGCMData(String regId, int appVersion) {
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPrefUtil.GCM_REGISTRATION_ID, regId);
        editor.putInt(SharedPrefUtil.GCM_APP_VERSION, appVersion);
        editor.commit();
    }

    public void setAccessToken(String token) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPrefUtil.ACCESS_TOKEN_KEY, token);
        editor.commit();
    }

    public void setUserName(String name) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPrefUtil.USERNAME, name);
        editor.commit();
    }

    public void setDisplayPassword(String displayPassword) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPrefUtil.DISPLAY_PASSWORD, displayPassword);
        editor.commit();
    }

    public void setUserInfo(String fullName, String administrationAreas) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPrefUtil.FULLNAME, fullName);
        editor.putString(SharedPrefUtil.ADMIN_AREA, administrationAreas);
        editor.commit();
    }

    public void setAWSKey(String awsSecretKey, String awsAccessKey) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPrefUtil.AWS_SECRET_KEY, awsSecretKey);
        editor.putString(SharedPrefUtil.AWS_ACCESS_KEY, awsAccessKey);
        editor.commit();
    }

    public void clearAllData() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.commit();
    }

    public String getProfileImageFilePath() {
        return sharedPrefs.getString(PROFILE_IMAGE_FILE_PATH, null);
    }

    public void setProfileImageFilePath(String filePath) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PROFILE_IMAGE_FILE_PATH, filePath);
        editor.commit();
    }

    public Boolean getIsVolunteer() {
        return sharedPrefs.getBoolean(IS_VOLUNTEER, true);
    }

    public void setIsVolunteer(Boolean isVolunteer) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(IS_VOLUNTEER, isVolunteer);
        editor.commit();
    }

    public Boolean getCanSetFlag() {
        return sharedPrefs.getBoolean(CAN_SET_FLAG, false);
    }

    public void setCanSetFlag(Boolean canSetFlag) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(CAN_SET_FLAG, canSetFlag);
        editor.commit();
    }
}

