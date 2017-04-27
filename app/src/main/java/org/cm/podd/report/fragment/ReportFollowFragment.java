package org.cm.podd.report.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.model.ReportAdapter;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReportFollowFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReportFollowFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReportFollowFragment extends Fragment implements ReportAdapter.OnItemClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private final static String TAG = "ReportFollowFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private Long parentReportId;
    private Long parentReportFlag;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ReportAdapter mAdapter;
    protected ProgressBar mProgressBar;

    protected TextView empty;

    public ReportFollowFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReportFollowFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportFollowFragment newInstance(String param1, String param2) {
        ReportFollowFragment fragment = new ReportFollowFragment();
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

        ((ReportViewActivity)getActivity()).showProgressBar();

        View view = inflater.inflate(R.layout.fragment_report_follow, null);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list_follow_up);
        mLayoutManager = new LinearLayoutManager(getActivity());

        mAdapter = new ReportAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);

        empty = (TextView) view.findViewById(R.id.empty);

        String _report = getArguments().getString("report");
        try {
            JSONObject report = new JSONObject(_report);
            try {
                parentReportId = report.getLong("id");
                parentReportFlag = report.getLong("flag");
            } catch (JSONException e) {

            }

        } catch (JSONException e) {
            // nothing.
        }

        if (parentReportId != null) {
            fetchFollowUpReports(parentReportId);
        } else {
            empty.setVisibility(View.VISIBLE);
            ((ReportViewActivity)getActivity()).showProgressBar();
        }

        FontUtil.overrideFonts(getContext(), view);
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    private void selectItem(int position) {
        Log.d(TAG, "Clicked on position: " + Integer.toString(position));

        Intent intent = new Intent(getActivity(), ReportViewActivity.class);
        intent.putExtra("id", mAdapter.mDataSet.get(position).getItemId());
        startActivity(intent);
    }

    private void fetchFollowUpReports(final Long reportId) {
        final ReportViewActivity currentActivity = ((ReportViewActivity)getActivity());
        ReportService.FollowUpAsyncTask task = new ReportService.FollowUpAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                super.onPostExecute(resp);

                FeedItemDataSource feedItemDataSource = new FeedItemDataSource(getContext());
                FeedItem reportItem = feedItemDataSource.loadByItemId(reportId);
                try {
                    if (reportItem != null & reportItem.getFollow() != null) {
                        JSONArray items = new JSONArray(reportItem.getFollow());
                        ArrayList<FeedItem> feedItems = new ArrayList<FeedItem>();

                        for (int i = 0; i != items.length(); ++i) {
                            JSONObject item = items.getJSONObject(i);

                            String flagString = item.getString("flag");
                            int flag = flagString.equals("") ? 0 : Integer.parseInt(flagString);

                            FeedItem feedItem = new FeedItem();
                            feedItem.setItemId(item.getLong("id"));
                            feedItem.setType("report");

                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                                    Locale.getDefault());
                            feedItem.setDate(formatter.parse(item.getString("date")));
                            feedItem.setExplanation(item.toString());

                            if (parentReportFlag != null && parentReportFlag == 4) {
                                if (flag == 5) {
                                    feedItems.add(feedItem);
                                }
                            } else {
                                feedItems.add(feedItem);
                            }
                        }

                        mAdapter.mDataSet = feedItems;
                        mAdapter.notifyDataSetChanged();

                        if (feedItems.size() == 0) {
                            empty.setVisibility(View.VISIBLE);
                        }
                    } else {
                        empty.setVisibility(View.VISIBLE);
                    }

                } catch (ParseException e) {
                    Log.e(TAG, "Date is not valid, skipping");
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON data", e);
                }

                currentActivity.hideProgressBar();
            }
        };
        task.setContext(getActivity());
        task.execute(Long.toString(reportId));
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

    @Override
    public void onClick(View view, int position) {
        selectItem(position);
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
