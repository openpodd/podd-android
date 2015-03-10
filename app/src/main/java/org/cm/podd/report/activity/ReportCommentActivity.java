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
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.User;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ReportCommentActivity extends ActionBarActivity {
    private String TAG = "CommentActivity";
    private Long reportId;

    private CommentDataSource commentDataSource;

    private EditText commentText;
    private LinearLayout linearLayout;
    private LinearLayout submitCommentText;
    private ListView listView;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private TextView emptyText;

    private UserAdapter adapter;

    private int queryStart = -1;
    private int queryEnd = -1;
    private boolean mention = false;

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
        listView = (ListView) findViewById(R.id.list_view);
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
        commentText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fetchMentions(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        submitCommentText = (LinearLayout) findViewById(R.id.submit);
        submitCommentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!commentText.getText().toString().equals(null) || !commentText.getText().toString().equals("")){
                    showProgressBar();
                    fetchComments(reportId);
                }
            }
        });

        Intent intent = getIntent();
        reportId = intent.getLongExtra("reportId", -99);
        commentDataSource = new CommentDataSource(this);

        registerReceiver(mSyncReceiver, new IntentFilter(CommentService.SYNC));

        runOnUiThread(new Runnable() {
            public void run() {
                showProgressBar();
                refreshComment();
            }
        });

        if (RequestDataUtil.hasNetworkConnection(this)) {
            if(getAll().size() == 0)
                showProgressBar();

            startSyncCommentService(reportId);
        }


    }

    private void fetchMentions(CharSequence s, int start, int before, int count){
        if (s.length() > 0){
            if (mention){
                queryEnd = s.length() - 1;
                if (RequestDataUtil.hasNetworkConnection(ReportCommentActivity.this)) {
                    String query = commentText.toString().substring(queryStart, queryEnd).replace("@", "");
                    new SyncUserTask().execute(new String[]{query});
                    listView.setVisibility(View.VISIBLE);
                }
            }

            if (s.charAt(s.length()-1) == '@'){
                mention = true;
                queryStart = s.length() - 1;
                listView.setVisibility(View.VISIBLE);
            }else if(s.charAt(s.length()-1) == ' '){
                mention = false;
                queryStart = -1;
                queryEnd = -1;
                listView.setVisibility(View.INVISIBLE);
            }

        }
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
                    hideProgressBar();
                }
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

            String message = comments.get(i).getMessage();
            message = message.replaceAll(getString(R.string.mention_regex), getString(R.string.mention_render));

            TextView messageTextView = (TextView) view.findViewById(R.id.message);
            messageTextView.setTypeface(face);
            messageTextView.setText(Html.fromHtml(message), TextView.BufferType.SPANNABLE);

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

        commentText.setText("");

        if (RequestDataUtil.hasNetworkConnection(this)) {
            startSyncCommentService(reportId);
        }
    }

    private void hideProgressBar(){
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void showProgressBar(){
        emptyText.setVisibility(View.INVISIBLE);
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

    public class SyncUserTask extends AsyncTask<String[], Void, RequestDataUtil.ResponseObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String[]... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
            return RequestDataUtil.get("/users/search/?username=" + params[0], null, sharedPrefUtil.getAccessToken());
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    ArrayList<User> users = new ArrayList<User>();
                    JSONArray items = new JSONArray(resp.getRawData());

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        Long id = item.optLong("id", -99);
                        String userName = item.optString("username");
                        String fullName = item.optString("fullName");
                        User user = new User(id, userName, fullName);
                        users.add(user);
                    }

                    adapter = new UserAdapter(ReportCommentActivity.this, R.layout.list_item_user, users);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private class UserAdapter extends ArrayAdapter<User> {

        Context context;
        int resource;

        public UserAdapter(Context context, int resource, List<User> users) {
            super(context, resource, users);
            this.context = context;
            this.resource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(this.resource, parent, false);

            TextView textFullNameView = (TextView) view.findViewById(R.id.full_name);
            TextView textUserNameView = (TextView) view.findViewById(R.id.username);


            textFullNameView.setText(getItem(position).getFullName());
            textUserNameView.setText(getItem(position).getUsername());

            FontUtil.overrideFonts(getContext(), view);
            return view;
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
