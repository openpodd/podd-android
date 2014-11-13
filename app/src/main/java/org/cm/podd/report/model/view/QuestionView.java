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
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.util.StyleUtil;

/**
 * Created by pphetra on 9/30/14 AD.
 */
public class QuestionView extends LinearLayout {

    private final Question question;

    public QuestionView(Context context, Question q) {
        super(context);
        this.question = q;

        setOrientation(VERTICAL);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        setLayoutParams(params);
        setTag(q.getName());
        setId(q.getId());

        ViewGroup.LayoutParams itemParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(itemParams);
        titleView.setTextAppearance(context, R.style.ReportTextLabel);
        titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
        addView(titleView);

        final EditText editView = new EditText(context);
        editView.setLayoutParams(params);
        editView.setPadding(0,0,0,0);
        editView.setTextAppearance(context, R.style.EditTextFlat);
        editView.setBackgroundResource(R.drawable.ab_solid_white_podd);
        editView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
        int type = 0;
        if (question.getDataType() == DataType.INTEGER) {
            type = type | InputType.TYPE_CLASS_NUMBER;
        }
        if (question.getDataType() == DataType.DOUBLE) {
            type = type | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        }
        if (question.getDataType() == DataType.STRING) {
            type = type | InputType.TYPE_CLASS_TEXT;
        }
        editView.setInputType(type);
        Object value = question.getValue();
        if (value != null) {
            editView.setText(value.toString());
        }
        addView(editView);

        editView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                question.setData(question.getDataType().parseFromString(editable.toString()));
            }
        });

        editView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (! hasFocus) {
                    question.setData(question.getDataType().parseFromString(editView.getText().toString()));
                }
            }
        });

        editView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (listener != null) {
                    return listener.onSoftKeyAction(v, actionId, event);
                }
                return false;
            }
        });
    }


    private SoftKeyActionHandler listener;

    public void setListener(SoftKeyActionHandler listener) {
        this.listener = listener;
    }

    public interface SoftKeyActionHandler {
        public boolean onSoftKeyAction(TextView view, int actionId, KeyEvent event);
    }

}
