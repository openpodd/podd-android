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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;

import java.util.List;

/**
 * Created by pphetra on 10/3/14 AD.
 */
public class PageView extends ScrollView {

    private final Page page;

    public PageView(Context context, Page page, boolean readonly) {
        super(context);
        this.page = page;

        ViewGroup.LayoutParams matchParent = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setLayoutParams(matchParent);

        LinearLayout scrollViewContent = new LinearLayout(context);
        scrollViewContent.setLayoutParams(matchParent);
        scrollViewContent.setOrientation(LinearLayout.VERTICAL);

        // only one child as a content for scroll view
        addView(scrollViewContent);

        ViewGroup.LayoutParams wrapContent = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        List<Question> questions = page.getQuestions();
        for (Question q : questions) {
            View qView;
            if (q instanceof MultipleChoiceQuestion) {
                qView = new MultipleChoiceQuestionView(context, (MultipleChoiceQuestion) q, readonly);
            } else {
                qView = new QuestionView(context, q, readonly);
            }
            qView.setLayoutParams(wrapContent);
            scrollViewContent.addView(qView);
        }
    }


    private QuestionView.SoftKeyActionHandler questionActionListener;

    public void setQuestionActionListener(QuestionView.SoftKeyActionHandler questionActionListener) {
        this.questionActionListener = questionActionListener;
        LinearLayout lo = (LinearLayout) getChildAt(0);
        List<Question> questions = page.getQuestions();
        QuestionView lastView = null;
        for (Question q : questions) {
            if (!(q instanceof MultipleChoiceQuestion)) {
                QuestionView qView = (QuestionView) lo.findViewWithTag(q.getName());
                qView.setListener(questionActionListener);
                if (lastView != null) {
                    lastView.setNextFocusDownId(qView.getId());
                }
                lastView = qView;
            }

        }
    }

}
