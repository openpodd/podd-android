package org.cm.podd.report.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ImageActivity;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.model.ReportImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Use the {@link ReportImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ReportImageFragment extends Fragment {

    private static final String ARG_REPORT_ID = "reportId";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 200;
    private static final int CHOOSE_IMAGE_ACTIVITY_REQUEST_CODE = 500;
    private static final String TAG = "ReportImageFragment";

    private long reportId;

    private ReportNavigationInterface navigationInterface;
    private ReportDataSource reportDataSource;

    private GridView gridView;
    private ImageAdapter imageAdapter;
    private List<ReportImage> allImage;

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param reportId
     * @return A new instance of fragment ReportImageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportImageFragment newInstance(long reportId) {
        ReportImageFragment fragment = new ReportImageFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }
    public ReportImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reportId = getArguments().getLong(ARG_REPORT_ID);
        }
        reportDataSource = new ReportDataSource(this.getActivity());

        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("mCurrentPhotoPath");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mCurrentPhotoPath", mCurrentPhotoPath);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report_image, container, false);
        final Button takePictureBtn = (Button) view.findViewById(R.id.take_picture_btn);
        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ie) {
                    Log.e(TAG, "can't create file", ie);
                }
                if (photoFile != null) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                }
            }
        });

        final Button choosePictureBtn = (Button) view.findViewById(R.id.choose_picture_btn);
        choosePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, CHOOSE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });

        gridView = (GridView) view.findViewById(R.id.image_grid_view);
        allImage = reportDataSource.getAllImage(reportId);
        imageAdapter = new ImageAdapter(getActivity(), allImage);
        gridView.setAdapter(imageAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ReportImage ri = (ReportImage) imageAdapter.getItem(position);

                /* use android default viewer
                Uri uri = Uri.parse(ri.getImageUri());
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(uri.getPath())), "image/png");
                startActivity(intent);
                */

                Intent intent = new Intent(getActivity(), ImageActivity.class);
                intent.putExtra("imagePath", ri.getImageUri());
                startActivity(intent);
            }
        });

        navigationInterface.setPrevVisible(false);
        navigationInterface.setNextEnable(true);

        return view;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        navigationInterface = (ReportNavigationInterface) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationInterface = null;

        for (ReportImage ri : allImage) {
            ri.getThumbnail().recycle();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "result code = " + resultCode + ", request code = " + requestCode + " data = " + data);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent
                Log.d(TAG, "Image saved to:\n" + mCurrentPhotoPath);
                saveImage(Uri.parse(mCurrentPhotoPath));

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User cancelled the image capture
                Log.d(TAG, "result = canceled");
            } else {
                // Image capture failed, advise user
                Log.d(TAG, "result = fail");
            }
        } else if (requestCode == CHOOSE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "image uri = " + data.getData());
                Uri selectedImage = data.getData();
                saveImage(selectedImage);
            }
        }
    }

    private void saveImage(Uri uri) {
        Bitmap thumbnailBitmap = createThumbnail(uri);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        ReportImage reportImage = reportDataSource.saveImage(reportId, mCurrentPhotoPath, stream.toByteArray());
        try {
            stream.close();
            thumbnailBitmap.recycle();
        } catch (IOException e) {
            Log.e(TAG, "error when closing stream", e);
        }
        allImage.add(reportImage);
        imageAdapter.notifyDataSetChanged();
    }


    protected Bitmap createThumbnail(Uri uri) {
        Log.d(TAG, "image uri = " + uri.toString());
        String selectedImagePath = null;

        Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            selectedImagePath = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            selectedImagePath = cursor.getString(idx);
        }

        Bitmap thumb1 =  ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(selectedImagePath), 128, 128);

        int rotate = neededRotation(new File(selectedImagePath));
        Bitmap thumb2 = null;
        if (rotate != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotate);
            thumb2 = thumb1.createBitmap(thumb1, 0, 0, 128, 128, m, true);
            thumb1.recycle();
        } else {
            thumb2 = thumb1;
        }

        return thumb2;
    }

    public static int neededRotation(File ff) {
        try {

            ExifInterface exif = new ExifInterface(ff.getAbsolutePath());
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

    public class ImageAdapter extends BaseAdapter {

        private final List<ReportImage> images;
        private Context context;

        public ImageAdapter(Context context, List<ReportImage> images) {
            this.context = context;
            this.images = images;
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public Object getItem(int i) {
            return images.get(i);
        }

        @Override
        public long getItemId(int i) {
            return images.get(i).getId();
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            if (view == null) {
                view = newView(parent);
            }
            bindView(i, view);
            return view;
        }

        private void bindView(int i, View view) {
            ImageView imageView = (ImageView) view;
            imageView.setImageBitmap(images.get(i).getThumbnail());

        }

        private View newView(ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            ImageView v = (ImageView) inflater.inflate(R.layout.image_item, parent, false);
            return v;
        }
    }
}
