package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.util.StyleUtil;

public class ReportConfirmFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_REPORT_ID = "reportId";

    // TODO: Rename and change types of parameters
    private long reportId;

    private ReportNavigationInterface navigationInterface;
    private ReportDataInterface dataInterface;


    private ProgressDialog progress;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param reportId Parameter 1.
     * @return A new instance of fragment ReportConfirmFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportConfirmFragment newInstance(long reportId) {
        ReportConfirmFragment fragment = new ReportConfirmFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }
    public ReportConfirmFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reportId = getArguments().getLong(ARG_REPORT_ID);
        }
        navigationInterface.setPrevVisible(false);
        navigationInterface.setNextVisible(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report_confirm, container, false);
        Button testBtn = (Button) view.findViewById(R.id.testBtn);
        Button confirmBtn = (Button) view.findViewById(R.id.confirmBtn);
        Button cancelBtn = (Button) view.findViewById(R.id.cancelBtn);
        Button draftBtn = (Button) view.findViewById(R.id.draftBtn);
        TextView confirmText = (TextView) view.findViewById(R.id.confirm_message);

        if (dataInterface.isDoneSubmit()) {
            testBtn.setVisibility(View.GONE);
            draftBtn.setVisibility(View.GONE);
            confirmBtn.setVisibility(View.GONE);
            cancelBtn.setText(R.string.button_back_to_report_list);
            confirmText.setText(R.string.done_submit_message);
        } else {
            cancelBtn.setVisibility(View.GONE);
        }

        if (dataInterface.isTestReport()) {
            testBtn.setVisibility(View.GONE);
        }

        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Tracker tracker = ((PoddApplication) getActivity().getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
                tracker.setScreenName("ReportConfirmToTest");
                tracker.send(new HitBuilders.AppViewBuilder().build());

                displayDialog();
                navigationInterface.finishReport(ReportDataInterface.TEST_ACTION);
            }
        });

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (dataInterface.isTestReport()) {

                    displayDialog();
                    navigationInterface.finishReport(ReportDataInterface.CONFIRM_ACTION);

                } else {
                    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.confirm_message_title)
                            .setMessage(R.string.confirm_report_message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.confirm_positive_report, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    displayDialog();
                                    navigationInterface.finishReport(ReportDataInterface.CONFIRM_ACTION);
                                }
                            })
                            .setNegativeButton(R.string.confirm_test_report, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    displayDialog();
                                    navigationInterface.finishReport(ReportDataInterface.TEST_ACTION);
                                }
                            }).create();

                    dialog.show();
                }
            }
        });

        draftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayDialog();
                navigationInterface.finishReport(ReportDataInterface.DRAFT_ACTION);
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationInterface.finishReport(ReportDataInterface.CANCEL_ACTION);
            }
        });

        Typeface face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);
        testBtn.setTypeface(face);
        confirmText.setTypeface(face);
        confirmBtn.setTypeface(face);
        cancelBtn.setTypeface(face);
        draftBtn.setTypeface(face);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        navigationInterface = (ReportNavigationInterface) activity;
        dataInterface = (ReportDataInterface) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationInterface = null;
        if (progress != null) {
            progress.dismiss();
        }
    }

    private void displayDialog() {
        progress = ProgressDialog.show(this.getActivity(),
                getString(R.string.request_fetching_data),
                getString(R.string.request_please_wait), true);
    }

}
