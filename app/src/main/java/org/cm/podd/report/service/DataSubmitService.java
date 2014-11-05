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
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.model.Queue;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.model.ReportImage;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class DataSubmitService extends IntentService {

    private static final String TAG = "DataSubmitService";
    private static final String SERVER_HOST = "mister-podd.herokuapp.com";
//    private static final String SERVER_HOST = "128.1.1.237";
    private static final int SERVER_PORT = 80;
    private Charset utf8Charset = Charset.forName("UTF-8");

    public DataSubmitService() {
        super(null);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public DataSubmitService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        HttpURLConnection conn = null;
        Log.i(TAG, "submit data " + Long.toString(System.currentTimeMillis()));

        ReportQueueDataSource queueDataSource = new ReportQueueDataSource(getApplicationContext());
        try {
            List<Queue> queues = queueDataSource.getAllQueues();

            for (Queue que : queues) {
                ReportDataSource reportDataSource = new ReportDataSource(getApplicationContext());

                long id = que.getId();
                String type = que.getType();
                long reportId = que.getReportId();
                String guid = que.getGuid();

                Log.d(TAG, String.format("type= %s, report id= %d, guid= %s", type, reportId, guid));
                boolean success = false;

                if (type.equals(ReportQueueDataSource.DATA_TYPE)) {
                    // get report data
                    Report report = reportDataSource.getById(reportId);

                    success = submitReport(report.getFormData(), report.getId(), report.getGuid());
                    if (success) {
                        // mark report as done submitting to server
                        reportDataSource.updateSubmit(reportId);
                    }
                } else if (type.equals(ReportQueueDataSource.IMAGE_TYPE)) {
                    // get all images in a report
                    List<ReportImage> images = reportDataSource.getAllImage(reportId);
                    List<byte[]> filesData = new ArrayList<byte[]>();
                    List<String> notes = new ArrayList<String>();

                    for (ReportImage image : images) {
                        String uriStr = image.getImageUri();
                        Uri uri = Uri.parse(uriStr);
                        String filePath;

                        // find image file path
                        Cursor c = getContentResolver().query(uri, null, null, null, null);
                        if (c == null) {
                            filePath = uri.getPath();
                        } else {
                            c.moveToFirst();
                            int idx = c.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                            filePath = c.getString(idx);
                            c.close();
                        }

                        // convert file to bytes
                        filesData.add(getImageByte(filePath));
                        notes.add(image.getNote());
                    }
                    // submit image bytes
                    success = submitImages(filesData, notes, reportId, guid);
                }

                // After queue was submitted successfully then remove it
                if (success) {
                    Log.i(TAG, "success! submit report " + type);
                    queueDataSource.remove(id);
                }
            }

        } catch (Exception e) {
            // handle exception
            Log.e(TAG, "Error submit data", e);

        } finally {
            Log.e(TAG, "---- end submit");
            // signal this service is ended regardless successful submission
            // so that new work can be executed next
            Intent networkIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
            networkIntent.putExtra("SubmitDone", true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(networkIntent);
        }

    }

    private boolean submitReport(String body, long reportId, String guid)
            throws URISyntaxException, IOException {

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);
        boolean success = false;

        try {
            String query = String.format("type=data&reportId=%d&guid=%s", reportId, guid);
            URI http = URIUtils.createURI("http", SERVER_HOST, SERVER_PORT, "/report", query, null);
            Log.i(TAG, "submit report url=" + http.toURL());

            HttpPost post = new HttpPost(http);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.setCharset(utf8Charset);
            builder.addTextBody("data", body, ContentType.APPLICATION_JSON);

            post.setEntity(builder.build());
            HttpResponse response = null;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // Detect server complaints
            success = response.getStatusLine().getStatusCode() == 200;
            entity.consumeContent();

        } finally {
            client.getConnectionManager().shutdown();
        }
        return success;
    }

    private boolean submitImages(List<byte[]> filesData, List<String> notes, long reportId, String guid)
            throws URISyntaxException, IOException {

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);
        boolean success = false;

        try {
            String query = String.format("type=image&imageId=%d&guid=%s", reportId, guid);
            URI http = URIUtils.createURI("http", SERVER_HOST, SERVER_PORT, "/image", query, null);
            Log.i(TAG, "submit report image url=" + http.toURL());

            HttpPost post = new HttpPost(http);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.setCharset(utf8Charset);

            for (int i = 0; i < filesData.size(); i++) {
                byte[] data = filesData.get(i);
                String note = notes.get(i) == null ? "" : notes.get(i);
                String fileName = String.format("IMG_%s_%d", guid, System.currentTimeMillis());

                builder.addPart("image"+i, new ByteArrayBody(data, fileName));
                Log.d(TAG, String.format("note%d= %s", i, note));
                builder.addTextBody("note" + i, note, ContentType.create("plain/text", utf8Charset));
            }

            post.setEntity(builder.build());
            HttpResponse response = null;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            success = response.getStatusLine().getStatusCode() == 200;
            // Detect server complaints
            entity.consumeContent();

        } finally {
            client.getConnectionManager().shutdown();
        }
        return success;

    }

    public byte[] getImageByte(String path) throws IOException  {
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(path);
            int read = 0;
            while ( (read = ios.read(buffer)) != -1 ) {
                ous.write(buffer, 0, read);
            }
        } finally {
            try {
                if ( ous != null )
                    ous.close();
            } catch ( IOException e) {
            }

            try {
                if ( ios != null )
                    ios.close();
            } catch ( IOException e) {
            }
        }
        return ous.toByteArray();
    }


    public static String convertInputStream2String(InputStream in) {
        StringBuffer sb = new StringBuffer();
        if (in == null) {
            return sb.toString();
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in,
                    "utf-8"), 8);
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
