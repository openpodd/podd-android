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
package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdministrationAreaService extends IntentService {

    public static final String SYNC = "administrationArea.sync";
    private static final String TAG = "AreaService";

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public AdministrationAreaService() {
        super(AdministrationAreaService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        String accessToken = sharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/authorities/?short=true", null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            AdministrationAreaDataSource dbSource = new AdministrationAreaDataSource(this);
            List<AdministrationArea> origAdministrationArea = dbSource.getAll();

            ArrayList<Long> removeIds = new ArrayList<Long>();
            for (AdministrationArea area : origAdministrationArea) {
                removeIds.add(area.getId());
            }

            try {
                JSONArray items = new JSONArray(resp.getRawData());

                for (int i = 0; i < items.length(); i++) {
                    JSONObject updateAdministrationArea = items.getJSONObject(i);
                    removeIds.remove(new Long(updateAdministrationArea.optInt("id")));

                    AdministrationArea area = requireAreaUpdate(updateAdministrationArea, origAdministrationArea);

                    if (area != null) {

                        area.setName(updateAdministrationArea.optString("name"));
                        area.setParentName(updateAdministrationArea.optString("parentName"));
                        area.setIsLeaf(1);

                        dbSource.update(area);
                    }else{
                        long administrationAreaId = updateAdministrationArea.optInt("id");
                        String name = updateAdministrationArea.optString("name");
                        String parentName = updateAdministrationArea.optString("parentName");
                        int isLeaf = 1;

                        Log.d(TAG, "Found new administration area id= " + administrationAreaId);

                        area = new AdministrationArea(administrationAreaId, name, parentName, isLeaf);
                        dbSource.insert(area);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }

            for (Long id : removeIds) {
                dbSource.removeAdministrationArea(id);
            }

            dbSource.close();
        } else {
            Log.e(TAG, "Server error");
        }
        sendBroadcast(new Intent(SYNC));
    }

    private AdministrationArea requireAreaUpdate(JSONObject updateAdministrationArea, List<AdministrationArea> administrationAreas) {
        AdministrationArea found = null;
        Iterator<AdministrationArea> iterator = administrationAreas.iterator();

        while (iterator.hasNext() && found == null) {
            AdministrationArea area = iterator.next();
            String name = updateAdministrationArea.optString("name");
            String parentName = updateAdministrationArea.optString("parentName");
            int isLeaf = updateAdministrationArea.optBoolean("isLeaf")? 1: 0;
            long updateAdministrationAreaId = updateAdministrationArea.optInt("id");

            if (updateAdministrationAreaId == area.getId()) {
                found = area;
                found.setName(area.getName());
                found.setParentName(area.getParentName());
                found.setIsLeaf(area.getIsLeaf());

                if (!name.equals(area.getName()) || !parentName.equals(area.getParentName()) || isLeaf != area.getIsLeaf()) {
                    Log.d(TAG, "id:" + updateAdministrationAreaId + ": update");

                    found.setName(name);
                    found.setParentName(parentName);
                    found.setIsLeaf(isLeaf);
                }
            }
        }
        return found;
    }
}
