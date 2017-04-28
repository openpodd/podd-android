package org.cm.podd.report.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.cm.podd.report.R;
import org.cm.podd.report.TouchHighlightImageButton;
import org.cm.podd.report.activity.ImageActivity;
import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.db.ReportStateDataSource;
import org.cm.podd.report.model.FeedAdapter;
import org.cm.podd.report.model.ReportState;
import org.cm.podd.report.model.State;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReportStateFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReportStateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReportStateFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private LinearLayout flagReadOnlyView;
    private ImageView flagImageView;
    private TextView flagView;
    private Spinner flagSpinnerView;
    private HintAdapter mFlagAdapter;
    private TextView caseDialogTitle;
    private RelativeLayout caseDialog;
    private RadioGroup caseRadioGroup;
    private ProgressBar caseDialogProgressBar;

    private Long id;
    private Long currentFlag;
    private Long oldFlag;

    private String oldStateCode;
    private int oldStateCodePosition;

    private SharedPrefUtil sharedPrefUtil;

    public static final State[] stateColors = new State[]{
            new State("report", R.drawable.blank),
            new State("insignificant-report", R.drawable.flag_ignore),
            new State("false-report", R.drawable.flag_ignore),
            new State("no-outbreak-identified", R.drawable.flag_ignore),
            new State("case", R.drawable.flag_contact),
            new State("3", R.drawable.flag_contact),
            new State("follow", R.drawable.flag_follow),
            new State("4", R.drawable.flag_follow),
            new State("suspect-outbreak", R.drawable.flag_case),
            new State("outbreak", R.drawable.flag_case),
            new State("5", R.drawable.flag_case),
            new State("finish", R.drawable.flag_ok),
    };

    private Long selectedCaseId;
    private List<ReportState> reportStates;
    private int reportTypeId = 0;

    public Map<String, State> states = new HashMap<String, State>();

    ReportStateDataSource reportStateDataSource;

    private Context context;

    public ReportStateFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReportStateFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportStateFragment newInstance(String param1, String param2) {
        ReportStateFragment fragment = new ReportStateFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        context = getContext();

        View view = inflater.inflate(R.layout.fragment_report_state, null);

        flagReadOnlyView = (LinearLayout) view.findViewById(R.id.flag_read_only);
        flagView = (TextView) view.findViewById(R.id.flag_name);
        flagImageView = (ImageView) view.findViewById(R.id.flag_icon);
        flagSpinnerView = (Spinner) view.findViewById(R.id.flag_spinner);
        caseDialogTitle = (TextView) view.findViewById(R.id.case_dialog_title);
        caseDialog = (RelativeLayout) view.findViewById(R.id.case_dialog);
        caseRadioGroup = (RadioGroup) view.findViewById(R.id.case_radio);
        caseDialogProgressBar = (ProgressBar) view.findViewById(R.id.case_dialog_progressbar);
        // init flag element.
        caseDialog.setVisibility(View.GONE);

        sharedPrefUtil = new SharedPrefUtil(getContext());

        String _report = getArguments().getString("report");
        try {
            JSONObject report = new JSONObject(_report);

            id = report.getLong("id");

            // Add flag controller
            Long reportFlag;
            try {
                reportFlag = report.getLong("flag");
            } catch (JSONException e) {
                reportFlag = 0L;
            }
            currentFlag = reportFlag;
            oldFlag = reportFlag;

            // Flag spinner.
            if (sharedPrefUtil.getCanSetFlag()) {
                flagReadOnlyView.setVisibility(View.GONE);
                flagSpinnerView.setVisibility(View.VISIBLE);
            } else {

                flagReadOnlyView.setVisibility(View.VISIBLE);
                flagSpinnerView.setVisibility(View.GONE);
            }

            String stateCode = ((ReportViewActivity)getActivity()).getReportState();


            if (!report.getString("parent").equals("null")) {
                flagReadOnlyView.setVisibility(View.VISIBLE);
                flagSpinnerView.setVisibility(View.GONE);
                stateCode = "follow";
                currentFlag = 4L;
            }


            if (stateCode == null)
                stateCode = report.getString("stateCode");

            oldStateCode = stateCode;
            int stateImage = R.drawable.blank;
            for (int i = 0; i < stateColors.length; i++ ){
                if (stateColors[i].getCode().equals(stateCode)) {
                    stateImage = stateColors[i].getColor();
                    break;
                }
            }

            flagImageView.setImageResource(stateImage);

            flagView.setText(getResources().getStringArray(
                    R.array.flags_optional)[currentFlag.intValue()]);


            reportTypeId = report.getInt("reportTypeId");

            reportStateDataSource = new ReportStateDataSource(getContext());
            reportStates = reportStateDataSource.getByReportType(reportTypeId);

            String [] states_with_hint = new String[reportStates.size() + 1];
            int statePosition = reportStates.size();
            for (int i = 0; i < reportStates.size(); i++){
                states_with_hint[i] = reportStates.get(i).getCode();

                int color = R.drawable.blank;
                for (int j = 0; j < stateColors.length; j++){
                    if (reportStates.get(i).getCode().equals(stateColors[j].getCode())) {
                        color = stateColors[j].getColor();
                        break;
                    }
                }

                State state = new State(reportStates.get(i).getCode(), color);
                state.setName(reportStates.get(i).getName());
                states.put(reportStates.get(i).getCode(), state);

                if (stateCode.equals(reportStates.get(i).getCode())) {
                    statePosition = i;
                    oldStateCodePosition = statePosition;

                    ImageView iconView = (ImageView) view.findViewById(R.id.flag_icon);
                    iconView.setImageResource(state.getColor());

                    TextView textView = (TextView) view.findViewById(R.id.flag_name);
                    textView.setText(state.getName());
                    FontUtil.overrideFonts(getContext(), textView);
                }
            }

            states_with_hint[reportStates.size()] = getString(R.string.set_report_state);

            mFlagAdapter = new HintAdapter(getContext(), states_with_hint);

            flagSpinnerView.setAdapter(mFlagAdapter);
            flagSpinnerView.setSelection(statePosition);

            if (stateCode.equals("")) {
                flagSpinnerView.setSelection(mFlagAdapter.getCount());
            }

            final int[] initial = {0};
            flagSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                    final String state = mFlagAdapter.getItem(position);
                    if (position != mFlagAdapter.getCount() && !oldStateCode.equals(state) && initial[0] > 0) {
                        // Show prompt dialog.
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.flag_confirm_case_title)
                                .setMessage(R.string.flag_confirm_case_message)
                            .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    updateState(state, position);
                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    reverseState();
                                }
                            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    reverseState();
                                }
                            }).create().show();


                    } else {
                        // do nothings.
                    }

                    initial[0] = 1;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return view;


    }

    public class HintAdapter extends ArrayAdapter<String> {

        public HintAdapter(Context context, String[] objects) {
            super(context, android.R.layout.simple_list_item_1, android.R.id.text1, objects);
        }

        @Override
        public int getCount() {
            // don't display last item. It is used as hint.
            int count = super.getCount();
            return count > 0 ? count - 1 : count;
        }

        public int getRealCount() {
            return super.getCount();
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.flag_spinner_item, parent, false);
            }

            State state = states.get(getItem(position));
            ImageView iconView = (ImageView) view.findViewById(R.id.flag_icon);
            iconView.setImageResource(state.getColor());

            TextView textView = (TextView) view.findViewById(R.id.flag_name);
            textView.setText(state.getName());
            FontUtil.overrideFonts(getContext(), textView);

            return view;
        }

    }

    private void updateState(final String stateCode, final int position) {
        Long state = reportStateDataSource.getIdByReportTypeAndCode(reportTypeId, stateCode);

       final Activity currentActivity = getActivity();
        ReportService.StateAsyncTask task = new ReportService.StateAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                if (resp.getStatusCode() == 200) {
                    oldStateCode = stateCode;
                    oldStateCodePosition = position;

                    ((ReportViewActivity) currentActivity).changeReportState(stateCode);

                } else {
                    reverseState();

                    if (resp.getStatusCode() == 403) {
                        Crouton.makeText(getActivity(), getString(R.string.set_flag_forbidden), Style.ALERT).show();
                    } else {
                        Crouton.makeText(getActivity(), getString(R.string.set_flag_error), Style.ALERT).show();
                    }
                }
            }
        };

        task.setContext(getContext());
        task.execute(Long.toString(id), Long.toString(state));
    }

    private void reverseState() {
        flagSpinnerView.setSelection(oldStateCodePosition);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {

        if (caseDialog != null && caseDialog.getVisibility() == View.VISIBLE) {
            caseDialog.setVisibility(View.GONE);
            reverseState();
            selectedCaseId = null;
        } else {
            if (mListener != null) {
                mListener.onFragmentInteraction(uri);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        void onFragmentInteraction(Uri uri);
    }
}
