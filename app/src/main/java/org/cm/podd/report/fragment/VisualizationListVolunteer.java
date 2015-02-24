package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.VisualizationAreaActivity;
import org.cm.podd.report.activity.VisualizationVolunteerActivity;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.model.Volunteer;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VisualizationListVolunteer extends ListFragment {

    private volunteerAdapter adapter;
    ArrayList<Volunteer> volunteers;

    private int month;
    private int year;

    public VisualizationListVolunteer() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        month = getArguments().getInt("month");
        year = getArguments().getInt("year");

        volunteers = new ArrayList<Volunteer>();
        try {
            JSONArray items = new JSONArray(getArguments().getString("volunteers"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                long volunteerId = item.optLong("id");
                String volunteerName = item.optString("fullName");
                String volunteerGrade = item.optString("grade");
                int volunteerTotalReport = item.optInt("totalReport");
                int volunteerPositiveReport = item.optInt("positiveReport");
                int volunteerNegativeReport = item.optInt("negativeReport");

                Volunteer volunteer = new Volunteer(volunteerId, volunteerName);
                volunteer.setDetail(volunteerTotalReport, volunteerPositiveReport, volunteerNegativeReport, volunteerGrade);

                volunteers.add(volunteer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final Volunteer volunteer = adapter.getItem(position);

        Intent intent = new Intent(getActivity(), VisualizationVolunteerActivity.class);
        intent.putExtra("month", month);
        intent.putExtra("year", year);
        intent.putExtra("id", volunteer.getId());
        intent.putExtra("name", volunteer.getName());
        intent.putExtra("parentName", "");
        startActivity(intent);


    }
    public void refreshAdapter() {
        adapter = new volunteerAdapter(getActivity(), R.layout.list_item_volunteer, volunteers);
        setListAdapter(adapter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_visualization_list_volunteer, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบอาสา");
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);
        parent.addView(emptyText);

        return view;
    }

    /**
     * List Adapter
     */
    private class volunteerAdapter extends ArrayAdapter<Volunteer> {

        Context context;
        int resource;
        Typeface face;

        public volunteerAdapter(Context context, int resource, List<Volunteer> objects) {
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

            return view;
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapter();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

}
