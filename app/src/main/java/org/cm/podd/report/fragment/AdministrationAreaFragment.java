package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
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
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.service.AdministrationAreaService;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.StyleUtil;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AdministrationAreaFragment extends ListFragment {

    private static final String TAG = "AreaFragment";

    AdministrationAreaDataSource administrationAreaDataSource;
    Typeface face;

    private administrationAreaAdapter adapter;
    private  EditText editText;

    public AdministrationAreaFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    protected BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter = new administrationAreaAdapter(getActivity(), R.layout.list_item_administration_area, administrationAreaDataSource.getAll());
            setListAdapter(adapter);
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        administrationAreaDataSource = new AdministrationAreaDataSource(getActivity());
        face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);

        getActivity().registerReceiver(mSyncReceiver, new IntentFilter(AdministrationAreaService.SYNC));

        if (RequestDataUtil.hasNetworkConnection(getActivity())) {
            startSyncAdministrationAreaService();
        }
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
            StaticTitleDatePickerDialog datePickerDialog = createDialogWithoutDateField(new DatePickerDialog.OnDateSetListener() {

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
            });
            datePickerDialog.show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mSyncReceiver);
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
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(this.resource, parent, false);

            TextView textLeafView = (TextView) view.findViewById(R.id.nameLeaf);
            TextView textParentView = (TextView) view.findViewById(R.id.nameParent);

            boolean isLeaf = getItem(position).getIsLeaf() > 0;
            if(!isLeaf){
                textLeafView.setVisibility(View.GONE);
                textParentView.setVisibility(View.VISIBLE);

                textParentView.setTypeface(face, Typeface.BOLD);
                textParentView.setText(getItem(position).getName());

                view.setEnabled(false);
                view.setBackgroundColor(getResources().getColor(R.color.list_item_header_bg));
            }else{
                textLeafView.setVisibility(View.VISIBLE);
                textParentView.setVisibility(View.GONE);

                textLeafView.setTypeface(face);
                textLeafView.setText(getItem(position).getName());
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

    private StaticTitleDatePickerDialog createDialogWithoutDateField(DatePickerDialog.OnDateSetListener callback){
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) - 1;

        StaticTitleDatePickerDialog datePickerDialog = new StaticTitleDatePickerDialog(getActivity(), callback, year, month, 1,
                getString(R.string.title_picker_choose_month));

        return datePickerDialog;

    }

    class StaticTitleDatePickerDialog extends DatePickerDialog {
        String title;
        public StaticTitleDatePickerDialog(Context context, OnDateSetListener callBack,
                                           int year, int monthOfYear, int dayOfMonth, String title) {
            super(context, callBack, year, monthOfYear, dayOfMonth);
            this.title = title;
            setTitle(title);

            DatePicker datePicker = getDatePicker();
            try{
                Field[] datePickerFields = getDatePicker().getClass().getDeclaredFields();

                for (Field datePickerField : datePickerFields) {
                    Log.i(TAG, datePickerField.getName());

                    if ("mDaySpinner".equals(datePickerField.getName())) {
                        datePickerField.setAccessible(true);
                        Object dayPicker = new Object();
                        dayPicker = datePickerField.get(datePicker);
                        ((View) dayPicker).setVisibility(View.GONE);
                    }
                }
                Date minDate = null;
                Date maxDate = null;
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    minDate = format.parse(getString(R.string.min_date));
                    maxDate = format.parse(year + "-" + (monthOfYear + 1) + "-01");
                    datePicker.setMinDate(minDate.getTime());
                    datePicker.setMaxDate(maxDate.getTime());
                } catch (ParseException e) {
                    Log.e(TAG, e.toString());
                }

            }catch(Exception ex){
                Log.e(TAG, ex.toString());
            }
        }

        @Override
        public void onDateChanged(DatePicker view, int year,
                                  int month, int day) {
            super.onDateChanged(view, year, month, day);
            setTitle(title);
        }

    }

    private void startSyncAdministrationAreaService() {
        Intent intent = new Intent(getActivity(), AdministrationAreaService.class);
        getActivity().startService(intent);
    }
}
