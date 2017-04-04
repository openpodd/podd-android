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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
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
import org.cm.podd.report.activity.ReportTypeActivity;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportQueueDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.FollowAction;
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.Date;

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
    ReportTypeDataSource reportTypeDataSource;
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
        reportTypeDataSource = new ReportTypeDataSource(this.getActivity());
        reportQueueDataSource = new ReportQueueDataSource(this.getActivity());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getListView().setDividerHeight(0);
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
                        mMode = ((AppCompatActivity) getActivity()).startSupportActionMode(
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
        getActivity().registerReceiver(mReceiver, new IntentFilter(DataSubmitService.ACTION_REPORT_STATUS_CHANGE));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_report_list, null);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        TextView emptyText = (TextView) view.findViewById(R.id.empty);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText(R.string.no_report_found_msg);
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);

        FloatingActionButton fabAdd = (FloatingActionButton) view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ReportTypeActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        reportDataSource.close();
        reportQueueDataSource.close();
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
                intent.putExtra("test", report.isTestReport());

                if (report.getFollowFlag() == Report.TRUE) {
                    Form form = reportTypeDataSource.getForm(report.getType());
                    FollowAction action = form.getFollowActionByName(report.getActionName());
                    if (action != null) {
                        intent.putExtra("startPageId", action.getStartPageId());
                    }
                }
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

    private void follow(final long reportId, final long reportType) {
        final Form form = reportTypeDataSource.getForm(reportType);
        if (form.hasFollowActions()) {
            final String[] names = form.getFollowActionNames();
            new AlertDialog.Builder(getActivity()).setTitle(R.string.choose_report_type_title)
                    .setItems(names, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FollowAction action = form.getFollowAction(i);
                            Log.d(TAG, String.format("%s report %d, report type %d", action.getName(), reportId, reportType));
                            Intent intent = new Intent(getActivity(), ReportActivity.class);
                            intent.putExtra("reportType", reportType);
                            intent.putExtra("reportId", reportId);
                            intent.putExtra("follow", true);
                            intent.putExtra("followActionName", action.getName());
                            intent.putExtra("startPageId", action.getStartPageId());
                            startActivity(intent);
                        }
                    }).create().show();
        } else {
            new AlertDialog.Builder(getActivity()).setTitle(R.string.confirm_title)
                    .setMessage(getString(R.string.follow_report_confirm_msg))
                    .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, String.format("follow report %d, report type %d", reportId, reportType));
                            Intent intent = new Intent(getActivity(), ReportActivity.class);
                            intent.putExtra("reportType", reportType);
                            intent.putExtra("reportId", reportId);
                            intent.putExtra("follow", true);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create().show();
        }
    }


    public class ReportCursorAdapter extends CursorAdapter {

        long now;

        public ReportCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            now = new Date().getTime();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            Cursor cursor = (Cursor) getItem(position);
            int followFlag = cursor.getInt(cursor.getColumnIndex("follow_flag"));
            return followFlag;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View retView;
            ViewHolder holder;

            int follow = cursor.getInt(cursor.getColumnIndex("follow_flag"));
            if (follow == Report.FALSE) {
                retView = inflater.inflate(R.layout.report_list_item, parent, false);

                holder = new ViewHolder();
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
                holder.followText = (TextView) retView.findViewById(R.id.report_follow);
                holder.followText.setTypeface(StyleUtil.getSecondTypeface(context.getAssets(), Typeface.NORMAL));

                // cache drawable
                holder.positive = context.getResources().getDrawable(R.drawable.icon_status_good);
                holder.negative = context.getResources().getDrawable(R.drawable.icon_alert);
                holder.follow = context.getResources().getDrawable(R.drawable.icon_flag_follow);
                holder.testReport = context.getResources().getDrawable(R.drawable.icon_flag_ignore);
            } else {
                retView = inflater.inflate(R.layout.report_list_follow_item, parent, false);
                holder = new ViewHolder();
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
                holder.follow = context.getResources().getDrawable(R.drawable.icon_flag_follow);
            }


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
            int follow = cursor.getInt(cursor.getColumnIndex("follow_flag"));
            int testReport = cursor.getInt(cursor.getColumnIndex("test_report"));
            String typeName = cursor.getString(cursor.getColumnIndex("type_name"));
            String actionName = cursor.getString(cursor.getColumnIndex("action_name"));
            Date date = new Date(cursor.getLong(cursor.getColumnIndex("date")));

            if (follow == Report.TRUE) {
                if (actionName == null) {
                    holder.typeText.setText(R.string.follow);
                } else {
                    holder.typeText.setText(actionName);
                }
                date = new Date(cursor.getLong(cursor.getColumnIndex("follow_date")));
                holder.statusImage.setImageDrawable(holder.follow);

            } else {
                if (testReport == Report.TRUE) {
                    holder.typeText.setText(getResources().getText(R.string.test_title) + typeName);
                } else {
                    if (typeName == null) {
                        holder.typeText.setText(R.string.normal_case);
                    } else {
                        holder.typeText.setText(typeName);
                    }
                }

                if (testReport == Report.TRUE) {
                    holder.statusImage.setImageDrawable(holder.testReport);
                } else {
                    if (negative == Report.TRUE) {
                        holder.statusImage.setImageDrawable(holder.negative);
                    } else {
                        holder.statusImage.setImageDrawable(holder.positive);
                    }
                }

                holder.followText.setVisibility(View.GONE);
                if ((draft == Report.FALSE || submit == Report.TRUE ) && follow != Report.TRUE) {
                    long until = cursor.getLong(cursor.getColumnIndex("follow_until"));
                    Log.d(TAG, String.format("now = %d, until = %d", now, until));
                    if (until > now) {
                        Log.d(TAG, String.format("follow button should display"));
                        final long id = cursor.getLong(cursor.getColumnIndex("_id"));
                        final long type = cursor.getLong(cursor.getColumnIndex("type"));
                        holder.followText.setVisibility(View.VISIBLE);
                        holder.followText.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                follow(id, type);
                            }
                        });
                    }
                }
            }

            holder.draftText.setVisibility(
                    draft == Report.TRUE && submit == Report.FALSE ? View.VISIBLE : View.GONE);
            String dateStr = DateUtil.formatLocaleDate(date);
            holder.dateText.setText(dateStr);

            if (submit == Report.FALSE && draft == Report.FALSE) {
                holder.queueImage.setVisibility(View.VISIBLE);
                // submit pending in queue
                ((CheckableLayout) view).setBackgroundSubmitState(false);
            } else {
                holder.queueImage.setVisibility(View.GONE);
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
            Drawable follow;
            Drawable testReport;
            TextView followText;
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
