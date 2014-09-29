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

import org.cm.podd.report.model.validation.IValidation;
import org.cm.podd.report.model.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pphetra on 9/25/14 AD.
 */
public class Question<T> {

    private int id;
    private String title;
    private String name;
    private DataType dataType;
    private List<IValidation<T>> validations;

    private T data;
    private T oldData;

    public Question() {
        validations = new ArrayList<IValidation<T>>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addValidation(IValidation<T> validation) {
        validations.add(validation);
    }

    public void setData(T value) {
        oldData = data;
        data = value;
    }

    public List<ValidationResult> validate() {
        ArrayList<ValidationResult> results = new ArrayList<ValidationResult>();

        try {
            for (IValidation<T> v : validations) {
                ValidationResult result = v.validate(data, this);
                if (! result.isSuccess()) {
                    results.add(result);
                }
            }
        } catch (ClassCastException cce) {
            results.add(new ValidationResult(false, "Internal Error: Data type mismatch"));
        }

        return results;
    }

    public T getValue() {
        return data;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public boolean isDirty() {
        Log.d("Question", String.format("old:%s, new:%s", oldData, data));
        if (data == null) {
            if (oldData == null) {
                return false;
            } else {
                return true;
            }
        } else {
            if (oldData == null) {
                return true;
            } else {
                return ! data.equals(oldData);
            }
        }
    }

}