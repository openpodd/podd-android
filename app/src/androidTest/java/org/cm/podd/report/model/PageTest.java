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
 * Created by pphetra on 9/26/14 AD.
 */
public class PageTest extends TestCase {
    private Page page;
    private Question<Integer> question1;
    private Question<String> question2;

    private String minValidationMessage;
    private String maxValidationMessage;
    private String ageIsRequiredMessage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        page = new Page(1);

        question1 = new Question<Integer>();
        question1.setId(1);
        question1.setTitle("How old are you?");
        question1.setName("age");

        minValidationMessage = "Age must be greater than one year old";
        question1.addValidation(new MinValidation<Integer>(1, minValidationMessage));
        maxValidationMessage = "Age must be lesser than 130 years old";
        question1.addValidation(new MaxValidation<Integer>(130, maxValidationMessage));
        ageIsRequiredMessage = "Age is Required";
        question1.addValidation(new RequireValidation<Integer>(ageIsRequiredMessage));

        page.addQuestion(question1);


        question2 = new Question<String>();
        question2.setId(2);
        question2.setTitle("What is your name?");
        question2.setName("name");
        page.addQuestion(question2);
    }

    public void testValidateNoSetAtAll() {
        List<ValidationResult> validates = page.validate();
        assertTrue(validates.size() > 0);
        assertEquals(3, validates.size());
    }

    public void testValidateSetOnly1() {
        question1.setData(20);
        List<ValidationResult> validates = page.validate();
        assertEquals(0, validates.size());
    }

    public void testValidateSetBoth() {
        question1.setData(20);
        question2.setData("pphetra");
        List<ValidationResult> validates = page.validate();
        assertEquals(0, validates.size());
    }

    public void testValidateSetOnly1And2IsRequired() {
        question1.setData(20);
        question2.addValidation(new RequireValidation<String>("name is required"));

        List<ValidationResult> validates = page.validate();
        assertEquals(1, validates.size());
    }
}
