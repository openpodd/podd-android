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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import org.cm.podd.report.model.MultipleChoiceItem;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.MultipleChoiceSelection;
import org.cm.podd.report.util.StyleUtil;

/**
 * Created by pphetra on 10/3/14 AD.
 */
public class MultipleChoiceQuestionAdapter extends BaseAdapter {

    private final MultipleChoiceQuestion question;
    private final Context context;

    public MultipleChoiceQuestionAdapter(Context context, MultipleChoiceQuestion question) {
        this.context = context;
        this.question = question;
    }

    @Override
    public int getCount() {
        return question.getItems().size();
    }

    @Override
    public Object getItem(int i) {
        return question.getItems().get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(org.cm.podd.report.R.layout.multi_line_item, viewGroup, false);
        }
        CheckedTextView ctv = (CheckedTextView) view;
        ctv.setSingleLine(false);

        // Remove android checkbox
        ctv.setCheckMarkDrawable(0);
        if (question.getSelectionType() == MultipleChoiceSelection.MULTIPLE) {
            ctv.setCompoundDrawablesWithIntrinsicBounds(org.cm.podd.report.R.drawable.checkbox_multiple_state, 0, 0, 0);
        } else {
            ctv.setCompoundDrawablesWithIntrinsicBounds(org.cm.podd.report.R.drawable.checkbox_single_state, 0, 0, 0);
        }
        MultipleChoiceItem item = (MultipleChoiceItem) getItem(i);
        ctv.setText(item.getText().replaceAll("\\\\n", "\n"));
        ctv.setPadding(0, 0, 0, 24);
        ctv.setTextAppearance(this.context, org.cm.podd.report.R.style.ReportTextLabel);
        ctv.setTypeface(StyleUtil.getDefaultTypeface(this.context.getAssets(), Typeface.NORMAL));

        return ctv;
    }


}
