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

import junit.framework.TestCase;

import org.cm.podd.report.model.validation.MaxValidation;
import org.cm.podd.report.model.validation.MinValidation;
import org.cm.podd.report.model.validation.RequireValidation;
import org.cm.podd.report.model.validation.ValidationResult;

import java.util.List;

/**
 * Created by pphetra on 9/25/14 AD.
 */
public class QuestionTest extends TestCase {

    Question<Integer> question1;
    Question<Integer> question2;

    private String minValidationMessage;
    private String maxValidationMessage;
    private String ageIsRequiredMessage;

    private ScriptEngineInterface engineInterface;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        question1 = new Question<Integer>();
        question1.setId(1);
        question1.setName("age");
        question1.setTitle("How old are you");

        minValidationMessage = "Age must be greater than one year old";
        question1.addValidation(new MinValidation<Integer>(1, minValidationMessage));
        maxValidationMessage = "Age must be lesser than 130 years old";
        question1.addValidation(new MaxValidation<Integer>(130, maxValidationMessage));
        ageIsRequiredMessage = "Age is Required";
        question1.addValidation(new RequireValidation<Integer>(ageIsRequiredMessage));

        question2 = new Question<Integer>();
        question2.setId(2);
        question2.setName("age");
        question2.setTitle("How old are you");
        ageIsRequiredMessage = "Age is Required";
        question2.addValidation(new RequireValidation<Integer>(ageIsRequiredMessage));

        engineInterface = new ScriptEngineInterface() {
            @Override
            public boolean evaluateExpression(String expression) {
                return true;
            }
        };
    }

    public void testValidAge() {
        question1.setData(20);
        List<ValidationResult> results = question1.validate(engineInterface);
        assertEquals(0, results.size());
    }

    public void testAgeIsNotSet() {
        List<ValidationResult> results = question2.validate(engineInterface);
        assertEquals(1, results.size());

        ValidationResult result = results.get(0);
        assertEquals(ageIsRequiredMessage, result.getMessage());
    }

    public void testAgeIsNotLesserThan1() {
        question1.setData(0);
        List<ValidationResult> results = question1.validate(engineInterface);
        assertEquals(1, results.size());

        ValidationResult result = results.get(0);
        assertEquals(minValidationMessage, result.getMessage());
    }

    public void testAgeIsGreaterThan130() {
        question1.setData(131);
        List<ValidationResult> results = question1.validate(engineInterface);
        assertEquals(1, results.size());

        ValidationResult result = results.get(0);
        assertEquals(maxValidationMessage, result.getMessage());
    }

    public void testDirty() {
        assertFalse(question1.isDirty());

        question1.setData(30);
        assertTrue(question1.isDirty());

        question1.setData(17);
        assertTrue(question1.isDirty());

        question1.setData(17);
        assertFalse(question1.isDirty());

        question1.setData(null);
        assertTrue(question1.isDirty());

        question1.setData(null);
        assertFalse(question1.isDirty());
    }

    public void testInitIntegerQuestionWithZeroValue() {
        Question<Integer> q1 = new Question<Integer>();
        q1.setDataType(DataType.INTEGER);
        assertNull(q1.getValue());
        q1.initWithDefaultValue();
        assertNotNull(q1.getValue());
        assertTrue(q1.getValue().intValue() == 0);
    }
}
