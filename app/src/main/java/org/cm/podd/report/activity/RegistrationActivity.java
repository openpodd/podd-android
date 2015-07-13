package org.cm.podd.report.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.RegistrationFormFragment;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RegistrationActivity extends ActionBarActivity {

    private Bundle bundle;

    Fragment mCurrentFragment;
    EditText inviteCodeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        inviteCodeText = (EditText) findViewById(R.id.invite_code);
        findViewById(R.id.invite_code_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCode();
            }
        });

        FontUtil.overrideFonts(this, this.findViewById(R.id.invite_content).getRootView());
    }

    private void submitCode() {
        String inviteCode = inviteCodeText.getText().toString();
        if (inviteCode.length() > 0) {
            if (RequestDataUtil.hasNetworkConnection(this)) {
                new InviteCodeTask().execute((Void[]) null);
            }
        } else {
            Crouton.makeText(this, "Required invitation", Style.ALERT).show();
            return;
        }
    }

    /**
     * Post Invite code
     */
    public class InviteCodeTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            // authenticate and get access token
            String reqData = null;
            String inviteCode = inviteCodeText.getText().toString();
            return RequestDataUtil.get("/users/register/authority/?invitationCode=" + inviteCode, reqData, null);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            Log.d("resp", resp.toString());
            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    JSONObject obj = new JSONObject(resp.getRawData());

                    final String inviteCode = inviteCodeText.getText().toString();
                    final String name = obj.getString("name");

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RegistrationActivity.this);
                    alertDialogBuilder.setTitle("พื้นที่ลงทะเบียน");
                    alertDialogBuilder.setMessage("คุณต้องการลงทะเบียนในพื้นที่: " + name);
                    alertDialogBuilder.setPositiveButton("ใช่", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            setTitle("แบบฟอร์มลงทะเบียน");

                            mCurrentFragment = new RegistrationFormFragment();

                            bundle = new Bundle();
                            bundle.putString("AuthorityInviteCode", inviteCode);
                            bundle.putString("AuthorityName", name);

                            mCurrentFragment.setArguments(bundle);

                            FragmentManager fragmentManager = getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .replace(R.id.form_content, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                                    .commit();

                            findViewById(R.id.invite_content).setVisibility(View.GONE);
                            findViewById(R.id.form_content).setVisibility(View.VISIBLE);

                        }
                    });
                    alertDialogBuilder.setNegativeButton("ไม่ใช่", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                          finish();
                        }
                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }catch (JSONException ex) {
                    Crouton.makeText(RegistrationActivity.this, "Invitation Code is incorrect!", Style.ALERT).show();
                }
            } else {
                if (resp.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    Crouton.makeText(RegistrationActivity.this, "Error on Server, please contact administration", Style.ALERT).show();
                } else {
                    Crouton.makeText(RegistrationActivity.this, "Invitation Code is incorrect!", Style.ALERT).show();
                }

            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_registration));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);
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
            } else {
                this.finish();
                return true;
            }

        }

        return super.onOptionsItemSelected(item);
    }

}