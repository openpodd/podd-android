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

package org.cm.podd.report.model;

import android.util.Log;

import org.cm.podd.report.model.validation.ValidationResult;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class FormIterator implements Serializable, ScriptEngineInterface {

    private static final String TAG = "FormIterator";
    private final Form form;
    private Page currentPage;
    private transient ExpressionEngine expressionEngine;

    private Stack<Page> visited = new Stack<Page>();

    public FormIterator(Form form) {
        this.form = form;
        this.currentPage = form.getPage(form.getStartPageId());
        expressionEngine = new ExpressionEngine();
    }

    /**
     *
     * @return
     */
    public boolean isAtFirstPage() {
        return currentPage.getId() == form.getStartPageId();
    }

    /**
     *
     * @return
     */
    public boolean isAtLastPage() {
        return form.getTransitionsForPage(currentPage.getId()).size() == 0;
    }

    public boolean validatePage() {
        List<ValidationResult> results = this.currentPage.validate(this);
        if (results.size() > 0) {
            Log.d("FormIterator", "can't go to next coz of validation fail.");
            for (ValidationResult vr: results) {
                Log.d("FormIterator", vr.getMessage());
            }
            return false;
        }
        return true;
    }

    public List<ValidationResult> validatePageAndGetResult() {
        List<ValidationResult> results = this.currentPage.validate(this);
        if (results.size() > 0) {
            Log.d("FormIterator", "can't go to next coz of validation fail.");
            for (ValidationResult vr: results) {
                Log.d("FormIterator", vr.getMessage());
            }
        }
        return results;
    }

    public void flush() {
        setPageDataToExpressionEngine(currentPage);
    }

    /**
     *
     * @return true if operation can be done.
     */
    public boolean nextPage() {

        setPageDataToExpressionEngine(currentPage);

        boolean validatePass = validatePage();
        if (!validatePass) {
            return false;
        }


        int currentPageId = currentPage.getId();
        List<Transition> transitionList = form.getTransitionsForPage(currentPageId);
        int nextPageId = -99;
        for (Transition t: transitionList) {
            if (getExpressionEngine().evaluateBooleanExpression(t.getExpression())) {
                nextPageId = t.getToPage();
                break;
            }
        }

        if (nextPageId == -99) {
            Log.d("FormIterator", "can't find nextPageId");
            return false;
        } else {
            Log.d("FormIterator", "nextPageId =" + nextPageId);
            visited.add(currentPage);
            currentPage = form.getPage(nextPageId);
            return true;
        }
    }

    /**
     *
     * @return true if operation can be done.
     */
    public boolean previousPage() {
        if (visited.size() == 0) {
            return false;
        }

        currentPage = visited.pop();

        recreateExpressionEngine();

        return true;
    }

    private void recreateExpressionEngine() {
        Log.d(TAG, "recreateExpressionEngine");
        expressionEngine = new ExpressionEngine();
        assignDataToExpressionEngine(getData(true).entrySet());
    }

    private void assignDataToExpressionEngine(Set<Map.Entry<String, Object>> entries) {
        for (Map.Entry<String, Object> ks: entries) {
            expressionEngine.setObject(ks.getKey(), ks.getValue());
        }
    }

    /**
     *
     * @return current Page.
     */
    public Page getCurrentPage() {
        return currentPage;
    }

    private void setPageDataToExpressionEngine(Page page) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        page.getData(data, true);
        assignDataToExpressionEngine(data.entrySet());
    }

    public Map<String, Object> getData(boolean keyAsName) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        for (Page p : visited) {
            p.getData(data, keyAsName);
        }

        currentPage.getData(data, keyAsName);
        return data;
    }

    public Form getForm() {
        return form;
    }

    private ExpressionEngine getExpressionEngine() {
        if (expressionEngine == null) {
            recreateExpressionEngine();
        }
        return expressionEngine;
    }

    @Override
    public boolean evaluateExpression(String expression) {
        return getExpressionEngine().evaluateBooleanExpression(expression);
    }
}
