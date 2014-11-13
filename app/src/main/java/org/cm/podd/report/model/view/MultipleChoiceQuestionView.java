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
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.model.MultipleChoiceItem;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.MultipleChoiceSelection;
import org.cm.podd.report.util.StyleUtil;

import java.util.List;

/**
 * Created by pphetra on 10/3/14 AD.
 */
public class MultipleChoiceQuestionView extends LinearLayout {

    private static final String TAG = "MultipleChoiceQuestionView";
    private MultipleChoiceQuestion question;
    private EditText editText;

    public MultipleChoiceQuestionView(Context context, final MultipleChoiceQuestion question) {
        super(context);
        this.question = question;

        setOrientation(VERTICAL);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        setLayoutParams(params);

        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(params);
        titleView.setTextAppearance(context, R.style.ReportTextLabel);
        titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
        addView(titleView);

        ListView listView = new ListView(context);
        switch (question.getSelectionType()) {
            case SINGLE:
                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                break;
            case MULTIPLE:
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                break;
        }

        MultipleChoiceQuestionAdapter adapter = new MultipleChoiceQuestionAdapter(context, question);
        listView.setAdapter(adapter);
        listView.setLayoutParams(params);
        listView.setDividerHeight(0);

        List<MultipleChoiceItem> items = question.getItems();
        for (int i = 0; i < items.size(); i++) {
            Log.d(TAG, "item id = " + items.get(i).getId() + " , check = " + items.get(i).isChecked());
            listView.setItemChecked(i, items.get(i).isChecked());
        }

        if (question.isFreeTextChoiceEnable()) {
            editText = new EditText(context);
            editText.setLayoutParams(params);
            editText.setText(question.getFreeTextValue());
            if (question.getFreeTextValue() != null && ! question.getFreeTextValue().equals("")) {
                editText.setVisibility(VISIBLE);
            } else {
                editText.setVisibility(INVISIBLE);
            }


            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    question.setFreeTextValue(editable.toString());
                }
            });

            editText.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (! hasFocus) {
                        question.setFreeTextValue(editText.getText().toString());
                    }
                }
            });
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (question.getSelectionType() == MultipleChoiceSelection.SINGLE) {
                    question.clearAllItemChecked();
                    Log.d("---", "clear all checked");
                }

                List items = question.getItems();
                MultipleChoiceItem item = (MultipleChoiceItem) items.get(i);
                CheckedTextView tv = (CheckedTextView) view;
                item.setChecked(tv.isChecked());
                Log.d("---", String.format("click %d, %s, %s, size %d", i, item.getId(), tv.isChecked(), items.size()));

                if (question.isFreeTextChoiceEnable()) {
                    MultipleChoiceItem freeTextItem = (MultipleChoiceItem) items.get(items.size() - 1);
                    if (freeTextItem.isChecked()) {
                        editText.setVisibility(VISIBLE);
                    } else {
                        editText.setVisibility(INVISIBLE);
                        editText.setText("");
                    }
                }
            }
        });

        addView(listView);
        if (editText != null) {
            addView(editText);
        }


    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // HACK! TAKE THAT ANDROID!
        // Calculate entire height by providing a very large height hint.
        // View.MEASURED_SIZE_MASK represents the largest height possible.
        int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK,
                View.MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);

        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = getMeasuredHeight();
    }
}
