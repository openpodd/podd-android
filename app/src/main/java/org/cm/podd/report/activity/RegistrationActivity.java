package org.cm.podd.report.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.RegistrationFormFragment;
import org.cm.podd.report.model.Area;
import org.cm.podd.report.model.view.AreaSearchAdapter;
import org.cm.podd.report.service.SyncAreaService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RegistrationActivity extends AppCompatActivity {

    private Bundle bundle;

    Fragment mCurrentFragment;
    EditText inviteCodeText;
    AutoCompleteTextView areaText;
    Area selectedArea;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (this.isDestroyed()) {
                return;
            }
        } else {
            if (this.isFinishing()) { // or call isFinishing() if min sdk version < 17
                return;
            }
        }

        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        inviteCodeText = findViewById(R.id.invite_code);
        findViewById(R.id.invite_code_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pd == null || !pd.isShowing())
                    submitCode();
            }
        });

        FontUtil.overrideFonts(this, this.findViewById(R.id.invite_content).getRootView());

        try {
            ArrayList<Area> areas = SyncAreaService.getArea(getApplicationContext());
            final AreaSearchAdapter adapter = new AreaSearchAdapter(this, android.R.layout.two_line_list_item, areas);

            areaText = findViewById(R.id.area);
            areaText.setThreshold(1);//will start working from first character
            areaText.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView
            areaText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedArea = (Area) parent.getItemAtPosition(position);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void submitCode() {
        String inviteCode = inviteCodeText.getText().toString();

        if (inviteCode.length() > 0) {
            if (! inviteCode.matches("([0-9]{6,8})")) {
                Toast.makeText(this, R.string.invitation_code_must_be_numeric, Toast.LENGTH_LONG).show();
            } else {
                if (RequestDataUtil.hasNetworkConnection(this)) {
                    new InviteCodeTask().execute(inviteCode);
                }
            }
        } else {
            if (selectedArea != null) {
                mCurrentFragment = new RegistrationFormFragment();

                bundle = new Bundle();
                bundle.putString("GroupInviteCode", "");
                bundle.putString("GroupName", "");

                bundle.putInt("authorityId", selectedArea.getAuthorityId());
                bundle.putString("authorityName", selectedArea.getAuthorityName());

                mCurrentFragment.setArguments(bundle);

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.form_content, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                        .commit();

                findViewById(R.id.invite_content).setVisibility(View.GONE);
                findViewById(R.id.form_content).setVisibility(View.VISIBLE);
            } else {
                Crouton.makeText(this, getString(R.string.form_data_require_error), Style.ALERT, R.id.errorArea).show();
            }
        }
    }

    /**
     * Post Invite code
     */
    public class InviteCodeTask extends AsyncTask<String, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            // authenticate and get access token
            String reqData = null;
            String inviteCode = params[0];
            return RequestDataUtil.get("/users/register/group/code/?invitationCode=" + inviteCode, reqData, null);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
//            Log.d("resp", resp.toString());
            hideProgressDialog();

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    JSONObject obj = new JSONObject(resp.getRawData());

                    final String inviteCode = inviteCodeText.getText().toString();
                    final String name = obj.getString("name");

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RegistrationActivity.this);
                    alertDialogBuilder.setTitle(R.string.register_area_title);
                    alertDialogBuilder.setMessage(getString(R.string.your_area_text, name));
                    alertDialogBuilder.setPositiveButton(R.string.yes_choice, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            setTitle(R.string.registration_form);

                            mCurrentFragment = new RegistrationFormFragment();

                            bundle = new Bundle();
                            bundle.putString("GroupInviteCode", inviteCode);
                            bundle.putString("GroupName", name);

                            mCurrentFragment.setArguments(bundle);

                            FragmentManager fragmentManager = getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .replace(R.id.form_content, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                                    .commit();

                            findViewById(R.id.invite_content).setVisibility(View.GONE);
                            findViewById(R.id.form_content).setVisibility(View.VISIBLE);

                        }
                    });
                    alertDialogBuilder.setNegativeButton(R.string.no_choice, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                          finish();
                        }
                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }catch (JSONException ex) {
                    Crouton.makeText(RegistrationActivity.this, getString(R.string.invite_code_error), Style.ALERT, R.id.errorArea).show();
                }
            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(RegistrationActivity.this, getString(R.string.http_server_error), Style.ALERT, R.id.errorArea).show();
                } else {
                    Crouton.makeText(RegistrationActivity.this, R.string.invite_code_error, Style.ALERT, R.id.errorArea).show();
                }

            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_registration));
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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
                findViewById(R.id.invite_content).setVisibility(View.VISIBLE);
                findViewById(R.id.form_content).setVisibility(View.GONE);
                StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_registration));
            } else {
                this.finish();
                return true;
            }

        }

        return super.onOptionsItemSelected(item);
    }

}
