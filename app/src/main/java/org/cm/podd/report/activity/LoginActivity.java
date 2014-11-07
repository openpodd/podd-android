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
import org.cm.podd.report.util.SharedPrefUtil;

import java.util.Timer;
import java.util.TimerTask;

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

    public class LoginTask extends AsyncTask<Void, Void, Boolean> {

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
        protected Boolean doInBackground(Void... params) {
            // TODO authenticate and get access token
            String token = "token-xxxxx";

            Editor editor = sharedPrefs.edit();
            editor.putString(SharedPrefUtil.ACCESS_TOKEN_KEY, token);
            editor.putString(SharedPrefUtil.USERNAME, usernameText.getText().toString());
            editor.commit();

            isUserLoggedIn = SharedPrefUtil.isUserLoggedIn();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (pd != null && pd.isShowing()) {
                pd.dismiss();
            }

            if (success) {
                // goto home
                finish();
            } else {
                // alert error
            }
        }
    }
}
