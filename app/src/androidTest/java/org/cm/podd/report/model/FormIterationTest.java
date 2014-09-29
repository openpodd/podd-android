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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class FormIterationTest extends TestCase {

    Form f;
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        f = new Form();

        Question<Integer> q1 = new Question<Integer>();
        q1.setId(1);
        q1.setDataType(DataType.INTEGER);
        q1.setName("age");
        q1.setTitle("How old are you?");
        q1.addValidation(new RequireValidation<Integer>());
        f.addQuestion(q1);

        Question<String> q2 = new Question<String>();
        q2.setId(2);
        q2.setDataType(DataType.STRING);
        q2.setName("name");
        q2.setTitle("What is your name?");
        f.addQuestion(q2);

        Question<String> q3 = new Question<String>();
        q3.setId(3);
        q3.setDataType(DataType.STRING);;
        q3.setName("spouse_name");
        q3.setTitle("Spouse name?");
        f.addQuestion(q3);

        Question<String> q4 = new Question<String>();
        q4.setId(4);
        q4.setDataType(DataType.STRING);
        q4.setName("parent_name");
        q4.setTitle("Parent name");
        f.addQuestion(q4);

        Page p1 = new Page(1);
        p1.addQuestion(q1);
        f.addPage(p1);

        Page p2 = new Page(2);
        p2.addQuestion(q2);
        f.addPage(p2);

        Page p3 = new Page(3);
        p3.addQuestion(q3);
        f.addPage(p3);

        Page p4 = new Page(4);
        p4.addQuestion(q4);
        f.addPage(p4);

        Transition t1 = new Transition(1, 2, "true");
        Transition t2 = new Transition(2, 3, "age > 20");
        Transition t3 = new Transition(2, 4, "age <= 20");
        f.addTransition(t1);
        f.addTransition(t2);
        f.addTransition(t3);

        f.setStartPageId(1);

    }

    public void testCurrentPage() {
        FormIterator fi = new FormIterator(f);
        assertNotNull(fi.getCurrentPage());
        assertEquals(1, fi.getCurrentPage().getId());

        assertTrue(fi.isAtFirstPage());
        assertFalse(fi.isAtLastPage());
    }

    public void testNextPageIfValidate() {
        FormIterator fi = new FormIterator(f);

        assertFalse(fi.nextPage());

        HashMap<String, String> d1 = new HashMap<String, String>();
        d1.put("age", "30");
        fi.getCurrentPage().setRawData(d1);

        assertTrue(fi.nextPage());

        assertEquals(2, fi.getCurrentPage().getId());
    }

    public void testPreviousPageAtStart() {
        FormIterator fi = new FormIterator(f);
        assertFalse(fi.previousPage());
    }

    public void testCanGoNextIfNoValidation() {
        FormIterator fi = new FormIterator(f);

        HashMap<String, String> d1 = new HashMap<String, String>();
        d1.put("age", "30");
        fi.getCurrentPage().setRawData(d1);

        fi.nextPage();

        assertTrue(fi.nextPage());
    }

    public void testAgeGT20Branch() {
        FormIterator fi = new FormIterator(f);

        HashMap<String, String> d1 = new HashMap<String, String>();
        d1.put("age", "30");
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();

        d1 = new HashMap<String, String>();
        d1.put("name", "pphetra");
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();

        assertEquals(3, fi.getCurrentPage().getId());
        assertFalse(fi.nextPage()); // no more page
    }

    public void testAgeLT20Branch() {
        FormIterator fi = new FormIterator(f);

        HashMap<String, String> d1 = new HashMap<String, String>();
        d1.put("age", "19");
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();

        d1 = new HashMap<String, String>();
        d1.put("name", "pphetra");
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();

        assertEquals(4, fi.getCurrentPage().getId());
        assertFalse(fi.nextPage()); // no more page
    }

    public void testNextAndBack() {
        FormIterator fi = new FormIterator(f);

        HashMap<String, String> d1 = new HashMap<String, String>();
        d1.put("age", "19");
        fi.getCurrentPage().setRawData(d1);
        assertTrue(fi.nextPage());

        assertTrue(fi.previousPage());
        assertEquals(1, fi.getCurrentPage().getId());
    }

    public void testGoToLastAndThenBackToFirst() {
        FormIterator fi = new FormIterator(f);

        HashMap<String, String> d1 = new HashMap<String, String>();
        d1.put("age", "30");
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();
        assertEquals(2, fi.getCurrentPage().getId());

        d1 = new HashMap<String, String>();
        d1.put("name", "pphetra");
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();
        assertEquals(3, fi.getCurrentPage().getId());

        d1 = new HashMap<String, String>();
        d1.put("spouse_name", "bunny");
        fi.getCurrentPage().setRawData(d1);

        Map<String, Object> data = fi.getData();
        assertEquals(30, data.get("age"));
        assertEquals("pphetra", data.get("name"));
        assertEquals("bunny", data.get("spouse_name"));
        assertNull(data.get("parent_name"));

        assertTrue(fi.isAtLastPage());

        fi.previousPage();
        fi.previousPage();

        d1 = new HashMap<String, String>();
        d1.put("age", "12");
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();
        assertEquals(2, fi.getCurrentPage().getId());

        d1 = new HashMap<String, String>();
        d1.put("name", null);
        fi.getCurrentPage().setRawData(d1);
        fi.nextPage();
        assertEquals(4, fi.getCurrentPage().getId());


        d1 = new HashMap<String, String>();
        d1.put("parent_name", "John");
        fi.getCurrentPage().setRawData(d1);

        data = fi.getData();
        assertEquals(12, data.get("age"));
        assertNull(data.get("name"));
        assertEquals("John", data.get("parent_name"));
        assertNull(data.get("spouse_name"));

        assertTrue(fi.isAtLastPage());
    }



}
