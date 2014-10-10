package org.cm.podd.report.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ReportActivity;
import org.cm.podd.report.db.ReportDataSource;

import java.util.Date;
import java.util.Map;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link OnReportSelectListener}
 * interface.
 */
public class ReportListFragment extends ListFragment {

    private static final String TAG = "ReportListFragment";
    public static final int REQUEST_FOR_EDIT = 1;
    OnReportSelectListener mListener;

    ReportDataSource reportDataSource;
    private ReportCursorAdapter adapter;

    private boolean skipRefreshAdapter = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReportListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportDataSource = new ReportDataSource(this.getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume skipRefreshAdapter = " + skipRefreshAdapter);
        if (! skipRefreshAdapter) {
            Log.d(TAG, "refresh adapter");
            adapter = new ReportCursorAdapter(this.getActivity(), reportDataSource.getAll());
            setListAdapter(adapter);
        }
        skipRefreshAdapter = false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnReportSelectListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOR_EDIT) {
            skipRefreshAdapter = true;
        }
        Log.d(TAG, "request code = " + requestCode + " result code = " + resultCode);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long reportId) {
        super.onListItemClick(l, v, position, reportId);

        Map result = reportDataSource.getById(reportId);
        Log.d(TAG, "onReportSelect " + reportId + " type = " + result.get("type"));
        Intent intent = new Intent(getActivity(), ReportActivity.class);
        intent.putExtra("reportType", (Long) result.get("type")); // mock
        intent.putExtra("reportId", reportId);
        startActivityForResult(intent, REQUEST_FOR_EDIT);

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
    public interface OnReportSelectListener {

    }


    public class ReportCursorAdapter extends CursorAdapter {

        public ReportCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View retView = inflater.inflate(R.layout.report_list_item, parent, false);

            return retView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(R.id.report_date);
            StringBuilder builder = new StringBuilder();
            builder.append(cursor.getLong(cursor.getColumnIndex("_id")));
            Date date = new Date(cursor.getLong(cursor.getColumnIndex("date")));
            builder.append(" ").append(date);
            builder.append(" ").append(cursor.getInt(cursor.getColumnIndex("draft")));
            builder.append(" ").append(cursor.getInt(cursor.getColumnIndex("submit")));
            tv.setText(builder.toString());
        }
    }

}
