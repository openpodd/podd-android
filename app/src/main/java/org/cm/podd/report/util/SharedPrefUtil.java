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

import org.cm.podd.report.model.Config;
import org.cm.podd.report.model.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    public static final String FCM_REGISTRATION_ID = "fcm_registration_id";
    public static final String PROFILE_IMAGE_FILE_PATH = "profile_image_file_path";
    public static final String IS_VOLUNTEER = "is_volunteer";
    public static final String CAN_SET_FLAG = "can_set_flag";
    public static final String TAG = "SharedPrefUtil";
    public static final String CUSTOM_ICON_PATH = "custom_icon_path";
    public static final String CUSTOM_TITLE = "custom_title";
    public static final String CUSTOM_ICON_SIZE = "custom_icon_size";
    public static final String LANGUAGE = "language";
    public static final String REPORT_TYPE_CATEGORY = "report_type_category";
    private static final String DEFAULT_ADMINISTRATION_AREA_ID = "default_administration_area_id";
    public static final String FILTER_REPORT_TYPE = "filter_report_type";

    private SharedPreferences sharedPrefs;
    private String customTitle;

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

    public List<String> getAllParentRegions() {
        HashSet<String> regions = new HashSet<String>();
        String jsonStr = sharedPrefs.getString(ADMIN_AREA, null);
        if (jsonStr != null) {
            try {
                JSONArray jsonArr = new JSONArray(jsonStr);
                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject jsonObj = jsonArr.getJSONObject(i);
                    regions.add(jsonObj.getString("address").replace(jsonObj.getString("name") + " ", ""));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<String>(regions);
    }

    public List<Region> getFilterByRegions(String address) {
        if (address == null) return null;
        List<Region> regions = new ArrayList<Region>();
        String jsonStr = sharedPrefs.getString(ADMIN_AREA, null);
        if (jsonStr != null) {
            try {
                JSONArray jsonArr = new JSONArray(jsonStr);
                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject jsonObj = jsonArr.getJSONObject(i);
                    if (jsonObj.getString("address").contains(address))
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

    public String getFCMRegId() { return sharedPrefs.getString(FCM_REGISTRATION_ID, ""); }

    public int getGCMVersion() { return sharedPrefs.getInt(GCM_APP_VERSION, 0); }

    public void setGCMData(String regId, int appVersion) {
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPrefUtil.GCM_REGISTRATION_ID, regId);
        editor.putInt(SharedPrefUtil.GCM_APP_VERSION, appVersion);
        editor.commit();
    }

    public void setFCMRegId(String regId) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(FCM_REGISTRATION_ID, regId);
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
        // save customIcon & customTitle to next login
        String customIconPath = getCustomIconPath();
        String customTitle = getCustomTitle();

        editor.clear();

        if (customIconPath != null) {
            editor.putString(CUSTOM_ICON_PATH, customIconPath);
        }
        if (customTitle != null) {
            editor.putString(CUSTOM_TITLE, customTitle);
        }
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

    public String getCustomIconPath() {
        return sharedPrefs.getString(CUSTOM_ICON_PATH, null);
    }

    public void setCustomIconPath(String path) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(CUSTOM_ICON_PATH, path);
        editor.commit();
    }

    public void clearCustomIconPath() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(CUSTOM_ICON_PATH);
        editor.remove(CUSTOM_TITLE);
        editor.remove(CUSTOM_ICON_SIZE);
        editor.commit();
    }

    public void setCustomIconSize(int size) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(CUSTOM_ICON_SIZE, size);
        editor.commit();
    }

    public int getCustomIconSize() {
        return sharedPrefs.getInt(CUSTOM_ICON_SIZE, -1);
    }

    public void setCustomTitle(String customTitle) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(CUSTOM_TITLE, customTitle);
        editor.commit();
    }

    public String getCustomTitle() {
        return sharedPrefs.getString(CUSTOM_TITLE, null);
    }

    public void setLanguage(String lang) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(LANGUAGE, lang);
        editor.commit();
    }

    public String getLanguage() {
        return sharedPrefs.getString(LANGUAGE, "th");
    }

    public void setDefaultAdministrationAreaId(long id) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(DEFAULT_ADMINISTRATION_AREA_ID, id);
        editor.commit();
    }

    public long getDefaultAdministrationAreaId() {
        return sharedPrefs.getLong(DEFAULT_ADMINISTRATION_AREA_ID, -99);
    }

    public boolean isDefaultAdministrationAreaIdExits() {
        return sharedPrefs.contains(DEFAULT_ADMINISTRATION_AREA_ID);
    }

    public void setSyncData(String system, String key, String jsonData) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(system + ":[" +key + "]", jsonData);
        editor.commit();
    }

    public Config getSyncData(String system, String key) {
        String jsonData = sharedPrefs.getString(system + ":[" +key + "]", null);
        Config config = new Config(system, key, jsonData);
        return config;
    }

    public void setCategoryMap(HashMap<Long, String> categoryMap) {
        String dump = ObjectSerializerHelper.objectToString(categoryMap);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(REPORT_TYPE_CATEGORY, dump);
        editor.commit();
    }

    public HashMap<Long, String> getCategoryMap() {
        String dump = sharedPrefs.getString(REPORT_TYPE_CATEGORY, null);
        if (dump == null) {
            return new HashMap<Long, String>();
        } else {
            return (HashMap<Long, String>) ObjectSerializerHelper.stringToObject(dump);
        }
    }

    private String defaultFilterReportType = "สัตว์ป่วย/ตาย,สัตว์กัด,อาหารปลอดภัย,คุ้มครองผู้บริโภค,ปัญหาสิ่งแวดล้อม,ภัยธรรมชาติ,โรคในคน,จุดหรือกิจกรรมเสี่ยงต่อภัยสุขภาพ";
    public String getFilterReportType() {
        return sharedPrefs.getString(FILTER_REPORT_TYPE, defaultFilterReportType);
    }

    public void setFilterReportType(String value) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(FILTER_REPORT_TYPE, value);
        editor.commit();
    }

}

