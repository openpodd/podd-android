package org.cm.podd.report.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ReportActivity;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.util.StyleUtil;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getListView().setDivider(new ColorDrawable(getResources().getColor(R.color.report_row_divider)));
        getListView().setDividerHeight(1);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume skipRefreshAdapter = " + skipRefreshAdapter);
        if (! skipRefreshAdapter) {
            Log.d(TAG, "refresh adapter");
            adapter = new ReportCursorAdapter(this.getActivity(), reportDataSource.getAllWithTypeName());
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
            if (resultCode == Activity.RESULT_OK) {
                skipRefreshAdapter = false;
            } else {
                skipRefreshAdapter = true;
            }
        }
        Log.d(TAG, "request code = " + requestCode + " result code = " + resultCode);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long reportId) {
        super.onListItemClick(l, v, position, reportId);

        Report report = reportDataSource.getById(reportId);
        Log.d(TAG, "onReportSelect " + reportId + " type = " + report.getType());
        if (report.getNegative() == report.TRUE) {
            Intent intent = new Intent(getActivity(), ReportActivity.class);
            intent.putExtra("reportType", report.getType());
            intent.putExtra("reportId", reportId);
            startActivityForResult(intent, REQUEST_FOR_EDIT);
        } else {
            StringBuffer buff = new StringBuffer();
            buff.append("report on ").append(report.getDate()).append("\n");
            buff.append("positive report");
            final Crouton crouton = Crouton.makeText(getActivity(), buff.toString(), Style.INFO);
            crouton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Crouton.hide(crouton);
                }
            });
            crouton.show();
        }

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

            ViewHolder holder = new ViewHolder();
            holder.statusImage = (ImageView) retView.findViewById(R.id.report_status);
            holder.typeText = (TextView) retView.findViewById(R.id.report_type);
            // update fontface
            holder.typeText.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.BOLD));

            holder.dateText = (TextView) retView.findViewById(R.id.report_date);
            // update fontface
            holder.dateText.setTypeface(StyleUtil.getSecondTypeface(context.getAssets(), Typeface.NORMAL));

            holder.draftText = (TextView) retView.findViewById(R.id.report_draft);
            // update fontface
            holder.draftText.setTypeface(StyleUtil.getSecondTypeface(context.getAssets(), Typeface.NORMAL));

            holder.queueImage = (ImageView) retView.findViewById(R.id.report_queue);

            // cache drawable
            holder.positive = context.getResources().getDrawable(R.drawable.icon_status_good);
            holder.negative = context.getResources().getDrawable(R.drawable.icon_status_bad);

            retView.setTag(holder);
            return retView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            int draft = cursor.getInt(cursor.getColumnIndex("draft"));
            int submit = cursor.getInt(cursor.getColumnIndex("submit"));
            int negative = cursor.getInt(cursor.getColumnIndex("negative"));

            String typeName = cursor.getString(cursor.getColumnIndex("type_name"));
            if (typeName == null) {
                holder.typeText.setText(R.string.normal_incident);
            } else {
                holder.typeText.setText(typeName);
            }
            holder.draftText.setVisibility(
                    draft == Report.TRUE ? View.VISIBLE : View.INVISIBLE);

            Date date = new Date(cursor.getLong(cursor.getColumnIndex("date")));
            String dateStr = convertToThaiDate(date);
            holder.dateText.setText(dateStr);

            if (negative == Report.TRUE) {
                holder.statusImage.setImageDrawable(holder.negative);
            } else {
                holder.statusImage.setImageDrawable(holder.positive);
            }

            if (submit == Report.FALSE && draft == Report.FALSE) {
                holder.queueImage.setVisibility(View.VISIBLE);
                view.setBackgroundResource(R.color.report_row_bg);
            } else {
                holder.queueImage.setVisibility(View.INVISIBLE);
                view.setBackgroundResource(R.color.white);
            }

        }

        final String[] THAI_MONTH = {
            "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
            "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."
        };

        private String convertToThaiDate(Date date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dateNum = cal.get(Calendar.DATE);
            int month = cal.get(Calendar.MONTH);
            String thaiMonth = THAI_MONTH[month];
            int year = cal.get(Calendar.YEAR) + 543;
            return String.format("%d %s %d", dateNum, thaiMonth, year);
        }

        class ViewHolder {
            ImageView statusImage;
            TextView typeText;
            TextView dateText;
            TextView draftText;
            ImageView queueImage;
            Drawable positive;
            Drawable negative;
        }
    }

}
