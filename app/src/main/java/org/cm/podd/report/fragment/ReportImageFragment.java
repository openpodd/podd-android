package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.activity.ImageActivity;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.model.ReportImage;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.util.StyleUtil;
import org.cm.podd.report.view.TouchyGridView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.cm.podd.report.fragment.ReportDataInterface.CameraInteractionListener;

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
    private static final int MAX_IMAGE_GUIDE = 4;

    private long reportId;

    private ReportNavigationInterface navigationInterface;
    private ReportDataInterface dataInterface;
    private ReportDataSource reportDataSource;
    private ReportQueueDataSource reportQueueDataSource;

    private GridView gridView;
    private ImageAdapter imageAdapter;
    private List<ReportImage> allImage;

    private GridView gridImageGuideView;
    private ImageAdapter imageGuideAdapter;
    private List<ReportImage> allImageGuide;

    long mReportImageId;
    String mCurrentPhotoPath;
    String mImageNote;

    private ActionMode mMode;

    private File createImageFile() throws IOException {
        // Create an image file name
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
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        // file:///storage/sdcard/Pictures/xxx.jpg
        mCurrentPhotoPath = Uri.fromFile(image).toString();

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
        reportQueueDataSource = new ReportQueueDataSource(this.getActivity());

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
        gridView = (GridView) view.findViewById(R.id.image_grid_view);
        allImage = reportDataSource.getAllImage(reportId);

        /* image guide grid behind real image grid */
        gridImageGuideView = (GridView) view.findViewById(R.id.image_place_holder);
        allImageGuide = getAllImageGuide();
        imageGuideAdapter = new ImageAdapter(getActivity(), allImageGuide);
        gridImageGuideView.setAdapter(imageGuideAdapter);
        gridImageGuideView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mMode == null) {
                    popUpChooseImage();
                }
            }
        });

        imageAdapter = new ImageAdapter(getActivity(), allImage);
        imageAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                // hide image guide grid so when user scroll down, the guide below images can not be seen
                gridImageGuideView.setVisibility(allImage.size() > MAX_IMAGE_GUIDE ?
                        View.INVISIBLE : View.VISIBLE);
                super.onChanged();
            }
        });
        gridView.setAdapter(imageAdapter);
        gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (mMode == null) {
                    // only in ActionMode to do a check
                    gridView.setItemChecked(position, false);

                    // View full image if actionMode is not active
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
                } else {
                    // multiple select other images when actionMode is active
                    SparseBooleanArray checked = gridView.getCheckedItemPositions();

                    if (checked != null) {
                        boolean hasCheckedElement = false;
                        for (int i = 0 ; i < checked.size() && ! hasCheckedElement ; i++) {
                            hasCheckedElement = checked.valueAt(i);
                        }

                        if (hasCheckedElement) {
                            mMode.invalidate();
                        } else {
                            // until nothing is selected then deactivate actionMode
                            mMode.finish();
                        }
                    }
                }
            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // use ActionMode.Callback for compatibility with pre Honeycomb
                if (mMode == null) {
                    mMode = ((ActionBarActivity)getActivity()).startSupportActionMode(
                            new ActionModeCallback(getActivity()));
                }
                return false;
            }
        });
        ((TouchyGridView) gridView).setOnNoItemClickListener(new TouchyGridView.OnNoItemClickListener() {
            @Override
            public void onNoItemClick() {
                if (mMode == null) {
                    if (gridImageGuideView.getVisibility() == View.VISIBLE) {
                        popUpChooseImage();
                    }
                }
            }
        });

        navigationInterface.setPrevVisible(false);
        navigationInterface.setNextEnable(true);

        return view;

    }

    private List<ReportImage> getAllImageGuide() {
        int max = MAX_IMAGE_GUIDE, i = 0;
        List<ReportImage> images = new ArrayList<ReportImage>(max);
        while (i < MAX_IMAGE_GUIDE) {
            ReportImage image = new ReportImage(i, null);
            image.setThumbnail(BitmapFactory.decodeResource(
                    getResources(), R.drawable.gallery_default));
            images.add(image);
            i++;
        }
        return images;
    }

    /**
     * Handle dialog click on media selection
     * @param requestCode
     */
    private void onMediaChoiceRequest(int requestCode) {
        switch (requestCode) {
            case CHOOSE_IMAGE_ACTIVITY_REQUEST_CODE:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, CHOOSE_IMAGE_ACTIVITY_REQUEST_CODE);
                break;

            case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ie) {
                    Log.e(TAG, "can't create file", ie);
                }
                if (photoFile != null) {
                    intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024*1024);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                }
                break;
        }
    }

    private void deleteImages() {
        long[] ids = gridView.getCheckedItemIds();
        for (int i = 0; i < ids.length; i++) {
            ReportImage ri = imageAdapter.getItemById(ids[i]);
            mCurrentPhotoPath = ri.getImageUri();
            mReportImageId = ri.getId();
            mImageNote = ri.getNote();
            mCurrentPhotoPath = ri.getImageUri();

            Log.d(TAG, "Delete image id=" + mReportImageId);
            reportDataSource.deleteImage(mReportImageId);

            // remove image file if it was taken from capture camera, not from media list
            Uri uri = Uri.parse(mCurrentPhotoPath);
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);

            // Uri is not content://path
            if (cursor == null) {
                String filePath = uri.getPath();
                File f = new File(filePath);
                f.delete();
                Log.d(TAG, "Image file removed: path= " + filePath);
            }
            imageAdapter.removeItem(mReportImageId);
        }
        imageAdapter.notifyDataSetChanged();
    }

    private void saveNote() {
        NoteEditDialog dlg2 = NoteEditDialog.newInstance(mImageNote);
        dlg2.setTargetFragment(this, 0);
        dlg2.show(getActivity().getSupportFragmentManager(), "NoteEditDialog");
    }

    private void onNoteSave(String note) {
        Log.d(TAG, "on note save id=" + mReportImageId + " / text=" + note);
        int pos = imageAdapter.getPositionById(mReportImageId);
        ReportImage item = (ReportImage) imageAdapter.getItem(pos);
        item.setNote(note);
        reportDataSource.saveNote(mReportImageId, note);
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        navigationInterface = (ReportNavigationInterface) activity;

        dataInterface = (ReportDataInterface) activity;
        dataInterface.setCameraInteractionListener(new CameraInteractionListener() {
            @Override
            public void doGetImage() {
                popUpChooseImage();
            }
        });
    }

    public void popUpChooseImage() {
        MediaChoiceDialog dlg = new MediaChoiceDialog();
        dlg.setTargetFragment(this, 0);
        dlg.show(getActivity().getSupportFragmentManager(), "MediaChoiceDialog");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationInterface = null;

        for (ReportImage ri : allImage) {
            if (ri.getThumbnail() != null) {
                ri.getThumbnail().recycle();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        reportDataSource.close();
        reportQueueDataSource.close();
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
        ReportImage reportImage = reportDataSource.saveImage(reportId, uri.toString(), stream.toByteArray());
        try {
            stream.close();
            thumbnailBitmap.recycle();
        } catch (IOException e) {
            Log.e(TAG, "error when closing stream", e);
        }
        allImage.add(reportImage);
        imageAdapter.notifyDataSetChanged();

        if (dataInterface.isDoneSubmit()) {
            // if report already submitted before, now only submit images
            // that is not submitted yet and also not in a queue
            reportQueueDataSource.addImageQueue(reportId);

            // Broadcasts the Intent to network receiver, and prepare queue for sending
            Intent networkIntent = new Intent(DataSubmitService.ACTION_REPORT_SUBMIT);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(networkIntent);
        }

        // send event hit
        Tracker tracker = ((PoddApplication) getActivity().getApplication()).getTracker(
                PoddApplication.TrackerName.APP_TRACKER);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("ReportProcess").setAction("Camera")
                .build());
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

        Bitmap thumb1 =  ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(selectedImagePath), 400, 400);

        int rotate = neededRotation(new File(selectedImagePath));
        Bitmap thumb2 = null;
        if (rotate != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotate);
            thumb2 = thumb1.createBitmap(thumb1, 0, 0, 400, 400, m, true);
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
        public boolean hasStableIds() {
            return true;
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
            ViewHolder holder = (ViewHolder) view.getTag();
            ImageView imageView = holder.imageView;

            imageView.setImageBitmap(images.get(i).getThumbnail());
        }

        private View newView(ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.image_item, parent, false);

            ImageCheckableLayout checkable = new ImageCheckableLayout(context);
            checkable.setLayoutParams(new GridView.LayoutParams(
                    GridView.LayoutParams.MATCH_PARENT,
                    GridView.LayoutParams.MATCH_PARENT));
            checkable.addView(v);

            ViewHolder holder = new ViewHolder();
            holder.imageView = (ImageView) v.findViewById(R.id.image_view);
            checkable.setTag(holder);

            return checkable;
        }

        private boolean isLastItem(int pos) {
            return images.get(pos).getId() == 0;
        }

        public void removeItem(long id) {
            int pos = getPositionById(id);
            images.remove(pos);
        }

        public int getPositionById(long id) {
            int pos = 0, i = 0;
            for (ReportImage item : images) {
                if (item.getId() == id) {
                    pos = i;
                }
                i++;
            }
            return pos;
        }

        public ReportImage getItemById(long id) {
            for (ReportImage item : images) {
                if (item.getId() == id) {
                    return item;
                }
            }
            return null;
        }

        class ViewHolder {
            ImageView imageView;
        }
    }

    /**
     * Layout wrapper to make image view checkable
     */
    public class ImageCheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;
        View mask;

        public ImageCheckableLayout(Context context) {
            super(context);
        }

        @SuppressWarnings("deprecation")
        public void setChecked(boolean checked) {
            mChecked = checked;
            mask = findViewById(R.id.selected_mask);
            mask.setVisibility(checked ? VISIBLE : INVISIBLE);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (mask != null) {
                ViewGroup.LayoutParams layoutParams = mask.getLayoutParams();
                layoutParams.height = getMeasuredHeight();
                mask.setLayoutParams(layoutParams);
            }
        }
    }

    /**
     * Dialog for image selection methods
     */
    public static class MediaChoiceDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_pick_media_options)
                    .setItems(R.array.pick_media_selection, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ReportImageFragment fragment = (ReportImageFragment) getTargetFragment();
                            fragment.onMediaChoiceRequest(which == 0 ?
                            CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE : CHOOSE_IMAGE_ACTIVITY_REQUEST_CODE);
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    /**
     * Dialog to input note message for selected picture
     */
    public static class NoteEditDialog extends DialogFragment {
        EditText mEdit;

        public static NoteEditDialog newInstance(String note) {
            NoteEditDialog dlg = new NoteEditDialog();
            Bundle args = new Bundle();
            args.putString("note", note);
            dlg.setArguments(args);
            return  dlg;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String note = getArguments().getString("note");
            mEdit = new EditText(this.getActivity());
            mEdit.setText(note);
            mEdit.setHint(R.string.enter_image_note);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(mEdit);

            builder.setTitle(R.string.title_image_note_edit)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ReportImageFragment fragment = (ReportImageFragment) getTargetFragment();
                            fragment.onNoteSave(String.valueOf(mEdit.getText()));
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }

    /**
     * Callback to CAB (Contextual Action Bar)
     */
    private final class ActionModeCallback implements ActionMode.Callback {
        Context context;
        public ActionModeCallback(Context context) {
            this.context = context;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create the menu from the xml file
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.image_fragment_contextual_actions, menu);

            // use custom text as title
            // on pre honeycomb (<11), cab title background is action bar background (red in drme)
            TextView tv = new TextView(getActivity());
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setText("cab");
            tv.setBackgroundColor(context.getResources().getColor(R.color.action_bar_bg));
            mode.setCustomView(tv);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int selected = gridView.getCheckedItemCount();
            TextView title = ((TextView) mode.getCustomView());
            title.setText(getString(R.string.title_image_item_selected, new Object[]{selected}));
            title.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(TAG, "deselect all items");
            // Destroying action mode, let's unselect all items
            for (int i = 0; i < gridView.getAdapter().getCount(); i++) {
                gridView.setItemChecked(i, false);
            }
            gridView.clearChoices();

            if (mode == mMode) {
                mMode = null;
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.cab_action_delete:
                    deleteImages();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }
    };
}
