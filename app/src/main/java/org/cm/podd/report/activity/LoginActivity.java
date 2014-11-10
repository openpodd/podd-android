package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import org.cm.podd.report.R;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static android.content.SharedPreferences.Editor;
import static android.provider.Settings.Secure.ANDROID_ID;

public class LoginActivity extends ActionBarActivity {

    private boolean isUserLoggedIn;
    SharedPreferences sharedPrefs;

    EditText usernameText;
    EditText passwordText;

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

        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameText.getText().toString();
                String password = passwordText.getText().toString();
                if (username.length() > 0 && password.length() > 0) {
                    new LoginTask().execute((Void[]) null);
                } else {
                    if (username.length() == 0) {
                        Crouton.makeText(LoginActivity.this, "Required username", Style.ALERT).show();
                        return;
                    }
                    if (password.length() == 0) {
                        Crouton.makeText(LoginActivity.this, "Required password", Style.ALERT).show();
                        return;
                    }
                }
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
    public class LoginTask extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
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
            if (resp != null) {
                try {
                    String token = resp.getString("token");

                    Editor editor = sharedPrefs.edit();
                    editor.putString(SharedPrefUtil.ACCESS_TOKEN_KEY, token);
                    editor.putString(SharedPrefUtil.USERNAME, usernameText.getText().toString());
                    editor.commit();

                    // get configuration
                    new ConfigTask().execute((Void[]) null);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                // alert error
            }
        }
    }

    /**
     * Get preference configuration
     */
    public class ConfigTask extends AsyncTask<Void, Void, JSONObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            // authenticate and get access token
            String reqData = getIdentifier().toString();
            return RequestDataUtil.post("/configuration", null, reqData);
        }

        @Override
        protected void onPostExecute(JSONObject resp) {
            super.onPostExecute(resp);
            hideProgressDialog();
            if (resp == null)
                return;

            try {
                Editor editor = sharedPrefs.edit();
                editor.putString(SharedPrefUtil.FULLNAME, resp.getString("fullName"));
                editor.putString(SharedPrefUtil.AWS_SECRET_KEY, resp.getString("awsSecretKey"));
                editor.putString(SharedPrefUtil.AWS_ACCESS_KEY, resp.getString("awsAccessKey"));
                editor.putString(SharedPrefUtil.ADMIN_AREA, resp.getJSONArray("administrationAreas").toString());
                editor.commit();

                // save report types data into table


                isUserLoggedIn = SharedPrefUtil.isUserLoggedIn();
                // goto report home
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
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
