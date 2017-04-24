package org.cm.podd.report.activity;

import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.ResetPasswordFragment;
import org.cm.podd.report.util.SharedPrefUtil;

public class ResetPasswordActivity extends AppCompatActivity {

    public static final String TAG = "UserPasswordActivity";

    Fragment mCurrentFragment;
    Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_password);

        mCurrentFragment = new ResetPasswordFragment();

        bundle = new Bundle();
        bundle.putString("reset", "true");
        mCurrentFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.form_content, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                .commit();


    }
}
