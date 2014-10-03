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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Question;

/**
 * Created by pphetra on 9/30/14 AD.
 */
public class QuestionView extends LinearLayout {

    private final Question question;

    public QuestionView(Context context, Question q) {
        super(context);
        this.question = q;

        setOrientation(VERTICAL);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);

        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(params);
        addView(titleView);

        EditText editView = new EditText(context);
        editView.setLayoutParams(params);
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
    }

}
