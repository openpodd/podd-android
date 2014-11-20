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
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.services.s3.model.ObjectMetadata;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.model.Queue;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.model.ReportImage;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;

public class DataSubmitService extends IntentService {

    private static final String TAG = "DataSubmitService";
    private static final String S3IMAGE_URL_PREFIX = "https://s3-ap-southeast-1.amazonaws.com/podd-dev/";
    public static final String ACTION_REPORT_STATUS_CHANGE = "reportStatusChange";

    public DataSubmitService() {
        super(DataSubmitService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPrefUtil.getPrefs(getApplicationContext());

        HttpURLConnection conn = null;
        Log.i(TAG, "submit data " + Long.toString(System.currentTimeMillis()));

        ReportQueueDataSource queueDataSource = new ReportQueueDataSource(getApplicationContext());
        ReportDataSource reportDataSource = new ReportDataSource(getApplicationContext());

        try {
            List<Queue> queues = queueDataSource.getAllQueues();

            for (Queue que : queues) {

                long id = que.getId();
                String type = que.getType();
                long reportId = que.getReportId();
                long imageId = que.getImageId();

                Log.d(TAG, String.format("type= %s, report id= %d, image id= %d", type, reportId, imageId));
                boolean success = false;

                if (type.equals(ReportQueueDataSource.DATA_TYPE)) {
                    // get report data
                    Report report = reportDataSource.getById(reportId);

                    success = submitReport(report);
                    if (success) {
                        // mark report as done submitting to server
                        reportDataSource.updateSubmit(reportId);
                    }

                } else if (type.equals(ReportQueueDataSource.IMAGE_TYPE)) {
                    // get image data
                    ReportImage image = reportDataSource.getImageById(imageId);
                    Report report = reportDataSource.getById(image.getReportId());

                    if (image != null) {
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

                        success = uploadToS3(image.getGuid(), filePath, image.getThumbnail());

                        // submit image bytes
                        if (success) {
                            success = submitImage(image, report.getGuid());
                            if (success) {
                                reportDataSource.updateImageSubmit(imageId);
                            }
                        }
                    } else {
                        // Image could be deleted by user anytime,
                        // so just remove a queue if no image found
                        success = true;
                    }
                }

                if (success) {
                    // After queue was submitted successfully then remove it
                    Log.i(TAG, "success! submit report " + type);
                    queueDataSource.remove(id);
                }
            }
            // notify report list about change in report status
            sendBroadcast(new Intent(ACTION_REPORT_STATUS_CHANGE));

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

            queueDataSource.close();
            reportDataSource.close();
        }

    }

    private boolean submitReport(Report report)
            throws URISyntaxException, IOException, JSONException {

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);
        boolean success = false;

        try {
            URI http = new URI(BuildConfig.SERVER_URL + "/reports/");
            Log.i(TAG, "submit report url=" + http.toURL());

            HttpPost post = new HttpPost(http);
            post.setHeader("Content-type", "application/json");
            post.setHeader("Authorization", "Token " + SharedPrefUtil.getAccessToken());

            SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

            JSONObject data = new JSONObject();
            data.put("reportId", report.getId());
            data.put("guid", report.getGuid());
            data.put("reportTypeId", report.getType());
            data.put("date", sdfDateTime.format(report.getDate()));
            data.put("incidentDate", sdfDate.format(report.getStartDate()));
            if (report.getRegionId() != 0) {
                data.put("administrationAreaId", report.getRegionId());
            }
            data.put("remark", report.getRemark());
            data.put("negative", report.getNegative() == 1);
            data.put("formData", report.getNegative() == 1 ?
                    report.getSubmitJSONFormData() : new JSONObject());

            post.setEntity(new StringEntity(data.toString(), HTTP.UTF_8));
            Log.d(TAG, "request with " + EntityUtils.toString(post.getEntity()));

            HttpResponse response;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // Detect server complaints
            int statusCode = response.getStatusLine().getStatusCode();
            Log.d(TAG, "status code=" + statusCode);

            if (statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                Log.e(TAG, "error " + EntityUtils.toString(response.getEntity()));
            }
            success = statusCode == 201;
            entity.consumeContent();

        } finally {
            client.getConnectionManager().shutdown();
        }
        return success;
    }

    private boolean submitImage(ReportImage reportImage, String reportGuid)
            throws URISyntaxException, IOException, JSONException {


        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);
        boolean success = false;

        try {
            URI http = new URI(BuildConfig.SERVER_URL + "/reportImages/");
            Log.i(TAG, "submit report image url=" + http.toURL());

            HttpPost post = new HttpPost(http);
            post.setHeader("Content-type", "application/json");
            post.setHeader("Authorization", "Token " + SharedPrefUtil.getAccessToken());

            String note = reportImage.getNote();
            if (note == null) {
                note = "";
            }


            JSONObject data = new JSONObject();
            data.put("note", note);
            data.put("reportGuid", reportGuid);
            data.put("guid", reportImage.getGuid());
            data.put("imageUrl", S3IMAGE_URL_PREFIX + reportImage.getGuid());
            data.put("thumbnailUrl", S3IMAGE_URL_PREFIX + reportImage.getGuid() + "-thumbnail");

            post.setEntity(new StringEntity(data.toString(), HTTP.UTF_8));

            HttpResponse response;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            int statusCode = response.getStatusLine().getStatusCode();
            Log.d(TAG, "status code=" + statusCode);
            success = statusCode == 201;
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

    public static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    private boolean uploadToS3(String guid, String filePath, Bitmap thumbnail) {
        TransferManager transferManager = new TransferManager(
                new BasicAWSCredentials(SharedPrefUtil.getAwsSecretKey(), SharedPrefUtil.getAwsAccessKey()));

        // upload thumbnail
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        byte[] bitmapData = bos.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapData);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(bitmapData.length);
        meta.setContentType("image/jpeg");

        Upload upload1 = transferManager.upload(
                "podd-dev", // bucket
                guid + "-thumbnail", // name
                bs,
                meta
        );


        // upload image
        File imageFile = new File(filePath);
        Upload upload2 = transferManager.upload(
                "podd-dev",
                guid,
                imageFile
        );

        try {
            upload1.waitForUploadResult();
            upload2.waitForUploadResult();

        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG, "upload to s3 error", e);
            return false;
        }

        return true;

    }
}
