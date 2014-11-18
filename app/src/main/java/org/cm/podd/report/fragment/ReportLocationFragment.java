package org.cm.podd.report.fragment;


import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.model.Region;
import org.cm.podd.report.util.StyleUtil;

import java.util.Calendar;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReportLocationFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ReportLocationFragment extends Fragment implements ReportNavigationChangeCallbackInterface {

    private static final String ARG_REPORT_ID = "reportId";
    private static final String TAG = "ReportLocationFragment";
    private long reportId;

    private ReportDataInterface reportDataInterface;
    private ReportNavigationInterface navigationInterface;

    private ReportDataSource reportDataSource;

    private DatePicker mDatePicker;
    private Spinner mRegionsSpinner;
    private ArrayAdapter<Region> regionAdapter;

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

        reportDataSource = new ReportDataSource(this.getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report_location, container, false);
        mDatePicker = (DatePicker) view.findViewById(R.id.date_picker);
        long maxDate = new Date().getTime();
        long minDate = maxDate - (7 * 24 * 60 * 60 * 1000); // 7 days
        mDatePicker.setMaxDate(maxDate);
        mDatePicker.setMinDate(minDate);

        mRegionsSpinner = (Spinner) view.findViewById(R.id.regions_spinner);
        regionAdapter = new ArrayAdapter<Region>(getActivity(), R.layout.spinner_location_item, reportDataSource.getAllRegion());
        mRegionsSpinner.setAdapter(regionAdapter);

        loadData();
        navigationInterface.setPrevVisible(false);

        Typeface face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);
        ((TextView) view.findViewById(R.id.incident_date)).setTypeface(face);
        ((TextView) view.findViewById(R.id.incident_place)).setTypeface(face);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        reportDataInterface = (ReportDataInterface) activity;
        navigationInterface = (ReportNavigationInterface) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        reportDataInterface = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        reportDataSource.close();
    }

    public java.util.Date getDateFromDatePicket(DatePicker datePicker){
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year =  datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        //noinspection ResourceType
        calendar.set(year, month, day);

        return calendar.getTime();
    }

    private void setData() {
        Date date = getDateFromDatePicket(mDatePicker);
        reportDataInterface.setDate(date);

        Region region = (Region) mRegionsSpinner.getSelectedItem();
        reportDataInterface.setRegionId(region.getId());
    }

    private void loadData() {
        Date date = reportDataInterface.getDate();
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            mDatePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }

        long regionId = reportDataInterface.getRegionId();
        int cnt = regionAdapter.getCount();
        for (int i = 0; i < cnt; i++) {
            if (regionAdapter.getItem(i).getId() == regionId) {
                mRegionsSpinner.setSelection(i);
            }
        }
    }


    @Override
    public void onPrevious() {
        Log.d(TAG, "callback previous");
        setData();
    }

    @Override
    public void onNext() {
        Log.d(TAG, "callback next");
        setData();
    }
}
