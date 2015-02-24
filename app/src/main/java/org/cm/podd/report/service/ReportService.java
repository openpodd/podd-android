package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;


public class ReportService extends IntentService {

    private static final String TAG = "ReportService";
    private static final String ENDPOINT = "/reports/";

    public static final String ACTION_FETCH = "podd.report.fetch";
    public static final String ACTION_FETCH_DONE = "podd.report.fetch.done";

    public static final String PARAM_REPORT_ID = "id";

    private SharedPrefUtil sharedPrefUtil;

    public ReportService() {
        super("ReportService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH.equals(action)) {
                handleActionFetch(intent.getLongExtra(PARAM_REPORT_ID, 0));
            }
        }
    }

    public void handleActionFetch(Long id) {
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        try {
            RequestDataUtil.ResponseObject resp;
            resp = fetch(id);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                // notify report data.
                Intent intent = new Intent(ACTION_FETCH_DONE);
                intent.putExtra("report", resp.getRawData());
                sendBroadcast(intent);
            } else {
                Log.d(TAG, "Fail with errorCode:" + resp.getStatusCode());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
        }
    }

    private RequestDataUtil.ResponseObject fetch(Long id)
            throws IOException {
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        return RequestDataUtil.get(ENDPOINT + Long.toString(id),
                "", sharedPrefUtil.getAccessToken());
    }

    /**
     * Do fetch
     */
    public static void doFetch(Context context, Long id) {
        Intent intent = new Intent(context, ReportService.class);
        intent.setAction(ACTION_FETCH);
        intent.putExtra(PARAM_REPORT_ID, id);
        context.startService(intent);
    }

    public static class ReportAsyncTask extends AsyncTask<String, Void, RequestDataUtil.ResponseObject> {
        protected Context context;

        public void setContext(Context context) {
            this.context = context;
        }

        public Context getContext() {
            return context;
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
            String accessToken = sharedPrefUtil.getAccessToken();

            return RequestDataUtil.get(ENDPOINT + params[0], null, accessToken);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject responseObject) {
            super.onPostExecute(responseObject);
        }
    }

    public static class FollowUpAsyncTask extends ReportAsyncTask {

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
            String accessToken = sharedPrefUtil.getAccessToken();

            return RequestDataUtil.get(ENDPOINT + params[0] + "/involved", null, accessToken);
        }
    }
}
