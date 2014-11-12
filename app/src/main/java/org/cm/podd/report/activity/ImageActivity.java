package org.cm.podd.report.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import org.cm.podd.report.R;
import org.cm.podd.report.view.ZoomableImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ImageActivity extends ActionBarActivity {

    private static final String TAG = "ImageActivity";
    ZoomableImageView mImageView;
    String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_image);

        mImageView = (ZoomableImageView) findViewById(R.id.image_view);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            imagePath = intent.getStringExtra("imagePath");
        } else {
            imagePath = savedInstanceState.getString("imagePath");
        }

        Uri uri = Uri.parse(imagePath);
        try {
            // Get image from uri file:///path or content://path
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            if (bitmap != null) {
                int rotate = neededRotation(uri);
                Matrix matrix = new Matrix();
                if (rotate != 0) {
                    matrix.postRotate(rotate);
                }
                bitmap = getResizedTextureBitmap(bitmap, matrix);
                mImageView.setImageBitmap(bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("imagePath", imagePath);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public int neededRotation(Uri uri) {
        String imagePath = uri.getPath();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            imagePath = cursor.getString(idx);
        }
        try {

            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            }
            return 0;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Resize bitmap to fit in gl texture limit
     * Fix error 'OpenGLRenderer﹕ Bitmap too large to be uploaded into a texture'
     */
    public Bitmap getResizedTextureBitmap(Bitmap src, Matrix mat) {
        Bitmap bmp = src;
        Log.d(TAG, "src w/h=" + src.getWidth() + "/" + src.getHeight());

        int[] maxTextureSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        Log.i("glinfo", "Max texture size = " + maxTextureSize[0]);

        int maxSize = maxTextureSize[0];

        if (src.getHeight() > maxSize || src.getWidth() > maxSize) {
            // Determine how much to scale.
            // This way the image always stays inside bounding gl limit
            float scaleWidth = maxSize / (float) src.getWidth() ;
            float scaleHeight = maxSize / (float) src.getHeight();
            float scale = (scaleWidth <= scaleHeight) ? scaleWidth : scaleHeight;
            Log.d(TAG, "bitmap scale = " + scale);

            // Resize bitmap
            mat.postScale(scale, scale);
            bmp = Bitmap.createBitmap(src, 0, 0,
                    src.getWidth(), src.getHeight(), mat, false);
        }
        Log.d(TAG, "bmp w/h=" + bmp.getWidth() + "/" + bmp.getHeight());
        return bmp;
    }
}
