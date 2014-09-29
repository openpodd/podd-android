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

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class FormTest extends TestCase {

    public void testQuestionPopulate() {
        Form f = new Form();
        Question<Integer> q1 = new Question<Integer>();
        q1.setId(1);
        q1.setName("age");

        Question<String> q2 = new Question<String>();
        q2.setId(2);
        q2.setName("name");

        f.addQuestion(q1);
        f.addQuestion(q2);

        assertEquals("age", f.getQuestion(1).getName());
        assertEquals("name", f.getQuestion(2).getName());
    }

    public void testPagePopulate() {
        Form f = new Form();
        Page p1 = new Page(17);

        Page p2 = new Page(20);

        f.addPage(p1);
        f.addPage(p2);

        assertEquals(p1.getId(), f.getPage(17).getId());
        assertEquals(p2.getId(), f.getPage(20).getId());
    }

    public void testTransitionPopulate() {
        Form f = new Form();
        Transition t1 = new Transition(1, 2, "true");
        Transition t2 = new Transition(2, 3, "true");
        Transition t3 = new Transition(3, 4, "true");
        Transition t4 = new Transition(3, 4, "1==1");


        f.addTransition(t1);
        f.addTransition(t2);
        f.addTransition(t3);
        f.addTransition(t4);

        assertEquals(1, f.getTransitionsForPage(2).size());
        assertEquals(2, f.getTransitionsForPage(3).size());

    }
}
