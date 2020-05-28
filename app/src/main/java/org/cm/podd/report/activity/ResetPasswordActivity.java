package org.cm.podd.report.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.MenuItem;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.ResetPasswordFragment;

public class ResetPasswordActivity extends AppCompatActivity {

    public static final String TAG = "UserPasswordActivity";

    Fragment mCurrentFragment;
    Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_password);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentFragment = new ResetPasswordFragment();

        bundle = new Bundle();
        bundle.putString("reset", "true");
        mCurrentFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.form_content, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                .commit();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home){
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

}
