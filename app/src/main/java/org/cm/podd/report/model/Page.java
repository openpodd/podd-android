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

import org.cm.podd.report.model.validation.ValidationResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pphetra on 9/26/14 AD.
 */
public class Page implements Serializable {

    private int id;
    private List<Question> questions = new ArrayList<Question>();

    public Page(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public List<ValidationResult> validate() {
        List<ValidationResult> results = new ArrayList<ValidationResult>();
        for (Question q : questions) {
            results.addAll(q.validate());
        }
        return results;
    }

    public void setData(Map<String, Object> values) {
        for (Question q : questions) {
            q.setData(values.get(q.getName()));
        }
    }

    public void setRawData(Map<String, String> values) {
        for (Question q : questions) {
            String key = q.getName();
            String value = values.get(key);
            if (value != null) {
                q.setData(q.getDataType().parseFromString(value));
            } else {
                q.setData(null);
            }
        }
    }

    public Question getQuestion(int questionId) {
        for (Question q : questions) {
            if (q.getId() == questionId) {
                return q;
            }
        }
        return null;
    }

    public boolean isDirty() {
        boolean dirty = false;
        for (Question q : questions) {
            dirty = dirty || q.isDirty();
        }

        return dirty;
    }

    public void getData(HashMap<String, Object> data) {
        for (Question q : questions) {
            String key = q.getName();
            Object value = q.getValue();
            data.put(key, value);
        }
    }
}
