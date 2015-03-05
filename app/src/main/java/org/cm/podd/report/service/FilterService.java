package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fetch data from API using specific query.
 */
public class FilterService extends IntentService {
    private static final String TAG = "FilterService";

    private static final String ENDPOINT = "/reports/search/";

    private static final String ACTION_QUERY     = "podd.filter.query";
    public static final String ACTION_QUERY_DONE = "podd.filter.query.done";

    private static final String PARAM_QUERY     = "q";
    private static final String PARAM_TIMEZONE  = "tz";
    private static final String PARAM_PAGE_SIZE = "page_size";

    private static final int DEFAULT_PAGE_SIZE = 800;

    SharedPrefUtil sharedPrefUtil;
    FeedItemDataSource feedItemDataSource;

    public FilterService() {
        super("FilterService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_QUERY.equals(action)) {
                String query    = intent.getStringExtra(PARAM_QUERY);
                String timezone = intent.getStringExtra(PARAM_TIMEZONE);

                // Set tz to match mobile timezone if not specify.
                if (timezone == null || timezone.isEmpty()) {
                    timezone = getTimezoneOffset();
                }

                handleActionQuery(query, timezone);
            }
        }
    }

    private void handleActionQuery(String query, String timezone) {
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        feedItemDataSource = new FeedItemDataSource(getApplicationContext());

        Log.i(TAG, "Beginning network querying");

        try {
            RequestDataUtil.ResponseObject resp;

            Log.i(TAG, "Streaming data from network: " + ENDPOINT);
            resp = filterQuery(query, timezone);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Filtering success: " + resp.getRawData());

                JSONObject result = new JSONObject(resp.getRawData());
                try {
                    JSONArray items = result.getJSONArray("results");
                    for (int i = 0; i != items.length(); ++i) {
                        JSONObject item = items.getJSONObject(i);

                        // Prepare FeedItem object.
                        FeedItem feedItem = new FeedItem();
                        feedItem.setItemId(item.getLong("id"));
                        feedItem.setType("report");

                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                                Locale.getDefault());
                        feedItem.setDate(formatter.parse(item.getString("date")));
                        feedItem.setJsonString(item.toString());

                        feedItemDataSource.save(feedItem);
                    }

                    // notify feed updated.
                    sendBroadcast(new Intent(ACTION_QUERY_DONE));
                } catch (JSONException e) {
                    // DO NOTHING.
                    Log.e(TAG, "No results, skipping");
                } catch (ParseException e) {
                    Log.e(TAG, "Date is not valid, skipping");
                }
            } else {
                Log.d(TAG, "Filtering fail with errorCode:" + resp.getStatusCode());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Filter URL is malformed", e);
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
        }

        Log.i(TAG, "Network querying complete");
    }

    /**
     * Do query
     */
    public static void doQuery(Context context, String query, String timezone) {
        Intent intent = new Intent(context, FilterService.class);
        intent.setAction(ACTION_QUERY);
        intent.putExtra(PARAM_QUERY, query);
        intent.putExtra(PARAM_TIMEZONE, timezone);
        context.startService(intent);
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream.
     */
    private RequestDataUtil.ResponseObject filterQuery(String query, String timezone)
            throws IOException {

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        String queryString = null;

        // Limit only last 2 weeks.
        if (query == null) {
            query = "";
        }
        query += " date:last 14 days ";

        // Prepare query parameters
        if (!query.isEmpty() || !timezone.isEmpty()) {
            if (!query.isEmpty()) {
                params.add(new BasicNameValuePair(PARAM_QUERY, query));
            }

            if (!timezone.isEmpty()) {
                params.add(new BasicNameValuePair(PARAM_TIMEZONE, timezone));
            }
            else {
                params.add(new BasicNameValuePair(PARAM_TIMEZONE, getTimezoneOffset()));
            }

            // Fixed page size.
            params.add(new BasicNameValuePair(PARAM_PAGE_SIZE, Integer.toString(DEFAULT_PAGE_SIZE)));

            queryString = URLEncodedUtils.format(params, "utf-8");
        }

        return RequestDataUtil.get(ENDPOINT, queryString, sharedPrefUtil.getAccessToken());
    }

    public static class FilterAsyncTask extends AsyncTask<String, Void, RequestDataUtil.ResponseObject> {
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

            List<NameValuePair> queryParams = new LinkedList<NameValuePair>();
            String queryString = null;

            String query = params[0];
            String timezone = params[1];
            if (timezone == null) {
                timezone = "";
            }

            // Limit only last 2 weeks.
            if (query == null) {
                query = "";
            }

            // Prepare query parameters
            if (!query.isEmpty() || !timezone.isEmpty()) {
                if (!query.isEmpty()) {
                    queryParams.add(new BasicNameValuePair(PARAM_QUERY, query));
                }

                if (!timezone.isEmpty()) {
                    queryParams.add(new BasicNameValuePair(PARAM_TIMEZONE, timezone));
                }
                else {
                    queryParams.add(new BasicNameValuePair(PARAM_TIMEZONE, getTimezoneOffset()));
                }

                // Fixed page size.
                queryParams.add(new BasicNameValuePair(PARAM_PAGE_SIZE, Integer.toString(DEFAULT_PAGE_SIZE)));

                queryString = URLEncodedUtils.format(queryParams, "utf-8");
            }

            return RequestDataUtil.get(ENDPOINT, queryString, accessToken);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject responseObject) {
            super.onPostExecute(responseObject);
        }
    }

    private static String getTimezoneOffset() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        Date currentLocalTime = calendar.getTime();
        DateFormat date = new SimpleDateFormat("Z", Locale.getDefault());
        String offset = date.format(currentLocalTime);

        return offset.substring(2, 3);
    }
}
