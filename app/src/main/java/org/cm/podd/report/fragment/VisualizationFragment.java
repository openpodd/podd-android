package org.cm.podd.report.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.WebContentActivity;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VisualizationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VisualizationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VisualizationFragment extends ListFragment {

    private static final String TAG = "VisualizationFragment";

    private visualizationCursorAdapter adapter;


    public VisualizationFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void refreshAdapter() {
        adapter = new visualizationCursorAdapter(getActivity(), null, false);
        setListAdapter(adapter);

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor cursor = (Cursor) adapter.getItem(position);
        String title = cursor.getString(cursor.getColumnIndex("title"));
        String content = cursor.getString(cursor.getColumnIndex("content"));

        Intent intent = new Intent(getActivity(), WebContentActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("id", id);
        startActivity(intent);
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
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบการประเมิน");
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);
        parent.addView(emptyText);

        return view;
    }

    /**
     * List Adapter
     */
    private class visualizationCursorAdapter extends CursorAdapter {

        private Typeface typeFace;

        private visualizationCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            typeFace = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View rootView = LayoutInflater.from(context).inflate(R.layout.notification_list_item, parent, false);
            return rootView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String title = cursor.getString(cursor.getColumnIndex("title"));
            long time = cursor.getLong(cursor.getColumnIndex("created_at"));
            int seen = cursor.getInt(cursor.getColumnIndex("seen"));

            TextView titleTextView = (TextView) view.findViewById(R.id.title);
            titleTextView.setText(title);
            titleTextView.setTypeface(typeFace);

            TextView dateTextView = (TextView) view.findViewById(R.id.date);
            Date date = new Date(time);
            dateTextView.setText(DateUtil.convertToThaiDateTime(date));
            dateTextView.setTypeface(typeFace);

            TextView newTextView = (TextView) view.findViewById(R.id.new_label);
            newTextView.setTypeface(typeFace);
            newTextView.setVisibility(seen == Report.TRUE ? View.GONE : View.VISIBLE);
        }
    }
}
