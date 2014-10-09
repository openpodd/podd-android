package org.cm.podd.report.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.fragment.ReportConfirmFragment;
import org.cm.podd.report.fragment.ReportImageFragment;
import org.cm.podd.report.fragment.ReportLocationFragment;
import org.cm.podd.report.fragment.ReportNavigationInterface;
import org.cm.podd.report.model.FormIterator;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.validation.ValidationResult;
import org.cm.podd.report.model.view.PageView;

import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ReportActivity extends ActionBarActivity implements ReportNavigationInterface {

    private Button prevBtn;
    private Button nextBtn;

    private String currentFragment;
    private ReportDataSource reportDataSource;
    private ReportTypeDataSource reportTypeDataSource;
    private long reportId;
    private int reportType;
    private FormIterator formIterator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        reportType = getIntent().getIntExtra("reportType", 0);

        setContentView(R.layout.activity_report);
        if (savedInstanceState == null) {
            nextScreen();
        }

        prevBtn = (Button) findViewById(R.id.prevBtn);
        nextBtn = (Button) findViewById(R.id.nextBtn);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextScreen();
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        reportTypeDataSource = new ReportTypeDataSource(this);
        formIterator = new FormIterator(reportTypeDataSource.getForm(reportType));

        reportDataSource = new ReportDataSource(this);
        reportId = reportDataSource.createDraftReport();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("----", "from fragment = " + currentFragment);

        if (currentFragment != null) {
            if (currentFragment.equals(ReportLocationFragment.class.getName())) {
                currentFragment = ReportImageFragment.class.getName();
            } else if (currentFragment.equals(ReportImageFragment.class.getName())) {
                currentFragment = null;
            } else if (currentFragment.equals(ReportConfirmFragment.class.getName())) {
                currentFragment = "dynamicForm";
                setNextVisible(true);
            } else if (currentFragment.equals("dynamicForm")) {
                if (! formIterator.previousPage()) {
                    currentFragment = ReportLocationFragment.class.getName();
                }
            }
        }
        Log.d("----", "back to fragment = " + currentFragment);
    }

    private void nextScreen() {
        Fragment fragment = null;
        boolean isDynamicForm = false;

        if (currentFragment == null) { /* first screen */
            fragment = ReportImageFragment.newInstance(reportId);
        } else {
            if (currentFragment.equals(ReportImageFragment.class.getName())) {

                fragment = ReportLocationFragment.newInstance(reportId);

            } else if (currentFragment.equals(ReportConfirmFragment.class.getName())) {
                /* do nothing */

            } else {
                isDynamicForm = true;

                setNextVisible(true);
                setPrevVisible(true);
                setNextEnable(true);
                setPrevEnable(true);

                // case I
                // just come into this dynamic form
                // serving fragment(currentPage)
                // case II
                // we are not in first page
                // and not in last page
                // so we proceed to nextPage
                // case III
                // we are at last page
                // so we skip to ReportConfirmFragment
                if (currentFragment.equals(ReportLocationFragment.class.getName())) {
                    // no-op
                    fragment = getPageFramgment(formIterator.getCurrentPage());
                } else if (formIterator.isAtLastPage()) {
                    fragment = ReportConfirmFragment.newInstance(reportId);
                    isDynamicForm = false;
                } else {
                    if (! formIterator.nextPage()) {

                        // validation case
                        List<ValidationResult> validateResults = formIterator.getCurrentPage().validate();
                        StringBuffer buff = new StringBuffer();
                        for (ValidationResult vr : validateResults) {
                            buff.append(vr.getMessage()).append("\n");
                        }
                        final Crouton crouton = Crouton.makeText(this, buff.toString(), Style.ALERT);
                        crouton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Crouton.hide(crouton);
                            }
                        });
                        crouton.show();

                    } else {

                        fragment = getPageFramgment(formIterator.getCurrentPage());

                    }
                }

            }
        }

        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (currentFragment == null) {
                transaction.add(R.id.container, fragment);
            } else {
                transaction.replace(R.id.container, fragment);
                transaction.addToBackStack(fragment.getClass().getName());
            }
            transaction.commit();

            if (isDynamicForm) {
                currentFragment = "dynamicForm";
            } else {
                currentFragment = fragment.getClass().getName();
            }

        }

        Log.d("----", "current fragment = " + currentFragment);
    }

    private Fragment getPageFramgment(Page page) {
        FormPageFragment fragment = new FormPageFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("page", formIterator.getCurrentPage());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setNextEnable(boolean flag) {
        nextBtn.setEnabled(flag);
    }

    @Override
    public void setPrevEnable(boolean flag) {
        prevBtn.setEnabled(flag);
    }

    @Override
    public void setNextVisible(boolean flag) {
        if (flag) {
            nextBtn.setVisibility(View.VISIBLE);
        } else {
            nextBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPrevVisible(boolean flag) {
        if (flag) {
            prevBtn.setVisibility(View.VISIBLE);
        } else {
            prevBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void finishReport() {
        NavUtils.navigateUpFromSameTask(this);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class FormPageFragment extends Fragment {

        private Page page;

        public FormPageFragment() {
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            Page page = (Page) arguments.get("page");
            return new PageView(getActivity(), page);
        }

    }
}
