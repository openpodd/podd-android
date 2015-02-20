package org.cm.podd.report.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.VisualizationAreaActivity;
import org.cm.podd.report.activity.WebContentActivity;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.AdministrationAreaService;
import org.cm.podd.report.service.SyncReportTypeService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.Date;
import java.util.List;

public class VisualizationFragment extends ListFragment {

    private static final String TAG = "VisualizationFragment";

    AdministrationAreaDataSource administrationAreaDataSource;
    private visualizationAdapter adapter;

    public VisualizationFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        administrationAreaDataSource = new AdministrationAreaDataSource(getActivity());
    }

    public void refreshAdapter() {
        if(administrationAreaDataSource.getAll().size() == 0){
            startSyncAdministrationAreaService();
        }

        adapter = new visualizationAdapter(getActivity(), R.layout.list_item_administration_area, administrationAreaDataSource.getAll());
        setListAdapter(adapter);

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        AdministrationArea area = adapter.getItem(position);
        String name = area.getName();
        String parentName = area.getParentName();

        Intent intent = new Intent(getActivity(), VisualizationAreaActivity.class);
        intent.putExtra("parentName", parentName);
        intent.putExtra("name", name);
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
    private class visualizationAdapter extends ArrayAdapter<AdministrationArea> {

        Context context;
        int resource;
        Typeface face;

        public visualizationAdapter(Context context, int resource, List<AdministrationArea> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            face = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(this.resource, parent, false);

            TextView textView = (TextView) view.findViewById(R.id.name);
            textView.setTypeface(face);
            textView.setText(getItem(position).getName());

            boolean isLeaf = getItem(position).getIsLeaf() > 0;
            if(!isLeaf){
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                textView.setTypeface(null, Typeface.BOLD);
                view.setEnabled(false);
            }

            return view;
        }

    }

    private void startSyncAdministrationAreaService() {
        Intent intent = new Intent(getActivity(), AdministrationAreaService.class);
        getActivity().startService(intent);
    }
}
