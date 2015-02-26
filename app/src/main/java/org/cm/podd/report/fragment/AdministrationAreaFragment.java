package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.SearchView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.VisualizationAreaActivity;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.service.AdministrationAreaService;
import org.cm.podd.report.util.StyleUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AdministrationAreaFragment extends ListFragment {

    private static final String TAG = "VisualizationFragment";

    AdministrationAreaDataSource administrationAreaDataSource;
    private administrationAreaAdapter adapter;
    private  EditText editText;
    public AdministrationAreaFragment() {
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
        adapter = new administrationAreaAdapter(getActivity(), R.layout.list_item_administration_area, administrationAreaDataSource.getAll());
        setListAdapter(adapter);

        try {
            String search = editText.getText().toString();
            adapter.getFilter().filter(search.toString());
        }catch (Exception ex){}

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final AdministrationArea area = adapter.getItem(position);
        boolean isLeaf = area.getIsLeaf() > 0;
        if(isLeaf) {
            DatePickerDialog.OnDateSetListener callback = new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Long administrationAreaId = area.getId();
                    String name = area.getName();
                    String parentName = area.getParentName();

                    Intent intent = new Intent(getActivity(), VisualizationAreaActivity.class);
                    intent.putExtra("month", monthOfYear + 1);
                    intent.putExtra("year", year);
                    intent.putExtra("parentName", parentName);
                    intent.putExtra("name", name);
                    intent.putExtra("id", administrationAreaId);
                    startActivity(intent);
                }

            };
            DatePickerDialog datePickerDialog = createDialogWithoutDateField(callback);
            datePickerDialog.show();
        }
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
        View view = inflater.inflate(R.layout.fragment_administration_area, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        TextView emptyText = (TextView) view.findViewById(android.R.id.empty);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบพื้นที่");
        emptyText.setVisibility(View.GONE);
        listView.setEmptyView(emptyText);

        final SearchView searchView = (SearchView) view.findViewById(R.id.searchView);
        searchView.setQueryHint(getString(R.string.search));
        searchView.setIconified(false);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    searchView.setFocusable(true);
                    searchView.setIconified(false);
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                try {
                    adapter.getFilter().filter(newText.toString());
                }catch (Exception ex){}

                return false;
            }
        });

        return view;
    }

    /**
     * List Adapter
     */
    private class administrationAreaAdapter extends ArrayAdapter<AdministrationArea> implements Filterable {

        Context context;
        int resource;
        Typeface face;
        List<AdministrationArea> originalData;
        List<AdministrationArea> filteredData;
        ItemFilter mFilter = new ItemFilter();

        public int getCount() {
            return filteredData.size();
        }

        public AdministrationArea getItem(int position) {
            return filteredData.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public administrationAreaAdapter(Context context, int resource, List<AdministrationArea> originalData) {
            super(context, resource, originalData);
            this.context = context;
            this.resource = resource;
            this.originalData = originalData;
            this.filteredData = originalData;
            face = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(this.resource, parent, false);

            TextView textView = (TextView) view.findViewById(R.id.name);
            textView.setTypeface(face);
            textView.setText(getItem(position).getName());
            textView.setPadding(20, 10, 0, 10);

            boolean isLeaf = getItem(position).getIsLeaf() > 0;
            if(!isLeaf){
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                textView.setPadding(20, 5, 0, 5);
                textView.setTypeface(face, Typeface.BOLD);

                view.setEnabled(false);
                view.setBackgroundColor(getResources().getColor(R.color.list_item_header_bg));
            }

            return view;
        }

        public Filter getFilter() {
            return mFilter;
        }

        private class ItemFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                String filterString = constraint.toString().toLowerCase();

                FilterResults results = new FilterResults();

                final List<AdministrationArea> list = originalData;

                int count = list.size();
                final ArrayList<AdministrationArea> filterData = new ArrayList<AdministrationArea>(count);

                for (int i = 0; i < count; i++) {
                    AdministrationArea item = list.get(i);
                    if ((item.getName() + " " + item.getParentName()).contains(filterString)) {
                        filterData.add(item);
                    }
                }

                results.values = filterData;
                results.count = filterData.size();

                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredData = (ArrayList<AdministrationArea>) results.values;
                notifyDataSetChanged();
            }

        }
    }

    private DatePickerDialog createDialogWithoutDateField(DatePickerDialog.OnDateSetListener callback){
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) - 1;

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), callback, year, month, 1);
        datePickerDialog.setTitle(getString(R.string.title_picker_choose_month));
        try{
            Field[] datePickerDialogFields = datePickerDialog.getClass().getDeclaredFields();
            for (Field datePickerDialogField : datePickerDialogFields) {
                if (datePickerDialogField.getName().equals("mDatePicker")) {
                    datePickerDialogField.setAccessible(true);
                    DatePicker datePicker = (DatePicker) datePickerDialogField.get(datePickerDialog);

                    Field[] datePickerFields = datePickerDialogField.getType().getDeclaredFields();

                    for (Field datePickerField : datePickerFields) {
                        Log.i(TAG, datePickerField.getName());
                        if ("mDaySpinner".equals(datePickerField.getName())) {
                            datePickerField.setAccessible(true);
                            Object dayPicker = new Object();
                            dayPicker = datePickerField.get(datePicker);
                            ((View) dayPicker).setVisibility(View.GONE);
                        }
                    }
                }

            }
        }catch(Exception ex){
        }
        return datePickerDialog;

    }

    private void startSyncAdministrationAreaService() {
        Intent intent = new Intent(getActivity(), AdministrationAreaService.class);
        getActivity().startService(intent);
    }
}
