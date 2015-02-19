package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Fetch data from API using specific query.
 */
public class FilterService extends IntentService {
    private static final String TAG = "FilterService";

    private static final String ENDPOINT = "/reports/search/";

    private static final String ACTION_QUERY   = "podd.filter.query";

    private static final String PARAM_QUERY    = "q";
    private static final String PARAM_TIMEZONE = "tz";

    SharedPrefUtil sharedPrefUtil;

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
                if (timezone.isEmpty()) {
                    timezone = "7";
                }

                handleActionQuery(query, timezone);
            }
        }
    }

    private void handleActionQuery(String query, String timezone) {
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());

        Log.i(TAG, "Beginning network querying");

        try {
            RequestDataUtil.ResponseObject resp;

            Log.i(TAG, "Streaming data from network: " + ENDPOINT);
            resp = filterQuery(query, timezone);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Filtering success: "+ resp.getRawData());

                JSONArray filteredItems = new JSONArray(resp.getRawData());
            } else {
                Log.d(TAG, "Filtering fail with errorCode:" + resp.getStatusCode());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON Array", e);
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

        // Prepare query parameters
        if (!query.isEmpty() || !timezone.isEmpty()) {
            if (!query.isEmpty()) {
                params.add(new BasicNameValuePair(PARAM_QUERY, query));
            }

            if (!timezone.isEmpty()) {
                params.add(new BasicNameValuePair(PARAM_TIMEZONE, timezone));
            }

            queryString = URLEncodedUtils.format(params, "utf-8");
        }

        return RequestDataUtil.get(ENDPOINT, queryString, sharedPrefUtil.getAccessToken());
    }
}
