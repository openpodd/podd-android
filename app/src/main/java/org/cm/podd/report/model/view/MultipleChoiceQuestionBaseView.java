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
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.util.StyleUtil;

/**
 * Created by pphetra on 9/15/15 AD.
 */
public abstract class MultipleChoiceQuestionBaseView extends LinearLayout {
    protected MultipleChoiceQuestion question;
    protected final LayoutParams layoutParams;

    public MultipleChoiceQuestionBaseView(Context context, final MultipleChoiceQuestion question) {
        super(context);
        this.question = question;

        setOrientation(VERTICAL);

        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 24);
        setLayoutParams(layoutParams);

        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(layoutParams);
        titleView.setTextAppearance(context, R.style.ReportTextLabel);
        titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
        addView(titleView);
    }
}
