package org.cm.podd.report.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class ReportService extends IntentService {

    private static final String TAG = "ReportService";
    private static final String ENDPOINT = "/reports/";

    public static final String ACTION_FETCH = "podd.report.fetch";
    public static final String ACTION_FETCH_DONE = "podd.report.fetch.done";
    public static final String ACTION_FLAG_SET_DONE = "podd.report.flag.set.done";

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

        protected ReportViewActivity activity;

        public void setActivity(ReportViewActivity activity) { this.activity = activity; }

        public ReportViewActivity getActivity() { return activity; }

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

    public static class FlagAsyncTask extends ReportAsyncTask {
        private static final String ENDPOINT = "/flags/";

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
            String accessToken = sharedPrefUtil.getAccessToken();

            JSONObject data = new JSONObject();
            try {
                data.put("reportId", params[0]);
                data.put("priority", params[1]);
            } catch (JSONException e) {
                // Do nothing.
            }

            return RequestDataUtil.post(ENDPOINT, "", data.toString(), accessToken);
        }
    }

    public static class FollowAsyncTask extends ReportAsyncTask {
        private static final String ENDPOINT = "/reports/";

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
            String accessToken = sharedPrefUtil.getAccessToken();

            String reportId = params[0];
            String parentId = params[1];

            JSONObject data = new JSONObject();
            try {
                data.put("parent", parentId);
            } catch (JSONException e) {
                // Do nothing.
            }

            return RequestDataUtil.post(ENDPOINT + reportId + "/follow/", "", data.toString(), accessToken);
        }
    }

    /**
     * Post comment
     */
    public static class PostCommentTask extends ReportAsyncTask {
        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
            String accessToken = sharedPrefUtil.getAccessToken();

            String reqData = null;
            try {
                JSONObject json = new JSONObject();
                json.put("reportId", params[0]);
                json.put("message", params[1]);
                reqData = json.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return RequestDataUtil.post("/reportComments/", null, reqData, accessToken);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            JSONObject obj = resp.getJsonObject();
            if (resp.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                long commentId = obj.optInt("id");
                long reportId = obj.optInt("reportId");
                String message = obj.optString("message");
                String fileUrl = obj.optString("fileUrl");
                String avatarCreatedBy = "";
                String createdBy = "";
                try {
                    JSONObject jsonCreatedBy = new JSONObject(obj.optString("createdBy"));
                    createdBy = jsonCreatedBy.getString("firstName") + " " + jsonCreatedBy.getString("lastName");
                    avatarCreatedBy = jsonCreatedBy.getString("thumbnailAvatarUrl");
                } catch (Exception ex) {}

                String createdAt = obj.optString("createdAt");
                Log.d(TAG, "Found new comment id= " + commentId);

                Comment comment = new Comment(commentId, reportId, message, avatarCreatedBy, fileUrl, createdBy, createdAt);

                CommentDataSource dbSource = new CommentDataSource(getContext());
                dbSource.insert(comment);
            }
        }
    }
}
