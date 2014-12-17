package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ReportActivity;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.Date;

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

    private ActionMode mMode;

    ReportDataSource reportDataSource;
    ReportQueueDataSource reportQueueDataSource;
    private ReportCursorAdapter adapter;

    private boolean skipRefreshAdapter = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Receiving action " + intent.getAction());
            refreshAdapter();
            if (mMode != null) {
                mMode.finish();
            }
        }
    };

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
        reportQueueDataSource = new ReportQueueDataSource(this.getActivity());
        getActivity().registerReceiver(mReceiver, new IntentFilter(DataSubmitService.ACTION_REPORT_STATUS_CHANGE));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getListView().setDivider(new ColorDrawable(getResources().getColor(R.color.report_row_divider)));
        getListView().setDividerHeight(1);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mMode == null) {

                    if (isReportItemSubmit(position)) {
                        // uncheck item on submitted report
                        getListView().setItemChecked(position, false);

                    } else {
                        // uncheck all except the one selected
                        for (int i = 0; i < getListView().getAdapter().getCount(); i++) {
                            if (i != position) {
                                getListView().setItemChecked(i, false);
                            }
                        }
                        mMode = ((ActionBarActivity) getActivity()).startSupportActionMode(
                                new ActionModeCallback(getActivity()));
                    }
                }
                return false;
            }
        });
        // multiple choice mode is active when entering action mode,
        // and becomes single choice mode when finish action mode
        getListView().setChoiceMode(GridView.CHOICE_MODE_SINGLE);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume skipRefreshAdapter = " + skipRefreshAdapter);
        if (! skipRefreshAdapter) {
            Log.d(TAG, "refresh adapter");
            refreshAdapter();
            getListView().clearChoices();
            if (mMode != null) {
                mMode.finish();
            }
        }
        skipRefreshAdapter = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ยังไม่มีการรายงาน\nกดปุ่ม + เพื่อเริ่มรายงาน ");
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);
        parent.addView(emptyText);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        reportDataSource.close();
        reportQueueDataSource.close();
        getActivity().unregisterReceiver(mReceiver);
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

        if (mMode == null) {
            Report report = reportDataSource.getById(reportId);
            Log.d(TAG, "onReportSelect " + reportId + " type = " + report.getType());
            if (report.getNegative() == report.TRUE) {
                Intent intent = new Intent(getActivity(), ReportActivity.class);
                intent.putExtra("reportType", report.getType());
                intent.putExtra("reportId", reportId);
                startActivityForResult(intent, REQUEST_FOR_EDIT);
            } else {
                // do nothing
            }
        } else {
            if (isReportItemSubmit(position)) {
                // uncheck item if is submit
                getListView().setItemChecked(position, false);

            } else {
                SparseBooleanArray checked = getListView().getCheckedItemPositions();

                if (checked != null) {
                    boolean hasCheckedElement = false;
                    for (int i = 0 ; i < checked.size() && ! hasCheckedElement ; i++) {
                        hasCheckedElement = checked.valueAt(i);
                    }

                    if (hasCheckedElement) {
                        mMode.invalidate();
                    } else {
                        mMode.finish();
                    }
                }
            }
        }
    }

    private boolean isReportItemSubmit(int position) {
        Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
        cursor.moveToPosition(position);
        int submit = cursor.getInt(cursor.getColumnIndex("submit"));
        return submit == Report.TRUE;
    }

    private void deleteReport() {
        final long[] ids = getListView().getCheckedItemIds();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog alertDialog = builder.setTitle(R.string.title_delete_report)
                .setMessage(R.string.message_confirm_delete_report)
                .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        for (int i = 0; i < ids.length; i++) {
                            long id = ids[i];
                            Report report = reportDataSource.getById(id);
                            int draft = report.getDraft();
                            int submit = report.getSubmit();
                            boolean delete = false;
                            Log.d(TAG, String.format("Select report to delete id=%d, draft=%d, submit=%d", id, draft, submit));

                            if (draft == Report.TRUE) {
                                // draft report
                                delete = true;
                            } else {
                                if (draft == Report.FALSE && submit == Report.FALSE) {
                                    // submit pending report
                                    delete = true;
                                }
                            }
                            if (delete) {
                                reportDataSource.deleteReport(id);
                                reportDataSource.deleteImagesByReportId(id);
                                reportQueueDataSource.deleteByReportId(id);
                                Log.d(TAG, "Report and images and queue deleted !");
                            }
                        }
                        refreshAdapter();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();

        alertDialog.show();
    }

    private void refreshAdapter() {
        adapter = new ReportCursorAdapter(getActivity(), reportDataSource.getAllWithTypeName());
        setListAdapter(adapter);
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
            holder.typeText.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));

            holder.dateText = (TextView) retView.findViewById(R.id.report_date);
            // update fontface
            holder.dateText.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));

            holder.draftText = (TextView) retView.findViewById(R.id.report_draft);
            // update fontface
            holder.draftText.setTypeface(StyleUtil.getSecondTypeface(context.getAssets(), Typeface.NORMAL));

            holder.queueImage = (ImageView) retView.findViewById(R.id.report_queue);

            // cache drawable
            holder.positive = context.getResources().getDrawable(R.drawable.icon_status_good);
            holder.negative = context.getResources().getDrawable(R.drawable.icon_alert);

            CheckableLayout checkableView = new CheckableLayout(context);
            checkableView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            checkableView.addView(retView);
            checkableView.setTag(holder);

            return checkableView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            int draft = cursor.getInt(cursor.getColumnIndex("draft"));
            int submit = cursor.getInt(cursor.getColumnIndex("submit"));
            int negative = cursor.getInt(cursor.getColumnIndex("negative"));

            String typeName = cursor.getString(cursor.getColumnIndex("type_name"));
            if (typeName == null) {
                holder.typeText.setText(R.string.normal_case);
            } else {
                holder.typeText.setText(typeName);
            }
            holder.draftText.setVisibility(
                    draft == Report.TRUE ? View.VISIBLE : View.INVISIBLE);

            Date date = new Date(cursor.getLong(cursor.getColumnIndex("date")));
            String dateStr = DateUtil.convertToThaiDate(date);
            holder.dateText.setText(dateStr);

            if (negative == Report.TRUE) {
                holder.statusImage.setImageDrawable(holder.negative);
            } else {
                holder.statusImage.setImageDrawable(holder.positive);
            }

            if (submit == Report.FALSE && draft == Report.FALSE) {
                holder.queueImage.setVisibility(View.VISIBLE);
                // submit pending in queue
                ((CheckableLayout) view).setBackgroundSubmitState(false);
            } else {
                holder.queueImage.setVisibility(View.INVISIBLE);
                ((CheckableLayout) view).setBackgroundSubmitState(true);
            }

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

    /**
     * Checkable wrapper to toggle multiple selection state
     */
    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;
        private boolean mSubmit;

        public CheckableLayout(Context context) {
            super(context);
        }

        @SuppressWarnings("deprecation")
        public void setChecked(boolean checked) {
            mChecked = checked;
            int bgColor;
            if (checked) {
                bgColor = R.color.report_row_selected_bg;
            } else {
                bgColor = mSubmit ? R.color.white : R.color.report_row_bg;
            }
            setBackgroundColor(getResources().getColor(bgColor));
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }

        public void setBackgroundSubmitState(boolean submit) {
            mSubmit = submit;
        }
    }

    /**
     * Callback to CAB (Contextual Action Bar)
     */
    private final class ActionModeCallback implements ActionMode.Callback {
        Context context;
        public ActionModeCallback(Context context) {
            this.context = context;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create the menu from the xml file
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.image_fragment_contextual_actions, menu);

            // use custom text as title
            // on pre honeycomb (<11), cab title background is action bar background (red in drme)
            TextView tv = new TextView(getActivity());
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setText("cab");
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            tv.setBackgroundColor(context.getResources().getColor(R.color.action_bar_bg));
            mode.setCustomView(tv);

            getListView().setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int selected = getListView().getCheckedItemCount();
            TextView title = ((TextView) mode.getCustomView());
            title.setText(getString(R.string.title_report_item_selected, new Object[]{selected}));
            title.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Destroying action mode, let's unselect all items
            for (int i = 0; i < getListView().getAdapter().getCount(); i++) {
                getListView().setItemChecked(i, false);
            }
            getListView().clearChoices();
            getListView().setChoiceMode(GridView.CHOICE_MODE_SINGLE);

            if (mode == mMode) {
                mMode = null;
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.cab_action_delete:
                    deleteReport();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }
    };
}
