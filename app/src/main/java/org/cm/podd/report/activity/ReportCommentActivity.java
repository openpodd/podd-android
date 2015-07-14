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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.User;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.CacheUtil;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
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
    private LinearLayout submitCommentText;
    private RelativeLayout emptyUserLayout;
    private ListView listCommentView;
    private ListView listUserView;
    private ProgressBar progressBar;
    private TextView commentEmptyText;
    private TextView userEmptyText;

    private CommentAdapter commentAdapter;
    private UserAdapter userAdapter;

    private int mentionStart = -1;
    private int mentionEnd = -1;
    private boolean mention = false;

    private String cacheKeyName = "mention-keyword-";

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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

        listCommentView = (ListView) findViewById(R.id.list_comment);

        listUserView = (ListView) findViewById(R.id.list_user);
        userEmptyText = (TextView) findViewById(R.id.user_empty);
        userEmptyText.setTypeface(StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL));
        userEmptyText.setVisibility(View.INVISIBLE);
        listUserView.setEmptyView(userEmptyText);

        emptyUserLayout = (RelativeLayout) findViewById(R.id.empty_user_layout);

        progressBar = (ProgressBar) findViewById(R.id.loading_spinner);

//        linearLayout = (LinearLayout) findViewById(R.id.list);
        commentEmptyText = (TextView) findViewById(R.id.comment_empty);

        commentText = (EditText) findViewById(R.id.editText);
        commentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentText.setFocusableInTouchMode(true);
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(commentText, InputMethodManager.SHOW_IMPLICIT);
                listCommentView.setSelection(listCommentView.getAdapter().getCount() - 1);
            }
        });
        commentText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hideProgressBar();
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
                    fetchComments(reportId);
                    showProgressBar();
                    commentText.setEnabled(false);
                    submitCommentText.setEnabled(false);
                }
            }
        });

        Intent intent = getIntent();
        reportId = intent.getLongExtra("reportId", -99);
        commentDataSource = new CommentDataSource(this);

        registerReceiver(mSyncReceiver, new IntentFilter(CommentService.SYNC));

        if (RequestDataUtil.hasNetworkConnection(this)) {
            if(getAll().size() == 0)
                showProgressBar();

            startSyncCommentService(reportId);
        }else {
            refreshComment();
        }

        Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("ReportComment");
        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    private void fetchMentions(CharSequence s, int start, int before, int count){
        String query = "";
        String messages[] = s.toString().trim().split(" ");
        if (s.toString().length() - 1 > 0 && s.toString().charAt(s.toString().length() - 1) == ' ') {
            mention = false;
        } else {
            int messageLen = 0;
            for (int i = 0; i < messages.length; i++) {
                String message = messages[i];
                messageLen += message.length();

                if (start <= messageLen) {
                    if (message.length() > 0 && message.charAt(0) == '@') {
                        query = message.replace("@", "");
                        mentionStart = messageLen - message.length();
                        mentionEnd = messageLen;
                        mention = true;
                        break;
                    }
                    mention = false;
                }
                messageLen +=  1;

                if (i == messages.length - 1)
                    mention = false;
            }
        }

        if (mention) {
            showProgressBar();
            String key = cacheKeyName + query;
            try {
                byte [] data = CacheUtil.retrieveData(ReportCommentActivity.this, key);
                if (data != null) {
                    String jsonMentionsData = new String(data, "UTF-8");
                    showMentionList(jsonMentionsData);
                } else {
                    if (RequestDataUtil.hasNetworkConnection(ReportCommentActivity.this)) {
                        new SyncUserTask().execute(new String[]{query});
                    }else{
                        hideProgressBar();
                    }
                }
                listUserView.setVisibility(View.VISIBLE);
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
        }else{
            listUserView.setVisibility(View.INVISIBLE);
            emptyUserLayout.setVisibility(View.INVISIBLE);
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
                        Crouton.makeText(ReportCommentActivity.this, getString(R.string.server_error), Style.ALERT).show();
                    } else {
                        Crouton.makeText(ReportCommentActivity.this, getString(R.string.comment_error), Style.ALERT).show();
                    }
                    hideProgressBar();
                }
                commentText.setEnabled(true);
                submitCommentText.setEnabled(true);
            }
        };
        String message = commentText.getText().toString();
        message = message.replaceAll(getString(R.string.comment_mention_regex), getString(R.string.comment_mention_render));

        task.setContext(getApplicationContext());
        task.execute((new String[]{reportId.toString(), message}));
    }

    public void refreshComment() {
        ArrayList<Comment> comments = getAll();

        commentAdapter = new CommentAdapter(this, R.layout.list_item_comment, comments);
        listCommentView.setAdapter(commentAdapter);

        listCommentView.setSelection(listCommentView.getAdapter().getCount() - 1);
        listCommentView.setEmptyView(commentEmptyText);

        hideProgressBar();
    }

    public void addComment() {
        refreshComment();
        commentText.setText("");

        if (RequestDataUtil.hasNetworkConnection(this)) {
            startSyncCommentService(reportId);
        }
    }

    private void hideProgressBar(){
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void showProgressBar(){
        commentEmptyText.setVisibility(View.INVISIBLE);
        emptyUserLayout.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public class SyncUserTask extends AsyncTask<String, Void, RequestDataUtil.ResponseObject> {
        String query;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            query = params[0];

            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
            return RequestDataUtil.get("/users/search/?username=" + query, null, sharedPrefUtil.getAccessToken());
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            hideProgressBar();
            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    String key = cacheKeyName + query;
                    showMentionList(resp.getRawData());
                    CacheUtil.cacheData(ReportCommentActivity.this, resp.getRawData().getBytes(Charset.forName("UTF-8")), key);

                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private class CommentAdapter extends ArrayAdapter<Comment> {

        Context context;
        int resource;
        Typeface face;

        public CommentAdapter(Context context, int resource, List<Comment> originalData) {
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

            String message = getItem(position).getMessage();
            message = message.replaceAll(getString(R.string.mention_regex), getString(R.string.mention_render));

            TextView messageTextView = (TextView) view.findViewById(R.id.message);
            messageTextView.setTypeface(face);
            messageTextView.setText(Html.fromHtml(message), TextView.BufferType.SPANNABLE);

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
            createdAtTextView.setText(getString(R.string.comment_date) + " " + DateUtil.convertToThaiDateTime(date));

            CircleImageView avatarCreatedByView = (CircleImageView) view.findViewById(R.id.profile_image);

            if (!getItem(position).getAvatarCreatedBy().equals("null")) {
                Picasso.with(getContext()).load(getItem(position).getAvatarCreatedBy()).into(avatarCreatedByView);
            }
            return view;
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

            if (getItem(position).getFullName().equals(""))
                textFullNameView.setText(getItem(position).getUsername());
            else
                textFullNameView.setText(getItem(position).getFullName());

            textUserNameView.setText(getItem(position).getUsername());

            FontUtil.overrideFonts(getContext(), view);
            return view;
        }
    }

    private void showMentionList(String jsonUsers){
        try {
            final ArrayList<User> users = new ArrayList<User>();
            JSONArray items = new JSONArray(jsonUsers);

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                Long id = item.optLong("id", -99);
                String userName = item.optString("username");
                String fullName = item.optString("fullName");
                User user = new User(id, userName, fullName);
                users.add(user);
            }

            userAdapter = new UserAdapter(ReportCommentActivity.this, R.layout.list_item_user, users);
            listUserView.setAdapter(userAdapter);
            listUserView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String mention = users.get(position).getUsername();
                    String message = commentText.getText().toString();
                    String lastMessage = "";
                    if (mentionEnd + 1 < message.length() - 1)
                        lastMessage = message.substring(mentionEnd + 1);
                    message = message.substring(0, mentionStart) + "@" + mention + " " + lastMessage;
                    commentText.setText(message, TextView.BufferType.EDITABLE);

                    int positionText = commentText.getText().length();
                    commentText.setSelection(positionText);
                    listUserView.setVisibility(View.INVISIBLE);
                }
            });
            emptyUserLayout.setVisibility(View.VISIBLE);
            hideProgressBar();
        } catch (JSONException e) {
            e.printStackTrace();
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
//        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
//        actionBar.setHomeAsUpIndicator(0);
//        actionBar.setHomeButtonEnabled(true);
//        actionBar.setLogo(R.drawable.arrow_left_with_pad);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

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
