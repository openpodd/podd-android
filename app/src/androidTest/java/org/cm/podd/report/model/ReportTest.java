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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by pphetra on 11/10/14 AD.
 */
public class ReportTest extends TestCase {

    private String DATA = "{\n" +
            "  \"2@@@animal_type\": \"ไก่เนื้อ\",\n" +
            "  \"15@@@symptom\": \"หลังโก่ง\",\n" +
            "  \"6@@@die_num\": 0,\n" +
            "  \"11@@@symptom\": \"\",\n" +
            "  \"5@@@sick_num\": 2,\n" +
            "  \"10@@@symptom\": \"\",\n" +
            "  \"7@@@total_num\": 6,\n" +
            "  \"8@@@age\": \"ระยะรุ่น\",\n" +
            "  \"1@@@animal_group\": \"สัตว์ปีก\",\n" +
            "  \"12@@@symptom\": \"ไข่รูปร่างผิดปกติ\",\n" +
            "  \"9@@@symptom\": \"ขี้ขาว\",\n" +
            "  \"14@@@symptom\": \"\",\n" +
            "  \"13@@@symptom\": \"\"\n" +
            "  }";

    public void testGetJSONFormData() {
        Report report = new Report(1, 1, new Date(), 0, 0, 0);
        report.setFormData(DATA);

        try {
            JSONObject result = report.getSubmitJSONFormData();

            String[] names = new String[] {
                    "animal_type",
                    "symptom",
                    "sick_num",
                    "die_num",
                    "animal_group",
                    "total_num"
            };

            for (String name: names) {
                assertTrue(result.has(name));
            }

            // merge case
            String mergeValue = result.getString("symptom");
            assertTrue(mergeValue.indexOf("หลังโก่ง") >= 0);
            assertTrue(mergeValue.indexOf("ไข่รูปร่างผิดปกติ") >= 0);
            assertTrue(mergeValue.indexOf("ขี้ขาว") >= 0);

            String[] strings = mergeValue.split(",");
            assertEquals(3, strings.length);


        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

}
