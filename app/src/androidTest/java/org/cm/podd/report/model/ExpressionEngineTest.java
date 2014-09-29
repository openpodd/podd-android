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

import android.util.Log;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pphetra on 9/26/14 AD.
 */
public class ExpressionEngineTest extends TestCase {

    ExpressionEngine engine;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        engine = new ExpressionEngine();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        engine.destroy();
    }

    public void testDirect() {
        assertTrue(engine.evaluateBooleanExpression("true"));
        assertFalse(engine.evaluateBooleanExpression("false"));
    }

    public void testIntegerVariable() {
        engine.setInteger("age", 10);
        assertTrue(engine.evaluateBooleanExpression("age === 10"));
        assertTrue(engine.evaluateBooleanExpression("age > 9"));
        assertTrue(engine.evaluateBooleanExpression("age < 11"));
    }

    public void testBooleanVariable() {
        engine.setBoolean("single", true);
        engine.setBoolean("marriage", false);
        assertTrue(engine.evaluateBooleanExpression("single"));
        assertFalse(engine.evaluateBooleanExpression("marriage"));
    }

    public void testStringVariable() {
        engine.setString("like", "food");
        assertTrue(engine.evaluateBooleanExpression("like==='food'"));
    }

    public void testSetArray() {
        List<String> values = new ArrayList<String>();
        values.add("one");
        values.add("two");
        values.add("three");
        engine.setStringArray("numbers", values);
        assertTrue(engine.evaluateBooleanExpression("numbers != undefined"));
        assertTrue(engine.evaluateBooleanExpression("numbers.size() === 3"));
    }

    public void testArrayIndexOf() {
        List<String> values = new ArrayList<String>();
        values.add("one");
        values.add("two");
        values.add("three");
        engine.setStringArray("numbers", values);

        assertTrue(engine.evaluateBooleanExpression("numbers != undefined"));
        assertTrue(engine.evaluateBooleanExpression("numbers.contains('two')"));
        assertFalse(engine.evaluateBooleanExpression("numbers.contains('Four')"));
    }
}
