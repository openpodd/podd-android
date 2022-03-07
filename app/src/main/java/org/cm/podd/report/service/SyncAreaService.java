package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import org.cm.podd.report.util.RequestDataUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public class SyncAreaService extends IntentService {

    public static final String TAG = "SyncAreaService";

    public SyncAreaService() {
        super(SyncAreaService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String currentMd5Value = getCurrentMd5();
        if (!currentMd5Value.equals("")) {
            // http get md5 first
            String newMd5Value = downloadMd5File();

            // if download md5 != current md5 then download content
            if (!newMd5Value.equals(currentMd5Value)) {
                downloadAreaFile();
            }
        } else {
            // download content
            downloadAreaFile();

            // download md5
            downloadMd5File();
        }
    }

    private void downloadAreaFile() {
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/static/area/area.json", null, null);
        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            try {
                FileOutputStream fos = getApplicationContext().openFileOutput("area.json", MODE_PRIVATE);
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                writer.write(resp.getRawData());
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getCurrentMd5() {
        try {
            FileInputStream fis = getApplicationContext().openFileInput("area.md5");
            InputStreamReader inputStreamReader =
                    new InputStreamReader(fis, StandardCharsets.UTF_8);
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                while (line != null) {
                    stringBuilder.append(line).append('\n');
                    line = reader.readLine();
                }
                inputStreamReader.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not read md5", e);
            } finally {
                return stringBuilder.toString().replace("\n", "");
            }
        } catch (FileNotFoundException fne) {
            // do nothing
        }
        return "";
    }

    /* return md5 value
     */
    private String downloadMd5File() {
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/static/area/area.md5", null, null);
        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            try {
                FileOutputStream fos = getApplicationContext().openFileOutput("area.md5", MODE_PRIVATE);
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                writer.write(resp.getRawData());
                writer.close();
                return resp.getRawData();
            } catch (FileNotFoundException e) {
                // should not be here
            } catch (IOException e) {
                Log.e(TAG, "error writing file md5", e);
            }
        }
        return "";
    }
}
