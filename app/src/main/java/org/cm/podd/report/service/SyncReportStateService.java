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

import org.cm.podd.report.db.ReportStateDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.ReportState;
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

public class SyncReportStateService extends IntentService {

    public static final String SYNC = "reportState.sync";
    private static final String TAG = "SyncReportStateService";

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public SyncReportStateService() {
        super(SyncReportStateService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        String accessToken = sharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/reportStates/", null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            ReportStateDataSource dbSource = new ReportStateDataSource(this);
            List<ReportState> origReportStates = dbSource.getAll();

            ArrayList<Long> removeIds = new ArrayList<Long>();
            for (ReportState rs : origReportStates) {
                removeIds.add(rs.getId());
            }

            try {
                JSONArray items = new JSONArray(resp.getRawData());

                for (int i = 0; i < items.length(); i++) {
                    JSONObject updateReportState = items.getJSONObject(i);
                    removeIds.remove(new Long(updateReportState.optInt("id")));

                    ReportState rs = requireReportStateUpdate(updateReportState, origReportStates);

                    if (rs != null) {

//                        RequestDataUtil.ResponseObject resp2 =
//                                RequestDataUtil.get("/reportStates/" + rs.getId(), null, accessToken);
//
//                        JSONObject result = new JSONObject(resp2.getRawData());
                        rs.setReportType(updateReportState.optInt("reportType"));
                        rs.setName(updateReportState.optString("name"));
                        rs.setCode(updateReportState.optString("code"));
                        rs.setDescription(updateReportState.optString("description"));
                        rs.setCanEdit(updateReportState.optInt("canEdit"));

                        dbSource.update(rs);

                    } else {
                        long id = updateReportState.optLong("id");
                        int reportType = updateReportState.optInt("reportType");
                        String name = updateReportState.optString("name");
                        String code = updateReportState.optString("code");
                        String description = updateReportState.optString("description");
                        int canEdit = updateReportState.optInt("canEdit");

//                        RequestDataUtil.ResponseObject resp2 =
//                                RequestDataUtil.get("/reportStates/" + id, null, accessToken);
//
//                        JSONObject result = new JSONObject(resp2.getRawData());

                        rs = new ReportState(id, reportType, name, code, description, canEdit);
                        dbSource.insert(rs);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }

            for (Long id : removeIds) {
                dbSource.removeReportState(id);
            }

            dbSource.close();
        } else {
            Log.e(TAG, "Server error");
        }

        // notify activity
        sendBroadcast(new Intent(SYNC));
    }

    private ReportState requireReportStateUpdate(JSONObject updateReportState, List<ReportState> reportStates) {
        ReportState found = null;
        Iterator<ReportState> iterator = reportStates.iterator();

        while (iterator.hasNext() && found == null) {
            ReportState reportState = iterator.next();
            int reportType = updateReportState.optInt("reportType");
            String name = updateReportState.optString("name");
            String code = updateReportState.optString("code");
            String description = updateReportState.optString("description");
            int canEdit = updateReportState.optBoolean("canEdit") ? 1 : 0;
            long updateReportStateId = updateReportState.optInt("id");

            if (updateReportStateId == reportState.getId()) {
                found = reportState;
                found.setReportType(reportState.getReportType());
                found.setName(reportState.getName());
                found.setCode(reportState.getCode());
                found.setDescription(reportState.getDescription());
                found.setCanEdit(reportState.getCanEdit());

                if (reportType != reportState.getReportType() || !name.equals(reportState.getName())
                        || !code.equals(reportState.getCode())
                        || !description.equals(reportState.getDescription())
                        || canEdit != reportState.getCanEdit()) {
                    Log.d(TAG, "id:" + updateReportStateId + ": update");

                    found.setReportType(reportType);
                    found.setName(name);
                    found.setCode(code);
                    found.setDescription(description);
                    found.setCanEdit(canEdit);
                }
            }
        }
        return found;
    }
}



