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
package org.cm.podd.report.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RequestDataUtil {

    private static final String TAG = "RequestDataUtil";
    private static Charset utf8Charset = Charset.forName("UTF-8");

    public static ResponseObject post(String path, String query, String json, String token) {
        JSONObject jsonObj = null;
        int statusCode = 0;
        String reqUrl = String.format("%s%s%s", BuildConfig.SERVER_URL, path,
                query == null ? "" : "?"+query);
        Log.i(TAG, "submit url=" + reqUrl);
        Log.i(TAG, "post data=" + json);

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);

        try {
            HttpPost post = new HttpPost(reqUrl);
            post.setHeader("Content-Type", "application/json");
            if (token != null) {
                post.setHeader("Authorization", "Token " + token);
            }
            post.setEntity(new StringEntity(json, HTTP.UTF_8));

            HttpResponse response;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // Detect server complaints
            statusCode = response.getStatusLine().getStatusCode();
            Log.v(TAG, "status code=" + statusCode);

            if (statusCode < HttpURLConnection.HTTP_INTERNAL_ERROR) {
                InputStream in = entity.getContent();
                String resp = FileUtil.convertInputStreamToString(in);

                jsonObj = new JSONObject(resp);
                entity.consumeContent();
            }

        } catch (ClientProtocolException e) {
            Log.e(TAG, "error post data", e);
        } catch (IOException e) {
            Log.e(TAG, "Can't connect server", e);
        } catch (JSONException e) {
            Log.e(TAG, "error convert json", e);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return new ResponseObject(statusCode, jsonObj);
    }

    public static ResponseObject get(String path, String query, String token) {
        JSONObject jsonObj = null;
        String rawData = null;
        int statusCode = 0;
        String reqUrl = String.format("%s%s%s", BuildConfig.SERVER_URL, path,
                query == null ? "" : "?"+query);
        Log.i(TAG, "submit url=" + reqUrl);

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);

        try {
            HttpGet get = new HttpGet(reqUrl);
            if (token != null) {
                get.setHeader("Authorization", "Token " + token);
            }

            HttpResponse response;
            response = client.execute(get);
            HttpEntity entity = response.getEntity();

            // Detect server complaints
            statusCode = response.getStatusLine().getStatusCode();
            Log.v(TAG, "status code=" + statusCode);

            if (statusCode < HttpURLConnection.HTTP_INTERNAL_ERROR) {
                InputStream in = entity.getContent();
                rawData = FileUtil.convertInputStreamToString(in);

                entity.consumeContent();
            }

        } catch (ClientProtocolException e) {
            Log.e(TAG, "error post data", e);
        } catch (IOException e) {
            Log.e(TAG, "Can't connect server", e);
        } finally {
            client.getConnectionManager().shutdown();
        }

        ResponseObject respObj = new ResponseObject(statusCode, jsonObj);
        respObj.setRawData(rawData);
        return respObj;
    }


    public static class ResponseObject implements Serializable {
        private JSONObject jsonObject;
        private int statusCode;
        private String rawData;

        public ResponseObject(int statusCode, JSONObject jsonObject) {
            this.jsonObject = jsonObject;
            this.statusCode = statusCode;
        }

        public JSONObject getJsonObject() {
            return jsonObject;
        }

        public void setJsonObject(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getRawData() {
            return rawData;
        }

        public void setRawData(String rawData) {
            this.rawData = rawData;
        }
    }

    public static boolean hasNetworkConnection(Activity activity) {
        ConnectivityManager cm = ((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE));
        boolean connected = cm != null &&
                cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnected();

        if (!connected) {
            Crouton.makeText(activity, R.string.alert_no_network_connection, Style.ALERT).show();
        }
        return connected;
    }

    public static ResponseObject registerDeviceId(String deviceId, String token) {
        JSONObject jsonObj = null;
        int statusCode = 0;
        String reqUrl = "http://128.1.1.237:8081/gcm-demo/register";
        Log.i(TAG, "submit url=" + reqUrl);

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);

        try {
            HttpPost post = new HttpPost(reqUrl);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            if (token != null) {
                post.setHeader("Authorization", "Token " + token);
            }
            post.setEntity(new StringEntity("regId="+deviceId, HTTP.UTF_8));

            HttpResponse response;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // Detect server complaints
            statusCode = response.getStatusLine().getStatusCode();
            Log.v(TAG, "status code=" + statusCode);

            if (statusCode < HttpURLConnection.HTTP_INTERNAL_ERROR) {
                InputStream in = entity.getContent();
                String resp = FileUtil.convertInputStreamToString(in);
                Log.d(TAG, "Register device id : Response text= " + resp);
                jsonObj = new JSONObject();
                entity.consumeContent();
            }

        } catch (ClientProtocolException e) {
            Log.e(TAG, "error post data", e);
        } catch (IOException e) {
            Log.e(TAG, "Can't connect server", e);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return new ResponseObject(statusCode, jsonObj);
    }

}
