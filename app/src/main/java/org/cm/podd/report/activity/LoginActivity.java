package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import org.cm.podd.report.R;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.SharedPreferences.Editor;

public class LoginActivity extends ActionBarActivity {

    private boolean isUserLoggedIn;
    SharedPreferences sharedPrefs;

    EditText usernameText;
    EditText passwordText;
    View progressMask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_login);

        sharedPrefs = SharedPrefUtil.getPrefs(getApplicationContext());
        isUserLoggedIn = SharedPrefUtil.isUserLoggedIn();

        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);
        progressMask = findViewById(R.id.progress_mask);

        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LoginTask().execute((Void[]) null);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isUserLoggedIn) {
            // back to home
            super.onBackPressed();
        }
    }

    public class LoginTask extends AsyncTask<Void, Void, JSONObject> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(LoginActivity.this);
            pd.setTitle("กำลังส่งข้อมูล");
            pd.setMessage("กรุณารอสักครู่");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            // authenticate and get access token
            String reqData = null;
            try {
                JSONObject json = new JSONObject();
                json.put("username", usernameText.getText().toString());
                json.put("password", passwordText.getText().toString());
                reqData = json.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return RequestDataUtil.post("/api-token-auth", null, reqData);
        }

        @Override
        protected void onPostExecute(JSONObject resp) {
            super.onPostExecute(resp);
            if (pd != null && pd.isShowing()) {
                pd.dismiss();
            }

            if (resp != null) {
                try {
                    String token = resp.getString("token");

                    Editor editor = sharedPrefs.edit();
                    editor.putString(SharedPrefUtil.ACCESS_TOKEN_KEY, token);
                    editor.putString(SharedPrefUtil.USERNAME, usernameText.getText().toString());
                    editor.commit();

                    isUserLoggedIn = SharedPrefUtil.isUserLoggedIn();
                    // goto home
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                // alert error
            }
        }
    }
}
