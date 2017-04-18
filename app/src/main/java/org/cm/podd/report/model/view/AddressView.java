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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ForgetPasswordActivity;

import org.cm.podd.report.fragment.ForgetPasswordFormFragment;
import org.cm.podd.report.model.Config;
import org.cm.podd.report.model.DataType;
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
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class AddressView extends LinearLayout {

    private final Question question;
    private View [] spinnerViews = null;
    private ArrayAdapter<String> adapter;

    private Context context;
    private Config config;

    private int init = 0;

    private SharedPrefUtil sharedPrefUtil;
    private CustomFilterUtil customFilterUtil;

    private LayoutParams params;
    private String hintText;

    private boolean readonly;

    private String[] fields;
    private EditText editView;

    private ProgressDialog progressDialog;

    public AddressView(final Context context, Question q, boolean readonly) {
        super(context);

        sharedPrefUtil = new SharedPrefUtil(context);
        customFilterUtil = new CustomFilterUtil();

        this.context = context;
        this.question = q;

        new SyncDataTask().execute((Void[]) null);

        hintText = context.getString(R.string.edittext_hint);

        setOrientation(VERTICAL);

        params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        setLayoutParams(params);
        setTag(q.getName());
        setId(q.getId());

        this.readonly = readonly;

        showProgressDialog();
        createAddressView();
    }

    public void createAddressView () {
        String system = "fetchData";
        String key = question.getDataUrl();

        config = sharedPrefUtil.getSyncData(system, key);
        if (init == 0 && config.getValue() == null) {
            return;
        }

        fields = question.getFilterFields().split(",");
        editView = new EditText(context);

        spinnerViews = new View[fields.length];
        for (int idx = 0; idx < fields.length; idx++) {

            CustomFilterUtil.FilterWord [] filterWords = new CustomFilterUtil.FilterWord[idx];
            for (int jdx = 0; jdx < idx; jdx++) {
                if (spinnerViews[jdx] instanceof Spinner) {
                    String _key = fields[jdx];
                    Object _value = ((Spinner) spinnerViews[jdx]).getSelectedItem();
                    String[] values = _key.split("\\|");
                    if (values.length > 1) {
                        _key = values[0].replaceAll(" ", "");
                    }

                    if (_value != null) {
                        CustomFilterUtil.FilterWord word = new CustomFilterUtil.FilterWord(_key, _value.toString());
                        filterWords[jdx] = word;
                    }
                }
            }

            String[] values = fields[idx].split("\\|");

            String header = fields[idx].replaceAll(" ", "");
            String value = fields[idx].replaceAll(" ", "");

            if (values.length > 1) {
                value = values[0].replaceAll(" ", "");
                header = values[1];
            }

            final ArrayList<String> listData;

            if (config.getValue() == null) {
                listData = new ArrayList<String>();
            } else {
                listData = customFilterUtil.getListStringByKey(config.getValue(), value, filterWords);
            }

            TextView headerView = new TextView(context);
            headerView.setLayoutParams(params);
            headerView.setPadding(10, 0, 0, 10);
            headerView.setTextAppearance(context, R.style.ReportTextLabel);
            headerView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
            headerView.setText(header);

            addView(headerView);

            if (listData != null) {
                adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listData);

                Spinner spinnerView =  new Spinner(context);
                spinnerView = new Spinner(context);
                spinnerView.setLayoutParams(params);
                spinnerView.setPadding(0, 0, 0, 0);
                spinnerView.setAdapter(adapter);
                spinnerView.setSelected(true);

                final int finalIdx = idx;
                final Spinner finalSpinnerView = spinnerView;
                spinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        if (init == 0) return;
                        question.setData(getAddressUpdated());

                        // refresh
                        for (int idx = finalIdx + 1; idx < fields.length; idx++) {
                            if (!(spinnerViews[idx] instanceof Spinner)) continue;

                            CustomFilterUtil.FilterWord[] filterWords = new CustomFilterUtil.FilterWord[idx];
                            for (int jdx = 0; jdx < idx; jdx++) {
                                if (spinnerViews[jdx] instanceof Spinner) {
                                    String _key = fields[jdx];
                                    Object _value = ((Spinner)spinnerViews[jdx]).getSelectedItem();
                                    if (_value != null) {
                                        CustomFilterUtil.FilterWord word = new CustomFilterUtil.FilterWord(_key, _value.toString());
                                        filterWords[jdx] = word;
                                    }
                                }
                            }

                            if (config.getValue() == null) {
                                continue;
                            }

                            String value = fields[idx].replaceAll(" ", "");
                            final ArrayList<String> listData = customFilterUtil.getListStringByKey(config.getValue(), value, filterWords);
                            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listData);
                            ((Spinner)spinnerViews[idx]).setAdapter(adapter);
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }

                });

                Object text = question.getValue();
                if (text != null) {
                    for (int i = 0; i < listData.size(); i++) {
                        if (text.toString().toLowerCase().contains(adapter.getItem(i).toLowerCase())) {
                            spinnerView.setSelection(adapter.getPosition(adapter.getItem(i)));
                        }
                    }
                }

                if (readonly) {
                    spinnerView.setEnabled(false);
                }

                spinnerViews[idx] = spinnerView;
                addView(spinnerViews[idx]);
            } else {

                final EditText editView = new EditText(context);
                editView.setLayoutParams(params);
                editView.setPadding(0, 0, 0, 20);
                editView.setClickable(true);
                editView.setHint(hintText);
                editView.setOnTouchListener(new OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        editView.setHint("");
                        return false;
                    }

                });

                Object text = question.getValue();
                if (text != null) {
                    Pattern specificPattern = Pattern.compile("\\[" + header + ":(.*?)\\]");
                    Matcher match = specificPattern.matcher(text.toString());
                    while (match.find()) {
                        value = match.group(1);
                        editView.setText(value);
                    }

                }

                if (readonly) {
                    editView.setFocusable(false);
                    editView.setClickable(false);
                }

                if (!readonly) {
                    editView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            question.setData(getAddressUpdated());
                        }
                    });
                }

                spinnerViews[idx] = editView;
                addView(spinnerViews[idx]);

            }


        }

        hideProgressDialog();
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
                    createAddressView();
                }
                init = 1;

            } else {
                // show error
            }
        }
    }

    public String getAddressUpdated() {
        String value = "";
        String [] findValue = new String[fields.length];
        for (int idx = 0; idx < fields.length; idx++) {
            String[] values = fields[idx].split("\\|");
            String header = fields[idx].replaceAll(" ", "");
            if (values.length > 1) {
                header = values[1];
            }

            if (spinnerViews[idx] instanceof Spinner) {
                Object selected = ((Spinner)spinnerViews[idx]).getSelectedItem();
                if(selected != null) {
                    findValue[idx] = value;
                    value += "[" + header + ":" +selected.toString() + "]";
                }
            } else {
                if (!((EditText) spinnerViews[idx]).getText().toString().equalsIgnoreCase("")) {
                    value += "[" + header + ":" + ((EditText) spinnerViews[idx]).getText().toString() + "]";
                }
            }

        }

        if (!editView.getText().toString().equalsIgnoreCase("")) {
            value += "[specific:" + editView.getText().toString() + "]";
        }
        return value;
    }

    public void showProgressDialog() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(R.string.request_fetching_data);
        progressDialog.setMessage(context.getString(R.string.request_please_wait));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

}
