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

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class RequestDataUtil {

    private static final String TAG = "RequestDataUtil";
    private static Charset utf8Charset = Charset.forName("UTF-8");

    public static JSONObject post(String path, String query, String json) {
        JSONObject jsonObj = null;
        String reqUrl = String.format("%s%s%s", SharedPrefUtil.getServerAddress(), path,
                query == null ? "" : "?"+query);
        Log.i(TAG, "submit report url=" + reqUrl);

        HttpURLConnection urlConn = null;
        try {
            URL url = new URL(reqUrl);
            urlConn = (HttpURLConnection) url.openConnection();

            if (json != null) {
                urlConn.setDoOutput(true); // HTTP Post
                urlConn.setChunkedStreamingMode(0);
                urlConn.setUseCaches(false);
                urlConn.setRequestProperty("Content-Type", "application/json");

                // Send POST output.
                OutputStream out = new BufferedOutputStream(urlConn.getOutputStream());
                out.write(json.getBytes("utf-8"));
                out.close();
            }

            urlConn.connect();

            InputStream in;
            // if server response code is error (>400) and there is response body
            // then it's in errror stream
            try {
                in = new BufferedInputStream(urlConn.getInputStream());
            } catch (FileNotFoundException ex) {
                in = new BufferedInputStream(urlConn.getErrorStream());
            }
            int resultCode = urlConn.getResponseCode();
            // resp code not above 400 is ok
            if (resultCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                String respData = FileUtil.convertInputStreamToString(in);
                jsonObj = new JSONObject(respData);
            }

        } catch (Exception e ) {
            Log.e(TAG, e.getMessage(), e);

        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        return jsonObj;
    }

    public static JSONObject postMultipart(String path, String query, String name, String jsonData) {
        JSONObject jsonObj = null;
        String reqUrl = String.format("%s%s%s", SharedPrefUtil.getServerAddress(), path,
                query == null ? "" : "?"+query);
        Log.i(TAG, "submit report url=" + reqUrl);

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);

        try {
            HttpPost post = new HttpPost(reqUrl);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.setCharset(utf8Charset);
            builder.addTextBody("data", jsonData, ContentType.APPLICATION_JSON);

            post.setEntity(builder.build());
            HttpResponse response;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // Detect server complaints
            int statusCode = response.getStatusLine().getStatusCode();
            Log.e(TAG, "status code=" + statusCode);
            entity.consumeContent();

            if (statusCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                InputStream in = entity.getContent();
                String resp = FileUtil.convertInputStreamToString(in);

                jsonObj = new JSONObject(resp);
            }

        } catch (ClientProtocolException e) {
            Log.e(TAG, "error post data", e);
        } catch (IOException e) {
            Log.e(TAG, "error post data", e);

        } catch (JSONException e) {
            Log.e(TAG, "error convert json", e);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return jsonObj;
    }
}
