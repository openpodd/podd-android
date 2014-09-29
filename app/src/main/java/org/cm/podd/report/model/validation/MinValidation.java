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

import java.util.Map;

/**
 * Created by pphetra on 9/25/14 AD.
 */
public class MinValidation<T> implements IValidation<T> {

    private T minValue;
    private String message;

    public MinValidation(T minValue, String message) {
        this.minValue = minValue;
        this.message = message;
    }

    @Override
    public ValidationResult validate(T value) {
        if (value instanceof Comparable) {
            if (((Comparable) value).compareTo(minValue) >= 0) {
                return SUCCESS;
            }
        }

        return new ValidationResult(false, message);
    }
}
