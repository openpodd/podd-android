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

import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.ScriptEngineInterface;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class MultipleChoiceValidation<T extends String> implements IValidation<T> {

    private String message;

    public MultipleChoiceValidation(String message) {
        this.message = message;
    }

    @Override
    public ValidationResult validate(T value, Question<T> question, ScriptEngineInterface engineInterface) {
        MultipleChoiceQuestion mQuestion = (MultipleChoiceQuestion) question;
        if (value != null && ! value.equals("")) {
            for (String v : value.split(",")) {
                if (! mQuestion.containKey(v)) {
                    return new ValidationResult(false, message);
                }
            }
        }
        return SUCCESS;
    }

}