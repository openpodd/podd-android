package org.cm.podd.report.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.cm.podd.report.R;

/**
 * Use the {@link ReportImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ReportImageFragment extends Fragment {

    private static final String ARG_REPORT_ID = "reportId";

    private long reportId;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param reportId
     * @return A new instance of fragment ReportImageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportImageFragment newInstance(long reportId) {
        ReportImageFragment fragment = new ReportImageFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }
    public ReportImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reportId = getArguments().getLong(ARG_REPORT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_report_image, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }



}
