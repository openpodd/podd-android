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

/**
 * Created by pphetra on 9/24/15 AD.
 */
public class ScriptValidation<T> implements IValidation<T> {


    private final String expression;
    private final String message;

    public ScriptValidation(String expression, String message) {
        this.expression = expression;
        this.message = message;
    }

    @Override
    public ValidationResult validate(T value, Question<T> question, ScriptEngineInterface engineInterface) {
        if (engineInterface.evaluateExpression(expression)) {
            return new ValidationResult(false, message);
        }
        return SUCCESS;
    }


    public static ScriptValidation newInstance(DataType dataType, String expression, String message) {
        switch (dataType) {
            case STRING:
                return new ScriptValidation<String>(expression, message);
            case INTEGER:
                return new ScriptValidation<Integer>(expression, message);
            case DOUBLE:
                return new ScriptValidation<Double>(expression, message);
        }
        return null;
    }
}
