package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static android.content.SharedPreferences.Editor;
import static android.provider.Settings.Secure.ANDROID_ID;

public class LoginActivity extends ActionBarActivity {

    private boolean isUserLoggedIn;
    SharedPrefUtil sharedPrefUtil;

    EditText usernameText;
    EditText passwordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_login);

        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();

        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);

        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticate();
            }
        });

        passwordText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    authenticate();
                }
                return false;
            }
        });

        findViewById(R.id.registration).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.forgot_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void authenticate() {
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        if (username.length() > 0 && password.length() > 0) {
            if (RequestDataUtil.hasNetworkConnection(this)) {
                new LoginTask().execute((Void[]) null);
            }
        } else {
            if (username.length() == 0) {
                Crouton.makeText(LoginActivity.this, getString(R.string.username_reqired), Style.ALERT)
                        .setConfiguration(new Configuration.Builder().setDuration(2000).build()).show();
                return;
            }
            if (password.length() == 0) {
                Crouton.makeText(LoginActivity.this, getString(R.string.password_reqired), Style.ALERT)
                        .setConfiguration(new Configuration.Builder().setDuration(2000).build()).show();
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isUserLoggedIn) {
            // back to home
            super.onBackPressed();
        }
    }

    ProgressDialog pd;

    public void showProgressDialog() {
        pd = new ProgressDialog(this);
        pd.setTitle("กำลังส่งข้อมูล");
        pd.setMessage("กรุณารอสักครู่");
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }

    public void hideProgressDialog() {
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideProgressDialog();
    }

    /**
     * Post login
     */
    public class LoginTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
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
            return RequestDataUtil.post("/api-token-auth/", null, reqData, null);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            JSONObject obj = resp.getJsonObject();
            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    String token = obj.getString("token");

                    sharedPrefUtil.setAccessToken(token);
                    sharedPrefUtil.setUserName(usernameText.getText().toString());

                    // get configuration
                    new ConfigTask().execute((Void[]) null);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                // alert error
                hideProgressDialog();

                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(LoginActivity.this, "Error on Server, please contact administration", Style.ALERT)
                            .setConfiguration(new Configuration.Builder().setDuration(2000).build()).show();
                } else {
                    Crouton.makeText(LoginActivity.this, getString(R.string.login_error), Style.ALERT)
                            .setConfiguration(new Configuration.Builder().setDuration(2000).build()).show();
                }

            }
        }
    }

    /**
     * Get preference configuration
     */
    public class ConfigTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            // authenticate and get access token
            String reqData = getIdentifier().toString();
            return RequestDataUtil.post("/configuration/", null, reqData, sharedPrefUtil.getAccessToken());
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);

            JSONObject obj = resp.getJsonObject();

            if (obj == null)
                return;

            try {

                sharedPrefUtil.setUserInfo(obj.getString("fullName"), obj.getJSONArray("administrationAreas").toString());
                sharedPrefUtil.setAWSKey(obj.getString("awsSecretKey"), obj.getString("awsAccessKey"));

                // save report types data into table
                ReportTypeDataSource dataSource = new ReportTypeDataSource(LoginActivity.this);
                dataSource.initNewData(obj.getJSONArray("reportTypes").toString());

                AdministrationAreaDataSource administrationAreaDataSource = new AdministrationAreaDataSource(LoginActivity.this);
                administrationAreaDataSource.initNewData(obj.getJSONArray("administrationAreas").toString());

                isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();
                // goto report home
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
                hideProgressDialog();

                Crouton.makeText(LoginActivity.this, "Error on Server, please contact administration", Style.ALERT).show();
            }
        }
    }

    private JSONObject getIdentifier() {
        Context context = this.getBaseContext();
        JSONObject data = new JSONObject();
        try {
            data.put("wifiMac", ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress());
            data.put("androidId", Settings.Secure.getString(context.getContentResolver(), ANDROID_ID));
            data.put("brand", Build.BRAND);
            data.put("model", Build.MODEL);
            data.put("deviceId", ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

}
