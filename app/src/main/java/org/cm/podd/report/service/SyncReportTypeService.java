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

import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;

public class SyncReportTypeService extends IntentService {

    public static final String SYNC = "reportType.sync";
    private static final String TAG = "SyncReportTypeService";

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public SyncReportTypeService() {
        super(SyncReportTypeService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String accessToken = SharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/reportTypes/", null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            ReportTypeDataSource dbSource = new ReportTypeDataSource(this);
            List<ReportType> origReportTypes = dbSource.getAll();

            try {
                JSONArray items = new JSONArray(resp.getRawData());
//                JSONArray items = new JSONArray("[{\"id\":1,\"version\":2,\"name\":\"สัตว์ป่วย\\/ไม่ตายสักที\"},{\"id\":2,\"version\":3,\"name\":\"สัตว์กัดกระจาย\"}]");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject updateReportType = items.getJSONObject(i);

                    // check to see if any of server report types has greater version
                    ReportType rt = requireVersionUpdate(updateReportType, origReportTypes);

                    if (rt != null) {
                        // Get more detail definition for this report type
                        RequestDataUtil.ResponseObject resp2 =
                                RequestDataUtil.get("/reportTypes/" + rt.getId(), null, accessToken);

                        JSONObject result = new JSONObject(resp2.getRawData());
                        rt.setDefinition(result.optJSONObject("definition").toString());
                        rt.setVersion(result.optInt("version"));
                        rt.setName(result.optString("name"));

                        Log.d(TAG, String.format("Report type id %d updated to version %d",
                                rt.getId(), rt.getVersion()));
                        Log.d(TAG, "new definition = " + rt.getDefinition());

                        dbSource.update(rt);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }

        } else {
            Log.e(TAG, "Server error");
        }

        // notify activity
        sendBroadcast(new Intent(SYNC));
    }

    private ReportType requireVersionUpdate(JSONObject updateReportType, List<ReportType> reportTypes) {
        ReportType found = null;
        boolean required = false;
        Iterator<ReportType> iterator = reportTypes.iterator();

        while (iterator.hasNext() && !required) {
            ReportType rt = iterator.next();
            int updateVersion = updateReportType.optInt("version");
            long updateReportTypeId = updateReportType.optInt("id");

            if (updateReportTypeId == rt.getId() && updateVersion > rt.getVersion()) {
                Log.d(TAG, String.format("id %d : new version %d > old version %d",
                        updateReportTypeId, updateVersion, rt.getVersion()));
                required = true;
                found = rt;
            }
        }
        return found;
    }


}
