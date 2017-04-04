package org.cm.podd.report.service;

/**
 * Created by sudarat on 4/3/2017 AD.
 */

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.db.ConfigurationDataSource;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.Config;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConfigService extends IntentService {

    public static final String SYNC = "ConfigService.sync";

    private static final String TAG = "ConfigService";

    public ConfigService() {
        super(CommentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String url = intent.getStringExtra("url");

        String system = intent.getStringExtra("system");
        String key = intent.getStringExtra("key");

        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        String accessToken = sharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                url, null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            ConfigurationDataSource dbSource = new ConfigurationDataSource(this);
            Config origConfig = dbSource.getConfigValue(system, key);

            JSONObject response = null;
            try {
                response = new JSONObject(resp.getRawData());
                if (origConfig != null) {
                    origConfig.setValue(response.getString("results"));
                    dbSource.update(origConfig);
                } else {
                    Config config = new Config(system, key, response.getString("results"));
                    dbSource.insert(config);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            dbSource.close();
        } else {
            Log.e(TAG, "Server error");
        }
        sendBroadcast(new Intent(SYNC));
    }

}
