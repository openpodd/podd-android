package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.VisualizationAreaActivity;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.AdministrationAreaService;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.SyncReportTypeService;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CommentFragment extends ListFragment {

    private static final String TAG = "CommentFragment";

    CommentDataSource commentDataSource;
    private commentAdapter adapter;
    private long reportId;

    public CommentFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Comment> items = getAll();
            adapter = new commentAdapter(context, R.layout.list_item_comment, items);
            setListAdapter(adapter);
        }
    };

    private ArrayList<Comment> getAll() {
        ArrayList<Comment> items = new ArrayList<Comment>();
        items.addAll(commentDataSource.getAllFromReport(reportId));
        return items;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportId = getArguments().getLong("reportId", -99);
        commentDataSource = new CommentDataSource(getActivity());

        if (RequestDataUtil.hasNetworkConnection(getActivity())) {
            getActivity().registerReceiver(mSyncReceiver, new IntentFilter(CommentService.SYNC));
        }
    }

    public void refreshAdapter() {
        adapter = new commentAdapter(getActivity(), R.layout.list_item_comment, commentDataSource.getAllFromReport(reportId));
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comment, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบความคิดเห็น");
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);
        parent.addView(emptyText);

        return view;
    }

    /**
     * List Adapter
     */
    private class commentAdapter extends ArrayAdapter<Comment> {

        Context context;
        int resource;
        Typeface face;

        public commentAdapter(Context context, int resource, List<Comment> originalData) {
            super(context, resource, originalData);
            this.context = context;
            this.resource = resource;
            face = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(this.resource, parent, false);

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setTypeface(face);
            textView.setText(getItem(position).getMessage());

            return view;
        }
    }

    private void startSyncCommentService() {
        Intent intent = new Intent(getActivity(), CommentService.class);
        intent.putExtra("reportId", reportId);
        getActivity().startService(intent);
    }
}
