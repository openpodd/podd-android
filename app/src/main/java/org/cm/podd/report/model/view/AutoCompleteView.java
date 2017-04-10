/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cm.podd.report.model.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.model.Config;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.util.CustomFilterUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AutoCompleteView extends LinearLayout {

    private final Question question;
    private AutoCompleteTextView autoCompleteTextView = null;
    private AutocompleteAdapter adapter;

    private Context context;
    private Config config;

    private SharedPrefUtil sharedPrefUtil;

    private CustomFilterUtil customFilterUtil;


    public AutoCompleteView(final Context context, Question q, final boolean readonly) {
        super(context);

        sharedPrefUtil = new SharedPrefUtil(context);
        customFilterUtil = new CustomFilterUtil();

        this.context = context;
        this.question = q;

        new SyncDataTask().execute((Void[]) null);

        final String hintText = context.getString(R.string.edittext_hint);

        setOrientation(VERTICAL);

        final LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        setLayoutParams(params);
        setTag(q.getName());
        setId(q.getId());


        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(params);
        titleView.setTextAppearance(context, R.style.ReportTextLabel);
        titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));

        addView(titleView);


        String system = "fetchData";
        String key = question.getDataUrl();

        config = sharedPrefUtil.getSyncData(system, key);

        String[] values = question.getFilterFields().split("\\|");
        String header = question.getFilterFields().replaceAll(" ", "");
        String filterKey = question.getFilterFields().replaceAll(" ", "");

        if (values.length > 1) {
            filterKey = values[0].replaceAll(" ", "");
            header = values[1];
        }

        final ArrayList<String> listData;

        if (config == null) {
            listData = new ArrayList<String>();
        } else {
            listData = customFilterUtil.getStringByKey(config.getValue(), filterKey, new CustomFilterUtil.FilterWord[0]);
        }

        adapter = new AutocompleteAdapter(context, android.R.layout.simple_spinner_dropdown_item, listData);

        autoCompleteTextView = new AutoCompleteTextView(context);
        autoCompleteTextView.setLayoutParams(params);
        autoCompleteTextView.setAdapter(adapter);
        Object value = question.getValue();
        if (value != null) {
            autoCompleteTextView.setText(value.toString());
        }

        if (readonly) {
            autoCompleteTextView.setFocusable(false);
            autoCompleteTextView.setClickable(false);
        }

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                question.setData(question.getDataType().parseFromString(autoCompleteTextView.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });

        TextView headerView = new TextView(context);
        headerView.setLayoutParams(params);
        headerView.setPadding(10, 0, 0, 10);
        headerView.setText(header);

        addView(headerView);
        addView(autoCompleteTextView);
    }

    private SoftKeyActionHandler listener;
    public void setListener(SoftKeyActionHandler listener) {
        this.listener = listener;
    }


    public interface SoftKeyActionHandler {
        public boolean onSoftKeyAction(TextView view, int actionId, KeyEvent event);
    }

    public class SyncDataTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {

            String system = "fetchData";
            String url = question.getDataUrl();

            String accessToken = sharedPrefUtil.getAccessToken();
            return RequestDataUtil.get(url, null, accessToken);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                String system = "fetchData";
                String key = question.getDataUrl();

                JSONObject response = null;
                try {
                    response = new JSONObject(resp.getRawData());
                    sharedPrefUtil.setSyncData(system, key, response.getString("results"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (config.getValue() == null) {

                    config = sharedPrefUtil.getSyncData(system, key);

                    if (config.getValue() == null) return;

                    String filterKey = question.getFilterFields().replaceAll(" ", "");
                    ArrayList<String> listData = customFilterUtil.getStringByKey(config.getValue(), filterKey, new CustomFilterUtil.FilterWord[0]);

                    adapter = new AutocompleteAdapter(context, android.R.layout.simple_spinner_dropdown_item, listData);
                    autoCompleteTextView.setAdapter(adapter);
                }

            } else {
                // show error
            }
        }
    }

    class StringContainFilter extends Filter {

        AutocompleteAdapter adapter;
        ArrayList<String> originalList;
        ArrayList<String> filteredList;

        public StringContainFilter(AutocompleteAdapter adapter, ArrayList<String> originalList) {
            super();
            this.adapter = adapter;
            this.originalList = (ArrayList<String>) originalList.clone();
            this.filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                final String filterPattern = constraint.toString().toLowerCase().trim();

                for (final String text : originalList) {
                    if (text.contains(filterPattern)) {
                        filteredList.add(text);
                    }
                }
            }
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            adapter.filteredString.clear();
            adapter.filteredString.addAll((ArrayList) results.values);
            adapter.notifyDataSetChanged();
        }
    }

    public class AutocompleteAdapter extends ArrayAdapter<String> implements Filterable {

        Context context;
        ArrayList<String> filteredString = new ArrayList<String>();

        public AutocompleteAdapter(Context context, int resource, ArrayList<String> filteredString) {
            super(context, resource, filteredString);
            this.context = context;
            this.filteredString = filteredString;
        }

        @Override
        public int getCount() {
            return filteredString.size();
        }

        @Override
        public Filter getFilter() {
            return new StringContainFilter(this, filteredString);
        }
    }

}
