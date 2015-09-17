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
import org.cm.podd.report.model.validation.MultipleChoiceValidation;
import org.cm.podd.report.model.validation.SingleChoiceValidation;
import org.cm.podd.report.model.validation.ValidationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class MultipleChoiceQuestion extends Question<String> {

    private static final String TAG = "MultipleChoiceQuestion";
    private List<MultipleChoiceItem> items = new ArrayList<MultipleChoiceItem>();
    private HashMap<String, MultipleChoiceItem> itemMap = new HashMap<String, MultipleChoiceItem>();
    private MultipleChoiceSelection selectionType;

    private String hiddenName;

    private boolean freeTextChoiceEnable = false;
    private String freeTextText;
    private String freeTextName;
    private String freeTextId;
    private String freeTextValue;

    public MultipleChoiceQuestion(MultipleChoiceSelection selectionType) {
        this.selectionType = selectionType;
        this.setDataType(DataType.STRING);
    }

    public MultipleChoiceItem addItem(String id, String text) {
        return addItem(id, text, false);
    }

    public MultipleChoiceItem addItem(String id, String text, boolean flag) {
        MultipleChoiceItem item = new MultipleChoiceItem(id, text);
        item.setChecked(flag);
        items.add(item);
        itemMap.put(id, item);
        return item;
    }

    public List<MultipleChoiceItem> getItems() {
        return items;
    }

    public MultipleChoiceSelection getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(MultipleChoiceSelection selectionType) {
        this.selectionType = selectionType;
    }

    public boolean containKey(String value) {
        return itemMap.containsKey(value);
    }

    public void clearAllItemChecked() {
        for (MultipleChoiceItem item: items) {
            item.setChecked(false);
        }
    }

    public boolean isFreeTextChoiceEnable() {
        return freeTextChoiceEnable;
    }

    public void setFreeTextChoiceEnable(boolean freeTextChoiceEnable) {
        this.freeTextChoiceEnable = freeTextChoiceEnable;
    }

    @Override
    public String getValue() {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (MultipleChoiceItem item : items) {
            if (item.isChecked()) {
                if (!isFirst) {
                    builder.append(",");
                }
                builder.append(item.getId());
                isFirst = false;
            }
        }
        return builder.toString();
    }

    @Override
    public void setData(String value) {
        Log.d(TAG, "question id:" + getId() + ", value = " + value);
        super.setData(value);
        String[] strings = value.split(",");
        clearAllItemChecked();
        for (String s: strings) {
            for (MultipleChoiceItem item : items) {
                if (item.getId().equals(s)) {
                    Log.d(TAG, "check on item:" + item.getId());
                    item.setChecked(true);
                }
            }
        }
    }

    @Override
    public List<ValidationResult> validate() {
        ArrayList<ValidationResult> results = new ArrayList<ValidationResult>();

        for (IValidation v : getValidations()) {
            ValidationResult result = v.validate(getValue(), this);
            if (! result.isSuccess()) {
                results.add(result);
            }
        }

        return results;
    }


    public String getFreeTextText() {
        return freeTextText;
    }

    public void setFreeTextText(String freeTextText) {
        this.freeTextText = freeTextText;
    }

    public String getFreeTextName() {
        return freeTextName;
    }

    public void setFreeTextName(String freeTextName) {
        this.freeTextName = freeTextName;
    }

    public String getFreeTextId() {
        return freeTextId;
    }

    public void setFreeTextId(String freeTextId) {
        this.freeTextId = freeTextId;
    }

    public String getFreeTextValue() {
        return freeTextValue;
    }

    public void setFreeTextValue(String freeTextValue) {
        this.freeTextValue = freeTextValue;
    }

    @Override
    public void setData(String name, String value) {
        if (name.equals(getFreeTextName())) {
            setFreeTextValue(value);
        } else {
            super.setData(name, value);
        }
    }

    @Override
    public void getData(Map data, boolean keyAsName) {
        super.getData(data, keyAsName);

        if (isFreeTextChoiceEnable() && isFreeTextChoiceTick()) {
            String key = getKey(keyAsName, getFreeTextName());
            data.put(key, getFreeTextValue());
        }

        if (hiddenName != null) {
            String key = getKey(keyAsName, hiddenName);
            data.put(key, getHiddenValue());
        }
    }

    private String getKey(boolean keyAsName, String name) {
        String key = null;
        if (keyAsName) {
            key = name;
        } else {
            key = getId() + "@@@" + name;
        }
        return key;
    }

    private boolean isFreeTextChoiceTick() {
        for (MultipleChoiceItem item: items) {
            if (item.getId().equals(getFreeTextId()) && item.isChecked()) {
                return true;
            }
        }
        return false;
    }

    public String getHiddenName() {
        return hiddenName;
    }

    public void setHiddenName(String hiddenName) {
        this.hiddenName = hiddenName;
    }

    private String getHiddenValue() {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (MultipleChoiceItem item : items) {
            if (item.isChecked()) {
                if (!isFirst) {
                    builder.append(",");
                }
                builder.append(item.getHiddenValue());
                isFirst = false;
            }
        }
        return builder.toString();
    }
}
