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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.util.SharedPrefUtil;

import java.io.ByteArrayOutputStream;

public class UploadProfileService extends IntentService {
    private static final String TAG = "UploadProfileService";

    public UploadProfileService() {
        super(UploadProfileService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());

        Uri uri = intent.getData();
        Bitmap bmp = BitmapFactory.decodeFile(uri.getPath());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 0, baos);

        String url = String.format("%s%s", BuildConfig.SERVER_URL, "/users/profile/upload/");

        try {
            MultipartHttpClient client = new MultipartHttpClient(url, sharedPrefUtil.getAccessToken());
            client.connectForMultipart();
            client.addFilePart("image", "user-profile-image.jpg", baos.toByteArray());
            client.finishMultipart();
            String data = client.getResponse();

            Log.d(TAG, "Post profile image resp:\n" + data);
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
    }
}
