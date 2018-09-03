package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import org.cm.podd.report.db.RecordSpecDataSource;
import org.cm.podd.report.model.RecordSpec;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by pphetra on 21/8/2018 AD.
 */

public class SyncRecordSpecService extends IntentService {

    public static final String TAG = "SyncRecordSpecService";
    public static final String SYNC = "recordSpec.sync";


    public SyncRecordSpecService() {
        super(SyncRecordSpecService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        String accessToken = sharedPrefUtil.getAccessToken();

        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/recordSpecs/", null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            RecordSpecDataSource dbSource = RecordSpecDataSource.Companion.getInstance(this);
            List<RecordSpec> origRecordSpecs = dbSource.getAll();

            ArrayList<Long> removeIds = new ArrayList<Long>();
            for (RecordSpec spec : origRecordSpecs) {
                removeIds.add(spec.id);
            }

            try {
                JSONArray items = new JSONArray(resp.getRawData());
                for (int i = 0; i < items.length(); i++) {
                    JSONObject updSpec = items.getJSONObject(i);
                    Long id = updSpec.optLong("id");
                    removeIds.remove(id);

                    RecordSpec spec = find(updSpec, origRecordSpecs);
                    long timestamp = updSpec.optLong("timestamp");

                    if (spec != null && spec.timestamp < timestamp) {
                        populate(updSpec, spec, accessToken);
                        Log.d(TAG, "update spec " + spec.id);
                        dbSource.update(spec);
                    } else if (spec == null) {
                        spec = new RecordSpec();
                        populate(updSpec, spec, accessToken);
                        Log.d(TAG, "insert spec " + spec.id);
                        dbSource.insert(spec);
                    } else {
                        Log.d(TAG, "ignore spec " + spec.id);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }

            for (Long id : removeIds) {
                Log.d(TAG, "remove spec " + id);
                dbSource.remove(id);
            }


            sendBroadcast(new Intent(SYNC));
        } else {
            Log.e(TAG, "Error while fetching /recordSpecs/");
        }


    }

    private void populate(JSONObject updSpec, RecordSpec spec, String accessToken) throws JSONException {
        Long id = updSpec.optLong("id");
        RequestDataUtil.ResponseObject detailResp = RequestDataUtil.get("/recordSpecs/" + id.toString() + "/", null, accessToken);
        JSONObject result = new JSONObject(detailResp.getRawData());

        spec.id = id;
        spec.name = result.getString("name");
        spec.timestamp = result.getLong("timestamp");
        spec.tplHeader = result.optString("tplHeader", "");
        spec.tplSubHeader = result.optString("tplSubHeader", "");
        spec.parentId = result.optLong("parentId",  0);
        spec.typeId = result.optLong("typeId", 0);
        spec.groupKey = result.optString("groupKey", "all");
    }

    private RecordSpec find(JSONObject updSpec, List<RecordSpec> recordSpecs) {
        RecordSpec found = null;
        Long id = updSpec.optLong("id");

        Iterator<RecordSpec> iterator = recordSpecs.iterator();
        while (iterator.hasNext() && found == null) {
            RecordSpec spec = iterator.next();
            if (spec.id == id) {
                found = spec;
            }

        }
        return found;
    }
}
