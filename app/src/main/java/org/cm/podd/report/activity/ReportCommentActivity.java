package org.cm.podd.report.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ReportCommentActivity extends ActionBarActivity {
    private String TAG = "CommentActivity";
    private Long reportId;

    private CommentDataSource commentDataSource;
    private LinearLayout linearLayout;
    private TextView emptyText;
    private EditText commentText;
    private LinearLayout submitCommentText;
    private ScrollView scrollView;
    private ProgressBar progressBar;

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Comment> items = getAll();
            refreshComment();
        }
    };

    private ArrayList<Comment> getAll() {
        ArrayList<Comment> items = new ArrayList<Comment>();
        items.addAll(commentDataSource.getAllFromReport(reportId));
        return items;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_comment);
        FontUtil.overrideFonts(this, getWindow().getDecorView());

        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        progressBar = (ProgressBar) findViewById(R.id.loading_spinner);

        linearLayout = (LinearLayout) findViewById(R.id.list);
        emptyText = (TextView) findViewById(android.R.id.empty);

        commentText = (EditText) findViewById(R.id.editText);
        commentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentText.setFocusableInTouchMode(true);
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(commentText, InputMethodManager.SHOW_IMPLICIT);
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });

        submitCommentText = (LinearLayout) findViewById(R.id.submit);
        submitCommentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!commentText.getText().toString().equals(null) || !commentText.getText().toString().equals(""))
                    fetchComments(reportId);
            }
        });

        Intent intent = getIntent();
        reportId = intent.getLongExtra("reportId", -99);
        commentDataSource = new CommentDataSource(this);

        registerReceiver(mSyncReceiver, new IntentFilter(CommentService.SYNC));

        if (RequestDataUtil.hasNetworkConnection(this)) {
            startSyncCommentService(reportId);
        }

        runOnUiThread(new Runnable() {
            public void run() {
                showProgressBar();
                refreshComment();
            }
        });
    }

    private void fetchComments(final Long reportId) {
        ReportService.PostCommentTask task = new ReportService.PostCommentTask(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                super.onPostExecute(resp);
                JSONObject obj = resp.getJsonObject();
                if (resp.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                    addComment();
                } else {
                    Log.e(TAG, resp.getStatusCode() + "" + resp.getJsonObject());

                    if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                        Crouton.makeText(ReportCommentActivity.this, "Error on Server, please contact administration", Style.ALERT).show();
                    } else {
                        Crouton.makeText(ReportCommentActivity.this, getString(R.string.comment_error), Style.ALERT).show();
                    }
                }
                commentText.setText("");
            }
        };
        task.setContext(getApplicationContext());
        task.execute((new String[]{reportId.toString(), commentText.getText().toString()}));
    }

    public void refreshComment() {
        linearLayout.removeAllViews();

        ArrayList<Comment> comments = getAll();

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบความคิดเห็น");

        if (comments.size() == 0){
            emptyText.setVisibility(View.VISIBLE);
            hideProgressBar();
            return;
        }

        Typeface face = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);

        for (int i = 0; i < comments.size(); i++){
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.list_item_comment, null, false);

            TextView createdByTextView = (TextView) view.findViewById(R.id.name);
            createdByTextView.setTypeface(face, Typeface.BOLD);
            createdByTextView.setText(comments.get(i).getCreatedBy());

            TextView messageTextView = (TextView) view.findViewById(R.id.message);
            messageTextView.setTypeface(face);
            messageTextView.setText(comments.get(i).getMessage());

            Date date = null;
            String dateText = comments.get(i).getCreatedAt();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
            try {
                date = format.parse(dateText);
            } catch (ParseException e) {
                Log.e(TAG, e.toString());
            }

            TextView createdAtTextView = (TextView) view.findViewById(R.id.date);
            createdAtTextView.setTypeface(face);
            createdAtTextView.setText(getString(R.string.comment_date) + " " + DateUtil.convertToThaiDateTime(date));

            CircleImageView avatarCreatedByView = (CircleImageView) view.findViewById(R.id.profile_image);

            if (!comments.get(i).getAvatarCreatedBy().equals(null)){
                new ImageDownloader(avatarCreatedByView).execute(comments.get(i).getAvatarCreatedBy());
            }

            if (i == comments.size() -1){
                LinearLayout line = (LinearLayout) view.findViewById(R.id.line);
                line.setVisibility(View.GONE);
            }
            linearLayout.addView(view);
        }

        emptyText.setVisibility(View.GONE);
        hideProgressBar();
    }

    public void addComment() {
        ArrayList<Comment> comments = getAll();

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบความคิดเห็น");

        if (comments.size() == 0){
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        Typeface face = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);

        int i = comments.size() - 1;
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.list_item_comment, null, false);

        TextView createdByTextView = (TextView) view.findViewById(R.id.name);
        createdByTextView.setTypeface(face, Typeface.BOLD);
        createdByTextView.setText(comments.get(i).getCreatedBy());

        TextView messageTextView = (TextView) view.findViewById(R.id.message);
        messageTextView.setTypeface(face);
        messageTextView.setText(comments.get(i).getMessage());

        Date date = null;
        String dateText = comments.get(i).getCreatedAt();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
        try {
            date = format.parse(dateText);
        } catch (ParseException e) {
            Log.e(TAG, e.toString());
        }

        TextView createdAtTextView = (TextView) view.findViewById(R.id.date);
        createdAtTextView.setTypeface(face);
        createdAtTextView.setText(getString(R.string.comment_date) + " " + DateUtil.convertToThaiDateTime(date));

        CircleImageView avatarCreatedByView = (CircleImageView) view.findViewById(R.id.profile_image);

        if (!comments.get(i).getAvatarCreatedBy().equals(null)){
            new ImageDownloader(avatarCreatedByView).execute(comments.get(i).getAvatarCreatedBy());
        }

        if (i == comments.size() -1){
            LinearLayout line = (LinearLayout) view.findViewById(R.id.line);
            line.setVisibility(View.GONE);
        }
        linearLayout.addView(view);
        emptyText.setVisibility(View.GONE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        }, 1000);

        if (RequestDataUtil.hasNetworkConnection(this)) {
            startSyncCommentService(reportId);
        }
    }

    private void hideProgressBar(){
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void showProgressBar(){
        progressBar.setVisibility(View.VISIBLE);
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
        Intent intent = new Intent(this, CommentService.class);
        intent.putExtra("reportId", reportId);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_comment));
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);

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
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
