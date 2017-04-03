package org.cm.podd.report.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import org.cm.podd.report.R;
import org.cm.podd.report.view.ZoomableImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageActivity extends AppCompatActivity {

    private static final String TAG = "ImageActivity";
    //The new size we want to scale to
    final int REQUIRED_SIZE = 2048;

    ZoomableImageView mImageView;
    ProgressBar mProgressBar;
    String imagePath;
    Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image);

        mImageView = (ZoomableImageView) findViewById(R.id.image_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            imagePath = intent.getStringExtra("imagePath");
        } else {
            imagePath = savedInstanceState.getString("imagePath");
        }

        if (imagePath.matches("^https?://.*")) {
           Picasso.with(getApplicationContext())
                   .load(imagePath)
                   .into(mImageView);
        } else {
            setImage(imagePath);
        }

    }

    private void setImage(String imagePath) {
        Uri uri = Uri.parse(imagePath);

        // Get image from uri file:///path or content://path
//            mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

        String imageFilePath = getImageFilePath(uri);

        int rotate = neededRotation(imageFilePath);
        Matrix matrix = new Matrix();
        if (rotate != 0) {
            matrix.postRotate(rotate);
        }

        mBitmap = decodeFile(new File(imageFilePath));
        if (mBitmap != null) {
            mBitmap = Bitmap.createBitmap(mBitmap, 0, 0,
                    mBitmap.getWidth(), mBitmap.getHeight(), matrix, false);

            mImageView.setImageBitmap(mBitmap);
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBitmap != null) {
            mBitmap.recycle();
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

    private String getImageFilePath(Uri uri) {
        String imageFilePath = uri.getPath();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            imageFilePath = cursor.getString(idx);
        }
        return imageFilePath;
    }

    public int neededRotation(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
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


    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);

            //Find the correct scale value. It should be the power of 2.
            int scale=1;
            while (o.outWidth*(2/scale) >= REQUIRED_SIZE || o.outHeight*(2/scale) >= REQUIRED_SIZE)
                scale*=2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;

            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error decode bitmap file");
        }

        return null;
    }

    /**
     * Resize bitmap to fit in gl texture limit
     * Fix error 'OpenGLRenderer﹕ Bitmap too large to be uploaded into a texture'
     *
     * ** still causing OutOfMemory error **
     * ** also not getting gl maxTexture value sometimes when screen rotates **
     */
    public Bitmap getResizedTextureBitmap(Bitmap src, Matrix mat) {
        Bitmap bmp = src;
        Log.d(TAG, "src w/h=" + src.getWidth() + "/" + src.getHeight());

        int[] maxTextureSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        Log.i("glinfo", "Max texture size = " + maxTextureSize[0]);

        int maxSize = maxTextureSize[0];

        if ((src.getHeight() > maxSize || src.getWidth() > maxSize) &&
                src.getHeight() > 0 && src.getWidth() > 0 && maxSize > 0) {
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

    public static class RemoteImageAsyncTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String url = params[0];
            String filePath = params[1];

            URL thumb_u;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                thumb_u = new URL(url);
                inputStream = thumb_u.openStream();

                outputStream = new BufferedOutputStream(new FileOutputStream(filePath));
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "Malformed URL", e);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "Cannot load image from :" + url, e);
                return false;
            }

            return true;
        }
    }
}
