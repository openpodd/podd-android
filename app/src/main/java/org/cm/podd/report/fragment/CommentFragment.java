package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.VisualizationAreaActivity;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.AdministrationAreaService;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.SyncReportTypeService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentFragment extends ListFragment {

    private static final String TAG = "CommentFragment";

    CommentDataSource commentDataSource;
    private commentAdapter adapter;
    private long reportId;

    public CommentFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Comment> items = getAll();
            adapter = new commentAdapter(context, R.layout.list_item_comment, items);
            setListAdapter(adapter);
        }
    };

    private ArrayList<Comment> getAll() {
        ArrayList<Comment> items = new ArrayList<Comment>();
        items.addAll(commentDataSource.getAllFromReport(reportId));
        return items;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportId = getArguments().getLong("reportId", -99);
        commentDataSource = new CommentDataSource(getActivity());

        getActivity().registerReceiver(mSyncReceiver, new IntentFilter(CommentService.SYNC));

        if (RequestDataUtil.hasNetworkConnection(getActivity())) {
            startSyncCommentService(reportId);
        }
    }

    public void refreshAdapter() {
        adapter = new commentAdapter(getActivity(), R.layout.list_item_comment, commentDataSource.getAllFromReport(reportId));
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mSyncReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comment, container, false);
        final ListView listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบความคิดเห็น");
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);
        parent.addView(emptyText);

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                listView.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        return view;
    }

    /**
     * List Adapter
     */
    private class commentAdapter extends ArrayAdapter<Comment> {

        Context context;
        int resource;
        Typeface face;

        public commentAdapter(Context context, int resource, List<Comment> originalData) {
            super(context, resource, originalData);
            this.context = context;
            this.resource = resource;
            face = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(this.resource, parent, false);

            TextView createdByTextView = (TextView) view.findViewById(R.id.name);
            createdByTextView.setTypeface(face, Typeface.BOLD);
            createdByTextView.setText(getItem(position).getCreatedBy());

            TextView messageTextView = (TextView) view.findViewById(R.id.message);
            messageTextView.setTypeface(face);
            messageTextView.setText(getItem(position).getMessage());

            Date date = null;
            String dateText = getItem(position).getCreatedAt();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
            try {
                date = format.parse(dateText);
            } catch (ParseException e) {
                Log.e(TAG, e.toString());
            }

            TextView createdAtTextView = (TextView) view.findViewById(R.id.date);
            createdAtTextView.setTypeface(face);
            createdAtTextView.setText(DateUtil.convertToThaiDateTime(date));

            CircleImageView avatarCreatedByView = (CircleImageView) view.findViewById(R.id.profile_image);

            if(!getItem(position).getAvatarCreatedBy().equals(null)){
                new ImageDownloader(avatarCreatedByView).execute(getItem(position).getAvatarCreatedBy());
            }
            return view;
        }
    }

    class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public ImageDownloader(ImageView imageView) {
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap mImage = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                mImage = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
            }
            return mImage;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    private void startSyncCommentService(long reportId) {
        Intent intent = new Intent(getActivity(), CommentService.class);
        intent.putExtra("reportId", reportId);
        getActivity().startService(intent);
    }


}
