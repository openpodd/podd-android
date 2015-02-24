package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.VisualizationActivity;
import org.cm.podd.report.db.AdministrationAreaDataSource;
import org.cm.podd.report.model.AdministrationArea;
import org.cm.podd.report.service.AdministrationAreaService;
import org.cm.podd.report.util.StyleUtil;

import java.util.Calendar;
import java.util.List;

public class AdministrationAreaFragment extends ListFragment {

    private static final String TAG = "VisualizationFragment";

    AdministrationAreaDataSource administrationAreaDataSource;
    private administrationAreaAdapter adapter;

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
//        if(administrationAreaDataSource.getAll().size() == 0){
//            startSyncAdministrationAreaService();
//        }

        adapter = new administrationAreaAdapter(getActivity(), R.layout.list_item_administration_area, administrationAreaDataSource.getAll());
        setListAdapter(adapter);

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

                    Intent intent = new Intent(getActivity(), VisualizationActivity.class);
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

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่พบพื้นที่");
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);
        parent.addView(emptyText);

        EditText editText = (EditText) view.findViewById (R.id.editTxt);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i(TAG, "Text [" + s + "]");
                if(!s.equals(null))
                    adapter.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return view;
    }

    /**
     * List Adapter
     */
    private class administrationAreaAdapter extends ArrayAdapter<AdministrationArea> {

        Context context;
        int resource;
        Typeface face;

        public administrationAreaAdapter(Context context, int resource, List<AdministrationArea> objects) {
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
                textView.setTypeface(face, Typeface.BOLD);
                view.setEnabled(false);
            }

            return view;
        }

    }

    private DatePickerDialog createDialogWithoutDateField(DatePickerDialog.OnDateSetListener callback){
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) - 1;

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), callback, year, month, 1);
        datePickerDialog.setTitle(getString(R.string.title_picker_choose_month));
        try{
            java.lang.reflect.Field[] datePickerDialogFields = datePickerDialog.getClass().getDeclaredFields();
            for (java.lang.reflect.Field datePickerDialogField : datePickerDialogFields) {
                if (datePickerDialogField.getName().equals("mDatePicker")) {
                    datePickerDialogField.setAccessible(true);
                    DatePicker datePicker = (DatePicker) datePickerDialogField.get(datePickerDialog);
                    java.lang.reflect.Field[] datePickerFields = datePickerDialogField.getType().getDeclaredFields();
                    for (java.lang.reflect.Field datePickerField : datePickerFields) {
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
