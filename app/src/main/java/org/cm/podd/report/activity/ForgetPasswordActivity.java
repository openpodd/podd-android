package org.cm.podd.report.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.ForgetPasswordFormFragment;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ForgetPasswordActivity extends AppCompatActivity {

    private Bundle bundle;

    Fragment mCurrentFragment;
    EditText serialNumberText;

    private Pattern pattern;
    private Matcher matcher;

    private static final String SERIAL_NUMBER_PATTERN = "^[0-9]{13,}$";

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        serialNumberText = (EditText) findViewById(R.id.serial_number);
        findViewById(R.id.serial_number_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pd == null || !pd.isShowing())
                    submitSerialNumber();
            }
        });

        FontUtil.overrideFonts(this, this.findViewById(R.id.serial_number_content).getRootView());
    }

    private void submitSerialNumber() {
        String serialNumber = serialNumberText.getText().toString();
        if (serialNumber.length() == 13 && Pattern.compile(SERIAL_NUMBER_PATTERN).matcher(serialNumber).matches()) {
            if (RequestDataUtil.hasNetworkConnection(this)) {
                new SerialNumberTask().execute((Void[]) null);
            } else {
                 /* alert when hasNetworkConnection */
            }
        } else {
            Crouton.makeText(this, getString(R.string.form_data_require_error), Style.ALERT, R.id.errorArea).show();
            return;
        }
    }

    /**
     * Post Invite code
     */
    public class SerialNumberTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            // authenticate and get access token
            String reqData = null;
            String serialNumber = serialNumberText.getText().toString();
            try {
                JSONObject json = new JSONObject();
                json.put("serialNumber", serialNumberText.getText().toString());

                reqData = json.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return RequestDataUtil.post("/users/forgot-password/", null, reqData, null);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
//            Log.d("resp", resp.toString());
            hideProgressDialog();

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    JSONObject obj = resp.getJsonObject();

                    final String uid = obj.getString("uid");
                    final String token = obj.getString("token");
                    final String telephone = obj.getString("telephone");

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ForgetPasswordActivity.this);
                    alertDialogBuilder.setTitle(R.string.prove_identity_dlg_title);
                    alertDialogBuilder.setMessage(getString(R.string.password_sent_via_sms_text, telephone));
                    alertDialogBuilder.setPositiveButton(getString(R.string.fill_temp_password_btn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {


                        }
                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                    setTitle(R.string.temp_password_title);

                    mCurrentFragment = new ForgetPasswordFormFragment();

                    bundle = new Bundle();
                    bundle.putString("uid", uid);
                    bundle.putString("token", token);

                    mCurrentFragment.setArguments(bundle);

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.form_content, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                            .commit();

                    findViewById(R.id.serial_number_content).setVisibility(View.GONE);
                    findViewById(R.id.form_content).setVisibility(View.VISIBLE);

                }catch (JSONException ex) {
                    Crouton.makeText(ForgetPasswordActivity.this, getString(R.string.serial_number_error), Style.ALERT, R.id.errorArea).show();
                }
            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(ForgetPasswordActivity.this, getString(R.string.http_server_error), Style.ALERT, R.id.errorArea).show();
                } else {
                    Crouton.makeText(ForgetPasswordActivity.this, R.string.serial_number_error, Style.ALERT, R.id.errorArea).show();
                }

            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_forget_password));
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
//        actionBar.setLogo(R.drawable.arrow_left_with_pad);
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
            if (bundle != null && findViewById(R.id.form_content).getVisibility() == View.VISIBLE) {
                findViewById(R.id.serial_number_content).setVisibility(View.VISIBLE);
                findViewById(R.id.form_content).setVisibility(View.GONE);
                StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_forget_password));
            } else {
                this.finish();
                return true;
            }

        }

        return super.onOptionsItemSelected(item);
    }
}
