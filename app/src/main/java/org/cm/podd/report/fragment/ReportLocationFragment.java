package org.cm.podd.report.fragment;


import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.crashlytics.android.Crashlytics;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.model.Area;
import org.cm.podd.report.model.Region;
import org.cm.podd.report.model.view.AreaSearchAdapter;
import org.cm.podd.report.service.SyncAreaService;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.ArrayList;
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
    private ReportDataSource reportDataSource;

    private DatePicker mDatePicker;

    private RadioGroup areaRadioGroup;
    private RadioButton inAreaRadioButton;
    private RadioButton outAreaRadioButton;

    private Spinner mRegionsSpinner;
    private ArrayAdapter<Region> regionAdapter;

    private Spinner mRegionsParentSpinner;
    private ArrayAdapter<String> regionParentAdapter;

    private SharedPrefUtil sharedPrefUtil;

    private AutoCompleteTextView areaText;
    private long areaId;
    private boolean outArea = false;


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

        sharedPrefUtil = new SharedPrefUtil(getActivity().getApplicationContext());

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

        areaRadioGroup = view.findViewById(R.id.area_radio_group);
        areaRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioInArea) {
                    mRegionsParentSpinner.setVisibility(View.VISIBLE);
                    mRegionsSpinner.setVisibility(View.VISIBLE);
                    areaText.setVisibility(View.GONE);
                    outArea = false;
                } else {
                    mRegionsParentSpinner.setVisibility(View.GONE);
                    mRegionsSpinner.setVisibility(View.GONE);
                    areaText.setVisibility(View.VISIBLE);
                    outArea = true;
                }
            }
        });
        inAreaRadioButton = view.findViewById(R.id.radioInArea);
        outAreaRadioButton = view.findViewById(R.id.radioOutArea);

        mRegionsParentSpinner = (Spinner) view.findViewById(R.id.regions_parent_spinner);
        regionParentAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_location_item, sharedPrefUtil.getAllParentRegions());
        mRegionsParentSpinner.setAdapter(regionParentAdapter);

        mRegionsParentSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshRegion();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mRegionsSpinner = (Spinner) view.findViewById(R.id.regions_spinner);

        loadData();

        TextView incidentDateLabel = (TextView) view.findViewById(R.id.incident_date);
        incidentDateLabel.setText(reportDataInterface.getIncidentDateLabel());

        Typeface face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);
        incidentDateLabel.setTypeface(face);
        ((TextView) view.findViewById(R.id.incident_place)).setTypeface(face);
        ((RadioButton) view.findViewById(R.id.radioInArea)).setTypeface(face);
        ((RadioButton) view.findViewById(R.id.radioOutArea)).setTypeface(face);

        try {
            ArrayList<Area> areas = SyncAreaService.getArea(this.getContext());
            final AreaSearchAdapter adapter = new AreaSearchAdapter(this.getContext(), android.R.layout.two_line_list_item, areas);

            areaText = view.findViewById(R.id.report_area);
            areaText.setThreshold(1);//will start working from first character
            areaText.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView
            areaText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    areaId = ((Area) parent.getItemAtPosition(position)).getAreaId();
                }
            });
            areaText.setTypeface(face);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        reportDataInterface = (ReportDataInterface) activity;
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

        if (outArea) {
            reportDataInterface.setRegionId(areaId);
        } else {
            Region region = (Region) mRegionsSpinner.getSelectedItem();
            if (region == null) {
                SpinnerAdapter adapter = mRegionsSpinner.getAdapter();
                if (adapter.getCount() > 0) {
                    region = (Region) adapter.getItem(0);
                } else {
                    String username = sharedPrefUtil.getUserName();
                    Crashlytics.log("AdministrationArea in ReportLocationFragment is empty!!, username=" + username);
                }
            }
            reportDataInterface.setRegionId(region.getId());
        }
    }

    private void loadData() {
        Date date = reportDataInterface.getDate();
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            mDatePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    private void setRegionValue() {
        long regionId = reportDataInterface.getRegionId();
        int cnt = regionAdapter.getCount();
        boolean found = false;

        for (int i = 0; i < cnt; i++) {
            if (regionAdapter.getItem(i).getId() == regionId) {
                mRegionsSpinner.setSelection(i, false);
                found = true;
                inAreaRadioButton.setChecked(true);
                outAreaRadioButton.setChecked(false);
            }
        }
        if (!found) {
            ListAdapter adapter = areaText.getAdapter();
            cnt = adapter.getCount();
            for (int i = 0; i < cnt; i++) {
                Area area = (Area) adapter.getItem(i);
                if (area.getAreaId() == regionId) {
                    areaText.setText(area.getAuthorityName());
                    found = true;
                    inAreaRadioButton.setChecked(false);
                    outAreaRadioButton.setChecked(true);
                }
            }
        }
    }


    private void refreshRegion() {

        String address = (String) mRegionsParentSpinner.getSelectedItem();
        regionAdapter = new ArrayAdapter<Region>(getActivity(), R.layout.spinner_location_item, sharedPrefUtil.getFilterByRegions(address));
        mRegionsSpinner.setAdapter(regionAdapter);

        setRegionValue();

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
