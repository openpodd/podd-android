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
package org.cm.podd.report.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.service.UploadProfileService;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingActivity extends ActionBarActivity {

    public static final String TAG = "SettingActivity";
    private static final int REQ_CODE_PICK_IMAGE = 1;
    private static final int REQ_CODE_TAKE_IMAGE = 2;
    private static final String TEMP_PHOTO_FILE = "temporary_holder.jpg";

    SharedPrefUtil sharedPrefUtil;
    ImageView profileImageView;
    Uri mCurrentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());

        setContentView(R.layout.activity_setting);

        Typeface face = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);

        final EditText userText = (EditText) findViewById(R.id.username);
        userText.setText(sharedPrefUtil.getFullName());
        userText.setTypeface(face);

        final EditText versionCodeText = (EditText) findViewById(R.id.version_code);
        versionCodeText.setText(String.valueOf(BuildConfig.VERSION_CODE));
        versionCodeText.setTypeface(face);

        final EditText versionNameText = (EditText) findViewById(R.id.version_name);
        versionNameText.setText(BuildConfig.VERSION_NAME);
        versionNameText.setTypeface(face);

        ((TextView) findViewById(R.id.username_label)).setTypeface(face);
        ((TextView) findViewById(R.id.app_version_code_label)).setTypeface(face);
        ((TextView) findViewById(R.id.app_version_name_label)).setTypeface(face);

        final SettingActivity me = this;

        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(me)
                    .setTitle("ยืนยันการออกจากระบบ")
                    .setMessage("ข้อมูลรายงานทั้งหมดจะถูกลบ และถูกยกเลิกการส่ง\n\n"
                            + "คุณต้องการออกจากระบบหรือไม่")
                    .setPositiveButton("ตกลง", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            logout();
                        }
                    })
                    .setNegativeButton("ยกเลิก", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
            }
        });

        profileImageView = (ImageView) findViewById(R.id.profileImage);
        String profileImageFilePath = sharedPrefUtil.getProfileImageFilePath();
        Bitmap profileBitmap;
        if (profileImageFilePath == null) {
            // Use default profile image if not setup
            profileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gallery_default);
        } else {
            profileBitmap = BitmapFactory.decodeFile(Uri.parse(profileImageFilePath).getPath());
        }
        profileImageView.setImageBitmap(profileBitmap);
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment fragment = new MediaChoiceDialog();
                fragment.show(getSupportFragmentManager(), "MediaChoiceDialog");
            }
        });
    }

    private void cropImage(Uri fileUri) {
        Intent photoPickerIntent;
        if (mCurrentPhotoUri == null) {
            // no photo to edit, then first select what to edit
            photoPickerIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        } else {
            // Edit photo after taken
            photoPickerIntent = new Intent("com.android.camera.action.CROP");
        }
        // indicate image type and Uri
        photoPickerIntent.setDataAndType(fileUri, "image/*");
        photoPickerIntent.putExtra("crop", "true");
        photoPickerIntent.putExtra("aspectX", 0);
        photoPickerIntent.putExtra("aspectY", 0);
        photoPickerIntent.putExtra("outputX", 400);
        photoPickerIntent.putExtra("outputY", 400);

        photoPickerIntent.putExtra("return-data", true);
        photoPickerIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        startActivityForResult(photoPickerIntent, REQ_CODE_PICK_IMAGE);
    }

    private Uri getTempUri() {
        return Uri.fromFile(getTempFile());
    }

    private File getTempFile() {
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        if (! storageDir.exists()) {
            if (! storageDir.mkdir()) {
                Log.d(TAG, "can't create directory " + storageDir);
            }
        }

        File file = new File(storageDir, TEMP_PHOTO_FILE);

        try {
            file.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "error create new file", e);
        }

        return file;
    }

    private Uri getImageUri() {
        return Uri.fromFile(createImageFile());
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        Log.d(TAG, "storageDir = " + storageDir + " file name = " + imageFileName);
        if (! storageDir.exists()) {
            if (! storageDir.mkdir()) {
                Log.d(TAG, "can't create directory " + storageDir);
            }
        }
        File image = null;
        try {
            image = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_CODE_PICK_IMAGE:
                    if (imageReturnedIntent != null) {

                        Bundle extras = imageReturnedIntent.getExtras();
                        // get the cropped bitmap
                        Bitmap bmp = extras.getParcelable("data");
                        profileImageView.setImageBitmap(bmp);

                        mCurrentPhotoUri = null;

                        // save output file and save path to share pref
                        saveProfileImage(bmp);
                    }
                    break;

                case REQ_CODE_TAKE_IMAGE:
                    cropImage(mCurrentPhotoUri);
                    break;

            }
            File tempFile = getTempFile();
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private void saveProfileImage(Bitmap bmp) {
        File file = createImageFile();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Uri imageUri = Uri.parse(file.getPath());
        sharedPrefUtil.setProfileImageFilePath(imageUri.toString());

        // upload to server
        Intent intent = new Intent(this, UploadProfileService.class);
        intent.setData(imageUri);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_setting));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);

        return true;
    }

    private void logout() {
        // clear access token
        sharedPrefUtil.clearAllData();

        // clear all report data
        ReportDataSource db = new ReportDataSource(this);
        db.clearAllData();

        // Back to home, then redirect to login
        finish();
    }

    private void onMediaChoiceRequest(int requestCode) {
        switch (requestCode) {
            case REQ_CODE_PICK_IMAGE:
                cropImage(getTempUri());
                break;

            case REQ_CODE_TAKE_IMAGE:
                mCurrentPhotoUri = getImageUri();
                Intent photoTakerIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoTakerIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024 * 1024);
                photoTakerIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                photoTakerIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
                startActivityForResult(photoTakerIntent, REQ_CODE_TAKE_IMAGE);
                break;
        }
    }

    /**
     * Dialog for image selection methods
     */
    public static class MediaChoiceDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_pick_media_options)
                    .setItems(R.array.pick_media_selection, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((SettingActivity) getActivity()).onMediaChoiceRequest(
                                    which == 0 ?
                                    REQ_CODE_TAKE_IMAGE : REQ_CODE_PICK_IMAGE);
                        }
                    });
            return builder.create();
        }
    }
}
