package org.cm.podd.report.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static android.provider.Settings.Secure.ANDROID_ID;

/**
 * A placeholder fragment containing a simple view.
 */
public class ResetPasswordFragment extends Fragment {
    private EditText passwordEditText;
    private EditText repasswordEditText;

    SharedPrefUtil sharedPrefUtil;
    private boolean isUserLoggedIn;
    private View rootView;

    public ResetPasswordFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        sharedPrefUtil = new SharedPrefUtil(getActivity().getApplicationContext());
        isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();

        View view = inflater.inflate(R.layout.fragment_reset_password, container, false);
        rootView = view.getRootView();

        passwordEditText = (EditText) view.findViewById(R.id.new_password);
        repasswordEditText = (EditText) view.findViewById(R.id.re_new_password);

        view.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
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

    private static final String PASSWORD_PATTERN = "^[0-9]+$";

    private void submitForgetPassword() {
        String newPassword = passwordEditText.getText().toString();
        String reNewPassword = repasswordEditText.getText().toString();

        if (newPassword.length() >= 4 && Pattern.compile(PASSWORD_PATTERN).matcher(newPassword).matches() &&
                newPassword.equalsIgnoreCase(reNewPassword)) {
            if (RequestDataUtil.hasNetworkConnection(getActivity())) {
                new ResetPasswordTask().execute((Void[]) null);
            } else {
                Crouton.makeText(getActivity(), getString(R.string.alert_no_network_connection), Style.ALERT).show();
            }
        } else {
            Crouton.makeText(getActivity(), getString(R.string.password_not_match), Style.ALERT).show();
            return;
        }
    }

    /**
     * Post Register code
     */
    public class ResetPasswordTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            String accessToken = sharedPrefUtil.getAccessToken();
            String reqData = null;
            try {
                JSONObject json = new JSONObject();
                json.put("password", passwordEditText.getText().toString());
                reqData = json.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return RequestDataUtil.post("/users/profile/password/", null, reqData, accessToken);
        }
        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                // Back
                rootView.getRootView().findViewById(R.id.detail_content).setVisibility(View.VISIBLE);
                rootView.getRootView().findViewById(R.id.form_content).setVisibility(View.GONE);

                StyleUtil.setActionBarTitle(getActivity(), getString(R.string.title_activity_setting));

                Crouton.makeText(getActivity(),getString(R.string.reset_passsword_submit_success), Style.INFO).show();

            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(getActivity(), "Error on Server, please contact administration", Style.ALERT).show();
                } else {
                    Crouton.makeText(getActivity(),getString(R.string.reset_passsword_submit_error), Style.ALERT).show();
                }

            }
        }
    }




}
