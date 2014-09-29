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

import org.cm.podd.report.model.validation.MultipleChoiceValidation;
import org.cm.podd.report.model.validation.SingleChoiceValidation;
import org.cm.podd.report.model.validation.ValidationResult;

import java.security.cert.CertPathValidatorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class MultipleChoiceQuestion<T extends String> extends Question<T> {

    private List<MultipleChoiceItem> items = new ArrayList<MultipleChoiceItem>();
    private HashMap<T, MultipleChoiceItem> itemMap = new HashMap<T, MultipleChoiceItem>();
    private MultipleChoiceSelection selectionType;

    public MultipleChoiceQuestion(MultipleChoiceSelection selectionType) {
        this.selectionType = selectionType;
        this.setDataType(DataType.STRING);
        switch(selectionType) {
            case SINGLE:
                addValidation(new SingleChoiceValidation<T>("Value must be present in the giving list."));
                break;
            case MULTIPLE:
                addValidation(new MultipleChoiceValidation<T>("Value must be present in the giving list."));
                break;
        }
    }

    public void addItems(T id, String text) {
        MultipleChoiceItem item = new MultipleChoiceItem(id, text);
        items.add(item);
        itemMap.put(id, item);
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

    public boolean containKey(T value) {
        return itemMap.containsKey(value);
    }
}
