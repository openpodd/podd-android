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

import org.cm.podd.report.model.validation.RequireValidation;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class MultipleChoiceQuestionTest extends TestCase {

    public void testSingleSelection() {
        MultipleChoiceQuestion<String> q1 = new MultipleChoiceQuestion<String>(MultipleChoiceSelection.SINGLE);
        q1.addItem("1", "item 1");
        q1.addItem("2", "item 2");
        q1.addItem("3", "item 3");

        q1.setData("4");
        assertEquals(1, q1.validate().size());

        q1.setData("2");

        assertEquals(0, q1.validate().size());
    }

    public void testMultipleSelection() {
        MultipleChoiceQuestion<String> q1 = new MultipleChoiceQuestion<String>(MultipleChoiceSelection.MULTIPLE);
        q1.addItem("1", "item 1");
        q1.addItem("2", "item 2");
        q1.addItem("3", "item 3");

        q1.setData("4");
        assertEquals(1, q1.validate().size());

        q1.setData("2");
        assertEquals(0, q1.validate().size());

        q1.setData("1,2");
        assertEquals(0, q1.validate().size());

        q1.setData("2,1,3");
        assertEquals(0, q1.validate().size());
    }

    public void testCombineWithRequireValidation() {
        MultipleChoiceQuestion<String> q1 = new MultipleChoiceQuestion<String>(MultipleChoiceSelection.SINGLE);
        q1.addItem("1", "item 1");
        q1.addItem("2", "item 2");
        q1.addItem("3", "item 3");

        assertEquals(0, q1.validate().size());
        q1.addValidation(new RequireValidation<String>());
        assertEquals(1, q1.validate().size());


        q1.setData("2");
        assertEquals(0, q1.validate().size());
    }
}
