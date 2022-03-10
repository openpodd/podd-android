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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static android.provider.Settings.Secure.ANDROID_ID;

/**
 * A placeholder fragment containing a simple view.
 */
public class RegistrationFormFragment extends Fragment {
    private String groupInviteCode;
    private String groupName;
    private int authorityId;
    private String authorityName;

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText serialNumberEditText;
    private EditText telephoneEditText;
    private EditText emailEditText;

    private Pattern pattern;
    private Matcher matcher;

    private SharedPrefUtil sharedPrefUtil;
    private boolean isUserLoggedIn;
    private boolean isSubmit = false;

    private static final String SERIAL_NUMBER_PATTERN = "^[0-9]{13,}$";

    private static final String TELEPHONE_PATTERN = "^[0-9]{10,}$";

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public RegistrationFormFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        sharedPrefUtil = new SharedPrefUtil(getActivity().getApplicationContext());
        isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();

        groupInviteCode = getArguments().getString("GroupInviteCode");
        groupName = getArguments().getString("GroupName");
        authorityId = getArguments().getInt("authorityId");
        authorityName = getArguments().getString("authorityName");

        View view = inflater.inflate(R.layout.fragment_form_registration, container, false);

        Typeface face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);

        firstNameEditText = view.findViewById(R.id.first_name);
        lastNameEditText = view.findViewById(R.id.last_name);
        serialNumberEditText = view.findViewById(R.id.serial_number);
        telephoneEditText = view.findViewById(R.id.telephone);
        emailEditText = view.findViewById(R.id.email);

        EditText textAuthorityName = view.findViewById(R.id.invite_code);
        if (groupName != null && groupName.length() > 0) {
            textAuthorityName.setText(groupName);
        } else {
            textAuthorityName.setText(authorityName);
        }
        textAuthorityName.setTypeface(face);

        view.findViewById(R.id.register_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRegister();
            }
        });

        isUserLoggedIn = sharedPrefUtil.isUserLoggedIn();
        FontUtil.overrideFonts(getActivity(), view);
        return view;
    }

    private void showDialogConfirm() {
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String serialNumber = serialNumberEditText.getText().toString();
        String telephone = telephoneEditText.getText().toString();
        String email = emailEditText.getText().toString().equalsIgnoreCase("")? "-": emailEditText.getText().toString();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.confirm_data_title);
        alertDialogBuilder.setMessage(getString(
                R.string.registration_data_detail, firstName, lastName, serialNumber,
                telephone, email, groupName)
        );
        alertDialogBuilder.setPositiveButton(R.string.yes_choice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (RequestDataUtil.hasNetworkConnection(getActivity())) {
                    if (!isSubmit) {
                        new RegisterTask().execute((Void[]) null);
                        isSubmit = true;
                    }
                } else {
                     /* alert when hasNetworkConnection */
                }
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.no_choice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    private void submitRegister() {
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String serialNumber = serialNumberEditText.getText().toString();
        String telephone = telephoneEditText.getText().toString();
        String email = emailEditText.getText().toString();

        boolean firstNameValid = firstName.length() > 0;
        boolean lastNameValid = lastName.length() > 0;
        boolean serialNumberValid = true;
        if ((serialNumber != null) && (serialNumber.length() > 0)) {
            serialNumberValid = Pattern.compile(SERIAL_NUMBER_PATTERN).matcher(serialNumber).matches();
        }
        boolean telephoneValid = Pattern.compile(TELEPHONE_PATTERN).matcher(telephone).matches();
        boolean emailValid = email.length() == 0 || Pattern.compile(EMAIL_PATTERN).matcher(email).matches();

        if (firstNameValid && lastNameValid && serialNumberValid && telephoneValid && emailValid) {
            showDialogConfirm();
        } else if (!firstNameValid) {
            Crouton.makeText(getActivity(), getString(R.string.name_error), Style.ALERT, R.id.errorArea).show();
            return;
        } else if (!lastNameValid) {
            Crouton.makeText(getActivity(), getString(R.string.name_error), Style.ALERT, R.id.errorArea).show();
            return;
        } else if (!serialNumberValid) {
            Crouton.makeText(getActivity(), getString(R.string.serial_number_error), Style.ALERT, R.id.errorArea).show();
            return;
        } else if (!telephoneValid){
            Crouton.makeText(getActivity(), getString(R.string.telephone_error), Style.ALERT, R.id.errorArea).show();
            return;
        } else if (!emailValid){
            Crouton.makeText(getActivity(), getString(R.string.email_error), Style.ALERT, R.id.errorArea).show();
            return;
        } else {
            Crouton.makeText(getActivity(), getString(R.string.form_data_require_error), Style.ALERT, R.id.errorArea).show();
            return;
        }
    }

    /**
     * Post Register code
     */
    public class RegisterTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            String reqData = null;
            try {
                JSONObject json = new JSONObject();
                json.put("firstName", firstNameEditText.getText().toString());
                json.put("lastName", lastNameEditText.getText().toString());
                json.put("serialNumber", serialNumberEditText.getText().toString());
                json.put("telephone", telephoneEditText.getText().toString());
                json.put("email", emailEditText.getText().toString());
                json.put("group", groupInviteCode);
                reqData = json.toString();

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return RequestDataUtil.post("/users/register/group/", null, reqData, null);
        }
        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
                try {
                    JSONObject obj = resp.getJsonObject();

                    String token = obj.getString("token");
                    String username = obj.getString("username");
                    String password = obj.getString("displayPassword");

                    sharedPrefUtil.setAccessToken(token);
                    sharedPrefUtil.setUserName(username);
                    sharedPrefUtil.setDisplayPassword(password);

                    // get configuration
//                    Crouton.makeText(getActivity(), getString(R.string.register_submit_success), Style.INFO).show();

                    new ConfigTask().execute((Void[]) null);

                }catch (JSONException ex) {
                    Crouton.makeText(getActivity(), getString(R.string.register_submit_error), Style.ALERT, R.id.errorArea).show();
                }
            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(getActivity(), getString(R.string.http_server_error), Style.ALERT, R.id.errorArea).show();
                } else {
                    try {
                        JSONObject obj = resp.getJsonObject();
                        String detail = obj.getString("detail");
                        if (detail.contains("telephone")) {
                            Crouton.makeText(getActivity(), getString(R.string.telephone_same_error), Style.ALERT, R.id.errorArea).show();
                        } else {
                            Crouton.makeText(getActivity(), getString(R.string.register_submit_error), Style.ALERT, R.id.errorArea).show();
                        }
                    }  catch (JSONException ex) {
                            Crouton.makeText(getActivity(), getString(R.string.register_submit_error), Style.ALERT, R.id.errorArea).show();
                    }
                }
            }
            isSubmit = false;
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
                intent.putExtra("register", "success");
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
