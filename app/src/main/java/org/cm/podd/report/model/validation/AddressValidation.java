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

package org.cm.podd.report.model.validation;

import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.ScriptEngineInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AddressValidation<T> implements IValidation<T> {

    private T minValue;
    private String message;

    public AddressValidation(T minValue) {
        this.message = "'Address' is not correct";
    }

    public AddressValidation(T minValue, String message) {
        this.minValue = minValue;
        this.message = message;

    }


    @Override
    public ValidationResult validate(T value, Question<T> question, ScriptEngineInterface engineInterface) {
        if (value instanceof Comparable) {
            boolean checked = true;

            Object text = value;
            String[] fields = question.getFilterFields().split(",");

            for (int idx = 0; idx < fields.length; idx++) {
                String[] values = fields[idx].split("\\|");
                String header = fields[idx].replaceAll(" ", "");

                if (values.length > 1) {
                    header = values[1];
                }

                if (text != null) {
                    Pattern specificPattern = Pattern.compile("\\[" + header + ":(.*?)\\]");
                    Matcher match = specificPattern.matcher(text.toString());
                    if (!match.find()) {
                        checked = false;
                        break;
                    }

                }
            }

            if (checked) {
                return SUCCESS;
            }
        }

        return new ValidationResult(false, message);
    }

    public static AddressValidation newInstance(DataType dataType, String message) {
        return new AddressValidation<String>(null, message);
    }
}
