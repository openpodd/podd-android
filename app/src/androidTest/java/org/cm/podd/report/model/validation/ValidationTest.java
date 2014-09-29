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

import junit.framework.TestCase;

/**
 * Created by pphetra on 9/25/14 AD.
 */
public class ValidationTest extends TestCase {

    public void testRequireValidation() {
        IValidation<String> v = new RequireValidation<String>();

        ValidationResult result = v.validate("any string");
        assertTrue(result.isSuccess());

        result = v.validate(null);
        assertFalse(result.isSuccess());
    }

    public void testIntegerMinValidation() {
        String message = "ค่าต้องมากกว่า 10";
        IValidation<Integer> v = new MinValidation<Integer>(10, message);

        ValidationResult result = v.validate(20);
        assertTrue(result.isSuccess());

        result = v.validate(10);
        assertTrue(result.isSuccess());
        assertNull(result.getMessage());

        result = v.validate(9);
        assertFalse(result.isSuccess());

        result = v.validate(-10);
        assertFalse(result.isSuccess());
        assertEquals(message, result.getMessage());
    }

    public void testIntegerMaxValidation() {
        String message = "ค่าต้องน้อยกว่า 120";
        IValidation<Integer> v = new MaxValidation<Integer>(120, message);

        ValidationResult result = v.validate(20);
        assertTrue(result.isSuccess());

        result = v.validate(119);
        assertTrue(result.isSuccess());
        assertNull(result.getMessage());

        result = v.validate(120);
        assertFalse(result.isSuccess());

        result = v.validate(121);
        assertFalse(result.isSuccess());
        assertEquals(message, result.getMessage());
    }
}
