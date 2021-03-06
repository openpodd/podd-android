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
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;

import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.FollowAlertDataSource;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.service.FollowAlertScheduleService;
import org.cm.podd.report.service.UploadProfileService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class SettingActivity extends AppCompatActivity {

    public static final String TAG = "SettingActivity";
    private static final int REQ_CODE_PICK_IMAGE = 1;
    private static final int REQ_CODE_TAKE_IMAGE = 2;
    private static final int REQUEST_FOR_WRITE_EXTERNAL_STORAGE_AND_CAPTURE_CAMERA = 21;
    private static final int REQUEST_FOR_WRITE_EXTERNAL_STORAGE_FOR_GALLERY = 22;

    SharedPrefUtil sharedPrefUtil;
    ImageView profileImageView;
    Uri mCurrentPhotoUri;

    Fragment mCurrentFragment;
    Bundle bundle;

    private Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());

        final Context context = this;

        setContentView(R.layout.activity_setting);

        Typeface face = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);

        final TextView userText = (TextView) findViewById(R.id.username);
        userText.setText(sharedPrefUtil.getUserName());
        userText.setTypeface(face);

        final TextView nameText = (TextView) findViewById(R.id.name);
        nameText.setText(sharedPrefUtil.getFullName());
        nameText.setTypeface(face);

        final TextView versionCodeText = (TextView) findViewById(R.id.version_code);
        versionCodeText.setText(String.valueOf(BuildConfig.VERSION_CODE));
        versionCodeText.setTypeface(face);

        final TextView versionNameText = (TextView) findViewById(R.id.version_name);
        versionNameText.setText(BuildConfig.VERSION_NAME);
        versionNameText.setTypeface(face);

        ((TextView) findViewById(R.id.username_label)).setTypeface(face);
        ((TextView) findViewById(R.id.password_label)).setTypeface(face);
        ((TextView) findViewById(R.id.app_version_code_label)).setTypeface(face);
        ((TextView) findViewById(R.id.app_version_name_label)).setTypeface(face);

        profileImageView = (ImageView) findViewById(R.id.profileImage);
        String profileImageFilePath = sharedPrefUtil.getProfileImageFilePath();
        Bitmap profileBitmap;
        if (profileImageFilePath == null) {
            // Use default profile image if not setup
            profileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
            profileImageView.setImageBitmap(profileBitmap);
        } else {
            if (profileImageFilePath.matches("^https?://.*")) {
                Picasso.with(this)
                        .load(profileImageFilePath)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.avatar)
                        .into(profileImageView);
            } else {
                profileBitmap = BitmapFactory.decodeFile(Uri.parse(profileImageFilePath).getPath());
                // use default image, if user deleted an image somehow
                if (profileBitmap == null) {
                    profileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                }
                profileImageView.setImageBitmap(profileBitmap);
            }
        }
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment fragment = new MediaChoiceDialog();
                fragment.show(getSupportFragmentManager(), "MediaChoiceDialog");
            }
        });

        findViewById(R.id.reset_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ResetPasswordActivity.class);
                context.startActivity(intent);
            }
        });


        FontUtil.overrideFonts(this, profileImageView.getRootView());

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("Setting");
        tracker.send(new HitBuilders.AppViewBuilder().build());

        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void cropImage() {
        Intent photoPickerIntent;
        if (mCurrentPhotoUri == null) {
            // no photo to edit, then first select what to edit
            photoPickerIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoPickerIntent.setType("image/*");
        } else {
            // Edit photo after taken
            photoPickerIntent = new Intent("com.android.camera.action.CROP");
            // indicate taken image type and Uri
            photoPickerIntent.setDataAndType(mCurrentPhotoUri, "image/*");
            photoPickerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        photoPickerIntent.putExtra("crop", "true");
        photoPickerIntent.putExtra("aspectX", 1);
        photoPickerIntent.putExtra("aspectY", 1);
        photoPickerIntent.putExtra("outputX", 400);
        photoPickerIntent.putExtra("outputY", 400);

        photoPickerIntent.putExtra("return-data", true);
        photoPickerIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        startActivityForResult(photoPickerIntent, REQ_CODE_PICK_IMAGE);
    }

    private Uri getImageUri() {
        return FileProvider.getUriForFile(this, "org.cm.podd.report.fileprovider", createImageFile());
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
                        Uri selectedImage = imageReturnedIntent.getData();
                        if (extras != null) {
                            // get the cropped bitmap
                            Bitmap bmp = extras.getParcelable("data");

                            profileImageView.setImageBitmap(bmp);
                            mCurrentPhotoUri = null;
                            // save output file and save path to share pref
                            saveProfileImage(bmp);
                            Crouton.makeText(this, getString(R.string.upload_image_success), Style.INFO, R.id.errorArea).show();
                        } else if (selectedImage != null) {
                            String[] filePathColumn = {MediaStore.Images.Media.DATA};

                            Cursor cursor = getContentResolver().query(
                                    selectedImage, filePathColumn, null, null, null);
                            cursor.moveToFirst();

                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String filePath = cursor.getString(columnIndex);
                            cursor.close();


                            Bitmap bmp = BitmapFactory.decodeFile(filePath);

                            profileImageView.setImageBitmap(bmp);
                            mCurrentPhotoUri = null;
                            // save output file and save path to share pref
                            saveProfileImage(bmp);
                            Crouton.makeText(this, getString(R.string.upload_image_success), Style.INFO, R.id.errorArea).show();
                        } else {
                            Crouton.makeText(this, getString(R.string.upload_image_error), Style.ALERT, R.id.errorArea).show();
                        }
                    }
                    break;

                case REQ_CODE_TAKE_IMAGE:
                    cropImage();
                    break;

            }
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

    public void setTitleToolbar(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_setting));
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
//        actionBar.setLogo(R.drawable.arrow_left_with_pad);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == android.R.id.home){
            if (bundle != null && findViewById(R.id.form_content).getVisibility() == View.VISIBLE) {
                findViewById(R.id.detail_content).setVisibility(View.VISIBLE);
                findViewById(R.id.form_content).setVisibility(View.GONE);
                StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_setting));

                setTitleToolbar(getString(R.string.action_settings));
                return true;

            } else {
                this.finish();
                return true;
            }

        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // clear access token
        sharedPrefUtil.clearAllData();

        clearAllPendingAlert();

        // clear all report data
        ReportDataSource db = new ReportDataSource(this);
        db.clearAllData();

        // Back to home, then redirect to login
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }

    private void clearAllPendingAlert() {
        FollowAlertDataSource followAlertDataSource = new FollowAlertDataSource(getApplicationContext());
        List<Map> requestCodes = followAlertDataSource.getUnDoneRequest();
        for (Map tmp : requestCodes) {
            FollowAlertScheduleService.cancelFollowAlert(getApplicationContext(),
                    (Long) tmp.get("reportId"),
                    (Integer) tmp.get("requestCode"),
                    (Long) tmp.get("reportType"),
                    (String) tmp.get("message")
            );
        }
    }

    private void onMediaChoiceRequest(int requestCode) {
        int forwardCode = 0;

        switch (requestCode) {
            case REQ_CODE_PICK_IMAGE:
                forwardCode = REQUEST_FOR_WRITE_EXTERNAL_STORAGE_FOR_GALLERY;
                break;

            case REQ_CODE_TAKE_IMAGE:
                forwardCode = REQUEST_FOR_WRITE_EXTERNAL_STORAGE_AND_CAPTURE_CAMERA;
                break;
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "ไม่ได้รับการอนุญาติให้เก็บรูปลง sdcard ได้", Toast.LENGTH_LONG).show();

            }
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    forwardCode);
        } else {
            switch (requestCode) {
                case REQ_CODE_PICK_IMAGE:
                    pickImageFromGallery();
                    break;
                case REQ_CODE_TAKE_IMAGE:
                    captureImageFromCamera();
                    break;
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("xxx", "onRequestPermissionsResult" + requestCode);
        if (requestCode == REQUEST_FOR_WRITE_EXTERNAL_STORAGE_AND_CAPTURE_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImageFromCamera();
            }
        }

        if (requestCode == REQUEST_FOR_WRITE_EXTERNAL_STORAGE_FOR_GALLERY) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            }
        }
    }

    private void pickImageFromGallery() {
        mCurrentPhotoUri = null;
        cropImage();
    }

    private void captureImageFromCamera() {
        mCurrentPhotoUri = getImageUri();
        Intent photoTakerIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoTakerIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024 * 1024);
        photoTakerIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        photoTakerIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            photoTakerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                photoTakerIntent.setClipData(ClipData.newRawUri("", mCurrentPhotoUri));
            }
        }
        startActivityForResult(photoTakerIntent, REQ_CODE_TAKE_IMAGE);
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
