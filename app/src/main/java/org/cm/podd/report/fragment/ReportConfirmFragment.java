package org.cm.podd.report.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.cm.podd.report.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReportConfirmFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReportConfirmFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ReportConfirmFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_REPORT_ID = "reportId";

    // TODO: Rename and change types of parameters
    private long reportId;

    private ReportNavigationInterface navigationInterface;
    private ReportDataInterface dataInterface;

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
        Button confirmBtn = (Button) view.findViewById(R.id.confirmBtn);
        Button cancelBtn = (Button) view.findViewById(R.id.cancelBtn);
        Button draftBtn = (Button) view.findViewById(R.id.draftBtn);

        if (dataInterface.isDoneSubmit()) {
            draftBtn.setVisibility(View.GONE);
        }

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO mark report status to "ready to send to server"
                navigationInterface.finishReport(ReportDataInterface.CONFIRM_ACTION);
            }
        });

        draftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO mark report status to "ready to send to server"
                navigationInterface.finishReport(ReportDataInterface.DRAFT_ACTION);
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationInterface.finishReport(ReportDataInterface.CANCEL_ACTION);
            }
        });

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
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationInterface = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
