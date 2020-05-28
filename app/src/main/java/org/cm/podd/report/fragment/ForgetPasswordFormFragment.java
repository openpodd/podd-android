package org.cm.podd.report.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.HomeActivity;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static android.provider.Settings.Secure.ANDROID_ID;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForgetPasswordFormFragment extends Fragment {
    private String uid;
    private String token;

    private EditText codeEditText;

    SharedPrefUtil sharedPrefUtil;
    private boolean isUserLoggedIn;


    public ForgetPasswordFormFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        sharedPrefUtil = new SharedPrefUtil(getActivity().getApplicationContext());
        isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();

        uid = getArguments().getString("uid");
        token = getArguments().getString("token");

        View view = inflater.inflate(R.layout.fragment_form_forget_password, container, false);

        codeEditText = (EditText) view.findViewById(R.id.code);

        view.findViewById(R.id.code_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitForgetPassword();
            }
        });

        isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();

        Typeface face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);
        FontUtil.overrideFonts(getActivity(), view);
        return view;
    }

    private void submitForgetPassword() {
        String code = codeEditText.getText().toString();

        if (code.length() > 0) {
            if (RequestDataUtil.hasNetworkConnection(getActivity())) {
                new LoginCodeTask().execute((Void[]) null);
            } else {
                /* alert when hasNetworkConnection */
            }
        } else {
            Crouton.makeText(getActivity(), getString(R.string.form_data_require_error), Style.ALERT, R.id.errorArea).show();
            return;
        }
    }

    /**
     * Post Register code
     */
    public class LoginCodeTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            String reqData = null;
            try {
                JSONObject json = new JSONObject();
                json.put("code", codeEditText.getText().toString());
                reqData = json.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return RequestDataUtil.post("/users/code-login/" + uid + "/" + token + "/", null, reqData, null);
        }
        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    JSONObject obj = resp.getJsonObject();

                    String token = obj.getString("token");
                    String username = obj.getString("username");
                    String password = obj.getString("displayPassword");

                    sharedPrefUtil.setAccessToken(token);
                    sharedPrefUtil.setUserName(username);
                    sharedPrefUtil.setDisplayPassword(password);

                    // get configuration
//                    Crouton.makeText(getActivity(), getString(R.string.forget_passsword_submit_success), Style.INFO).show();

                    new ConfigTask().execute((Void[]) null);

                }catch (JSONException ex) {
                    Crouton.makeText(getActivity(), getString(R.string.forget_passsword_submit_error), Style.ALERT, R.id.errorArea).show();
                }
            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(getActivity(), getString(R.string.http_server_error), Style.ALERT, R.id.errorArea).show();
                } else {
                    Crouton.makeText(getActivity(),getString(R.string.forget_passsword_submit_error), Style.ALERT, R.id.errorArea).show();
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
                ReportTypeDataSource dataSource = new ReportTypeDataSource(getActivity());
                dataSource.initNewData(obj.getJSONArray("reportTypes").toString());

                AdministrationAreaDataSource administrationAreaDataSource = new AdministrationAreaDataSource(getActivity());
                administrationAreaDataSource.initNewData(obj.getJSONArray("administrationAreas").toString());

                isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();

                // goto report home
                Intent intent = new Intent(getActivity(), HomeActivity.class);

                Bundle bundle = new Bundle();
                bundle.putString("forgetPassword", "success");

                intent.putExtras(bundle);

                startActivity(intent);

            } catch (JSONException e) {
                e.printStackTrace();

                Crouton.makeText(getActivity(), getString(R.string.http_server_error), Style.ALERT, R.id.errorArea).show();
            }
        }
    }

    private JSONObject getIdentifier() {
        Context context = getActivity().getBaseContext();
        JSONObject data = new JSONObject();
        try {
            data.put("wifiMac", ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress());
            data.put("androidId", Settings.Secure.getString(context.getContentResolver(), ANDROID_ID));
            data.put("brand", Build.BRAND);
            data.put("model", Build.MODEL);
            data.put("deviceId", Settings.Secure.getString(context.getContentResolver(), ANDROID_ID));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

}
