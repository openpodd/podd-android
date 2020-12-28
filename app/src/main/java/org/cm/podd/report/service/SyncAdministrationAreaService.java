package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Created by pphetra on 1/13/16 AD.
 */
public class SyncAdministrationAreaService extends IntentService {

    public static final String TAG = "SyncAdminAreaService";

    public SyncAdministrationAreaService() {
        super(SyncAdministrationAreaService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());

        String accessToken = sharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/users/administrationArea/", null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            try {
                JSONObject ret = new JSONObject(resp.getRawData());
                if (ret.getBoolean("success")) {

                    JSONObject adminArea = ret.getJSONObject("administartionArea");
                    long administrationAreaId = adminArea.getLong("id");
                    sharedPrefUtil.setDefaultAdministrationAreaId(administrationAreaId);

                    Intent setIdIntent = new Intent(TAG);
                    setIdIntent.putExtra("administrationAreaId", administrationAreaId);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(setIdIntent);

                } else {
                    Log.e(TAG, "no default administration area for this user");
                }

            } catch (JSONException je) {
                Log.e(TAG, "parse json error", je);
            }
        } else {
            Log.e(TAG, "Server error");
        }

    }
}
