package org.cm.podd.report.fragment;



import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.cm.podd.report.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReportLocationFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ReportLocationFragment extends Fragment {

    private static final String ARG_REPORT_ID = "reportId";
    private long reportId;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param reportId Parameter 1.
     * @return A new instance of fragment ReportLocationFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportLocationFragment newInstance(long reportId) {
        ReportLocationFragment fragment = new ReportLocationFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }
    public ReportLocationFragment() {
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
        return inflater.inflate(R.layout.fragment_report_location, container, false);
    }


}
