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

package org.cm.podd.report.model.parser;

import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.FollowAction;
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.MultipleChoiceItem;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.MultipleChoiceSelection;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.Transition;
import org.cm.podd.report.model.Trigger;
import org.cm.podd.report.model.validation.IValidation;
import org.cm.podd.report.model.validation.MaxValidation;
import org.cm.podd.report.model.validation.MinValidation;
import org.cm.podd.report.model.validation.RequireValidation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class FormParser {

    private Form form = new Form();

    /**
     * {
     *     startPageId: 1,
     *     questions: [],
     *     pages: [],
     *     transitions: []
     * }
     * @param doc
     * @throws JSONException
     */
    public void parse(JSONObject doc) throws JSONException {
        form.setStartPageId(doc.getInt("startPageId"));

        JSONArray questions = doc.getJSONArray("questions");
        for (int i = 0; i < questions.length(); i++) {
            JSONObject q = questions.getJSONObject(i);
            String type = q.getString("type");
            if (type.equals("integer") ||
                    type.equals("date") ||
                    type.equals("double") ||
                    type.equals("text")) {
                parseQuestion(q);
            } else {
                parseMultipleChoiceQuestion(q);
            }
        }

        JSONArray pages = doc.getJSONArray("pages");
        for (int i = 0; i < pages.length(); i++) {
            JSONObject page = pages.getJSONObject(i);
            parsePage(page);
        }


        JSONArray transitions = doc.getJSONArray("transitions");
        for (int i = 0; i < transitions.length(); i++) {
            JSONObject transition = transitions.getJSONObject(i);
            parseTransition(transition);
        }

        if (doc.has("trigger")) {
            parseTrigger(doc.getJSONObject("trigger"));
        }

        if (doc.has("forceLocation")) {
            form.setForceLocation(doc.getBoolean("forceLocation"));
        }

        if (doc.has("followActions")) {
            parseFollowActions(doc.getJSONArray("followActions"));
        }

    }

    /**
     * {
     *     from: 1,
     *     to: 2,
     *     expression: 'age>18'
     * }
     * @param transition
     */
    public void parseTransition(JSONObject transition) throws JSONException {
        int from = transition.getInt("from");
        int to = transition.getInt("to");
        String expression = transition.getString("expression");

        form.addTransition(new Transition(from, to, expression));
    }

    /**
     * {
     *     id: 1,
     *     questions: [1, 2, 3],
     * }
     * @param page
     */
    public void parsePage(JSONObject page) throws JSONException {
        Page p = new Page(page.getInt("id"));
        JSONArray questions = page.getJSONArray("questions");
        for (int i = 0; i < questions.length(); i++) {
            int id = questions.getInt(i);
            p.addQuestion(form.getQuestion(id));
        }
        form.addPage(p);
    }

    /**
     * {
     *     id: 1,
     *     title: "how old are you?",
     *     name: "age",
     *     type: "integer" // integer, text, double
     *     validations: [],     *
     *
     * }
     * @param q
     * @throws JSONException
     */
    public void parseQuestion(JSONObject q) throws JSONException {
        Question question = null;
        String type = q.getString("type");
        if (type.equals("integer")) {
            question = new Question<Integer>();
            question.setDataType(DataType.INTEGER);
        } else if (type.equals("double")) {
            question = new Question<Double>();
            question.setDataType(DataType.DOUBLE);
        } else if (type.equals("date")) {
            question = new Question<Date>();
            question.setDataType(DataType.DATE);
        } else {
            question = new Question<String>();
            question.setDataType(DataType.STRING);
        }

        question.setId(q.getInt("id"));
        question.setTitle(q.getString("title"));
        question.setName(q.getString("name"));

        if (!q.isNull("validations")) {
            JSONArray validations = q.getJSONArray("validations");
            for (int i = 0; i < validations.length(); i++) {
                JSONObject validation = validations.getJSONObject(i);
                parseValidation(validation, question);
            }
        }


        form.addQuestion(question);
    }

    /**
     * {
     *     type: "min",
     *     value: 10,
     *     message: "age must greater than 10"
     * }
     * @param v
     * @throws JSONException
     */
    public void parseValidation(JSONObject v, Question question) throws JSONException {
        String type = v.getString("type");
        IValidation iv = null;
        if (type.equals("min")) {
            iv = MinValidation.newInstance(question.getDataType(),
                    v.getString("value"),
                    v.getString("message"));
        } else if (type.equals("max")) {
            iv = MaxValidation.newInstance(question.getDataType(),
                    v.getString("value"),
                    v.getString("message"));
        } else if (type.equals("require")) {
            iv = RequireValidation.newInstance(question.getDataType(),
                    v.getString("message"));
        }
        if (iv != null) {
            question.addValidation(iv);
        }
    }

    /**
     * {
     *     id: 2,
     *     title: "What kind of animal?",
     *     name: "animal",
     *     type: "single", // single, multiple
     *     items: [
     *          {
     *              id: 'ox',
     *              text: 'ox
     *          },
     *          {
     *              id: 'cat',
     *              text: 'cat'
     *          }
     *     ],
     *     validations: [],
     * }
     * @param q
     * @throws JSONException
     */
    public void parseMultipleChoiceQuestion(JSONObject q) throws JSONException {
        MultipleChoiceQuestion question = null;
        String type = q.getString("type");
        if (type.equals("single")) {
            question = new MultipleChoiceQuestion(MultipleChoiceSelection.SINGLE);
            question.setDataType(DataType.STRING);
        } else if (type.equals("multiple")) {
            question = new MultipleChoiceQuestion(MultipleChoiceSelection.MULTIPLE);
            question.setDataType(DataType.STRING);
        }

        question.setId(q.getInt("id"));
        question.setTitle(q.getString("title"));
        question.setName(q.getString("name"));
        if (!q.isNull("freeTextText")) {
            question.setFreeTextText(q.getString("freeTextText"));
        }
        if (!q.isNull("freeTextName")) {
            question.setFreeTextName(q.getString("freeTextName"));
        }
        if (!q.isNull("freeTextId")) {
            question.setFreeTextId(q.getString("freeTextId"));
        }
        if (!q.isNull("freeTextChoiceEnable")) {
            question.setFreeTextChoiceEnable(q.getBoolean("freeTextChoiceEnable"));
        }

        if (!q.isNull("hiddenName")) {
            question.setHiddenName(q.getString("hiddenName"));
        }

        if (!q.isNull("validations")) {
            JSONArray validations = q.getJSONArray("validations");
            for (int i = 0; i < validations.length(); i++) {
                JSONObject validation = validations.getJSONObject(i);
                parseValidation(validation, question);
            }
        }

        JSONArray items = q.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            MultipleChoiceItem choiceItem = question.addItem(
                    item.getString("id"),
                    item.getString("text"));
            if (!item.isNull("hiddenValue")) {
                choiceItem.setHiddenValue(item.getString("hiddenValue"));
            }
        }

        if (question.isFreeTextChoiceEnable()) {
            question.addItem(question.getFreeTextId(), question.getFreeTextText());
        }

        form.addQuestion(question);
    }

    public void parseTrigger(JSONObject item) throws JSONException {
        if (item != null) {
            boolean merge = item.optBoolean("merge", false);
            Trigger trigger = new Trigger(item.getString("pattern"), item.getInt("pageId"), item.getString("notificationText"), merge);
            form.setTrigger(trigger);
        }
    }

    /**
     * "followActions": [
     *   {
     *     "name": "Lab Test",
     *     "startPageId": 404
     *   },
     *   {
     *     "name": "Case Mangment",
     *     "startPageId": 362
     *   }
     * ],
     * @param followActions
     * @throws JSONException
     */
    public void parseFollowActions(JSONArray followActions) throws JSONException {
        if (followActions !=  null) {
            for (int i = 0; i < followActions.length(); i++) {
                JSONObject item = followActions.getJSONObject(i);
                String name = item.getString("name");
                int startPageId = item.getInt("startPageId");

                form.addFollowActions(new FollowAction(name, startPageId));
            }
        }
    }

    public Form getForm() {
        return form;
    }
}
