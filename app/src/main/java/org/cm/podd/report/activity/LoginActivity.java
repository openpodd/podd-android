package org.cm.podd.report.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.R;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static android.provider.Settings.Secure.ANDROID_ID;

public class LoginActivity extends ActionBarActivity {

    public static final int REQUEST_FOR_QR_CODE_CONFIG = 700;
    private static String TAG = "LoginActivity";

    private boolean isUserLoggedIn;
    private int numOfValidServerUrl;
    private boolean prevValidServerUrlIsLongClick;
    private int numOfLongClick4QRCode = 0;
    SharedPrefUtil sharedPrefUtil;

    EditText usernameText;
    EditText passwordText;
    EditText serverUrlText;

    SharedPreferences settings;
    private ImageView logoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_login);

        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();
        numOfValidServerUrl = 0;
        prevValidServerUrlIsLongClick = false;

        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);
        serverUrlText = (EditText) findViewById(R.id.server_url);

        settings = getSharedPreferences("PoddPrefsFile", 0);

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

        FontUtil.overrideFonts(this, usernameText.getRootView());

        // Config api end point on the fly

        logoView = (ImageView) findViewById(R.id.logo);
        logoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                checkAndShowServerUrlForm(true);
                return true;
            }

        });

        logoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndShowServerUrlForm(false);
            }
        });

        String customLogoPath = sharedPrefUtil.getCustomIconPath();
        if (customLogoPath != null) {
            logoView.setImageDrawable(new BitmapDrawable(getResources(), customLogoPath));
        }

        findViewById(R.id.server_url_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveServerUrl(serverUrlText.getText().toString());
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FOR_QR_CODE_CONFIG && resultCode == 0) {
            String result = data.getStringExtra("result");

            /*
                format of result
                {
                    "server": "https://api.cmonehealth.org",
                    "shortcut": {
                        "icon": "http://lab.bon.co.th/mbds/ic_launcher.png",
                        "text": "MBDS"
                    },
                    "title": "MBDS DDR",
                    "loginLogoIcon": "http://lab.bon.co.th/mbds/logo.png"
                }
             */
            try {
                JSONObject obj = new JSONObject(result);
                if (obj.has("server")) {
                    String server = obj.getString("server");
                    saveServerUrl(server);
                    Crouton.makeText(LoginActivity.this, getString(R.string.change_server_message) + server, Style.CONFIRM)
                            .setConfiguration(new Configuration.Builder().setDuration(2000).build()).show();
                }

                if (obj.has("shortcut")) {
                    JSONObject shortcut = obj.getJSONObject("shortcut");
                    String icon = shortcut.getString("icon");
                    String text = shortcut.getString("text");

                    Crouton.makeText(LoginActivity.this, getString(R.string.create_shortcut_message) + " " + text, Style.CONFIRM)
                            .setConfiguration(new Configuration.Builder().setDuration(2000).build()).show();


                    if (obj.has("title")) {
                        String title = obj.getString("title");
                        sharedPrefUtil.setCustomTitle(title);
                    }

                    new ShortcutTask().execute(icon, text);

                    if (obj.has("loginLogoIcon")) {
                        String loginIcon = obj.getString("loginLogoIcon");
                        new LogoDownloadTask().execute(loginIcon);
                    }

                } else {
                    // clear shortcut
                    sharedPrefUtil.clearCustomIconPath();
                }

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }

        }


    }

    private void checkAndShowServerUrlForm(boolean isLongClick) {

        if (isLongClick && numOfValidServerUrl == 0) {
            numOfLongClick4QRCode = 1;
        } else if (isLongClick && numOfLongClick4QRCode == 1) {
            numOfLongClick4QRCode = 2;
        } else {
            numOfLongClick4QRCode = 0;
        }

        if ((prevValidServerUrlIsLongClick && !isLongClick) || (!prevValidServerUrlIsLongClick && isLongClick)) {
            numOfValidServerUrl++;
        }
        else {
            numOfValidServerUrl = 0;
        }

        if (numOfValidServerUrl >= 6) {
            String serverUrl = settings.getString("serverUrl", BuildConfig.SERVER_URL);
            if (serverUrl == "") {
                serverUrl = BuildConfig.SERVER_URL;
            }
            serverUrlText.setText(serverUrl);

            findViewById(R.id.server_url_form).setVisibility(View.VISIBLE);

            numOfValidServerUrl = 0;
        }


        if (numOfLongClick4QRCode == 2) {
            Intent intent = new Intent(getApplicationContext(), QRConfigActivity.class);
            startActivityForResult(intent, REQUEST_FOR_QR_CODE_CONFIG);
        }

        prevValidServerUrlIsLongClick = isLongClick;
    }

    private void saveServerUrl(String serverUrl) {

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("serverUrl", serverUrl);
        editor.commit();

        findViewById(R.id.server_url_form).setVisibility(View.GONE);


    }

    private void authenticate() {
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        if (username.length() > 0 && password.length() > 0) {
            if (RequestDataUtil.hasNetworkConnection(this)) {
                new LoginTask().execute(
                        usernameText.getText().toString(),
                        passwordText.getText().toString()
                );
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
        pd.setTitle(R.string.request_fetching_data);
        pd.setMessage(getString(R.string.request_please_wait));
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

    private File getCustomIconFile() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        path.mkdirs();
        File file = new File(path, "custom_logo.png");

        return file;
    }

    protected void setLogoView(Bitmap bitmap) {

        logoView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));

        String filename = getCustomIconFile().getPath();

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored

            sharedPrefUtil.setCustomIconPath(filename);

        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    public class LogoDownloadTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();

                InputStream is = ucon.getInputStream();
                return BitmapFactory.decodeStream(is);

            } catch (IOException e) {
                Log.d("ImageManager", "Error: " + e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                setLogoView(bitmap);
            }
        }
    }

    /**
     * download short-cut icon
     */
    public class ShortcutTask extends AsyncTask<String, Void, Bitmap> {

        String name;
        @Override
        protected Bitmap doInBackground(String... params) {

            name = params[1];
            try {
                URL url = new URL(params[0]);
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();

                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = ucon.getInputStream();
                return BitmapFactory.decodeStream(is);

            } catch (IOException e) {
                Log.d("ImageManager", "Error: " + e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                int size = (int) getResources().getDimension(android.R.dimen.app_icon_size);

                Intent shortcutIntent = new Intent(getApplicationContext(), HomeActivity.class);
                shortcutIntent.setAction("start");

                Intent addIntent = new Intent();
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, Bitmap.createScaledBitmap(bitmap, size, size, false));

                addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                getApplicationContext().sendBroadcast(addIntent);

                setLogoView(bitmap);
            }
        }


    }

    /**
     * Post login
     */
    public class LoginTask extends AsyncTask<String, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            // authenticate and get access token
            String reqData = null;
            try {
                JSONObject json = new JSONObject();
                json.put("username", username);
                json.put("password", password);
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
                    Crouton.makeText(LoginActivity.this, getString(R.string.http_server_error), Style.ALERT)
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

                Crouton.makeText(LoginActivity.this, getString(R.string.http_server_error), Style.ALERT).show();
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
