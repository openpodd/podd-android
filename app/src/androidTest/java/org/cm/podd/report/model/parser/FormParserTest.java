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

package org.cm.podd.report.model.parser;

import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.test.InstrumentationTestCase;

import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.FormIterator;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.validation.RequireValidation;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class FormParserTest extends InstrumentationTestCase {
    public String questionJson = "{"
            + "     id: 1,"
            + "     title: \"how old are you\","
            + "     name: \"age\","
            + "     type: \"integer\","
            + "     validations: ["
            + "       { type: \"require\", message: \"Age is required\"},"
            + "       { type: \"min\", value: 10, message: \"Age must greater than 10\"}"
            + "     ]"
            + "}";

    public void testParseQuestion() {
        JSONObject obj = null;
        try {
            obj = new JSONObject(questionJson);
            FormParser formParser = new FormParser();
            formParser.parseQuestion(obj);

            assertNotNull(formParser.getForm().getQuestion(1));
            assertNull(formParser.getForm().getQuestion(2));

            Question q = formParser.getForm().getQuestion(1);
            assertEquals(1, q.getId());
            assertEquals("how old are you", q.getTitle());
            assertEquals("age", q.getName());
            assertEquals(DataType.INTEGER, q.getDataType());

            assertEquals(2, q.getValidations().size());
            assertTrue(q.getValidations().get(0) instanceof RequireValidation);

        } catch (JSONException e) {
            fail();
        }
    }

    public String pageJson = "{"
            + "    id: 3,"
            + "    questions: [1,2]"
            + "}";

    public void testParsePage() {
        try {

            FormParser formParser = new FormParser();
            Question<Integer> q1 = new Question<Integer>();
            q1.setId(1);
            q1.setName("age");
            q1.setTitle("how old are you");
            q1.setDataType(DataType.INTEGER);

            Question<String> q2 = new Question<String>();
            q1.setId(2);
            q1.setName("name");
            q1.setTitle("What is your name");
            q1.setDataType(DataType.STRING);

            formParser.getForm().addQuestion(q1);
            formParser.getForm().addQuestion(q2);

            JSONObject obj = new JSONObject(pageJson);
            formParser.parsePage(obj);

            assertNotNull(formParser.getForm().getPage(3));
            assertNull(formParser.getForm().getPage(1));

            Page p = formParser.getForm().getPage(3);
            assertEquals(2, p.getQuestions().size());

        } catch (JSONException e) {
            fail();
        }

    }

    public String transitionJson = "{"
            + " from: 1,"
            + " to: 2,"
            + " expression: \"age>18\""
            + "}";

    public void testParseTransition() {
        try {

            FormParser formParser = new FormParser();
            JSONObject jsonObject = new JSONObject(transitionJson);
            formParser.parseTransition(jsonObject);

            assertEquals(1, formParser.getForm().getTransitionsForPage(1).size());

        } catch (JSONException e) {
            fail();
        }
    }

    public String multipleQuestionJson = "{" +
        "id: 3," +
        "name: \"pet_type\"," +
        "title: \"Kind of pet\"," +
        "type: \"multiple\"," +
        "items: [ " +
        "{ id: \"dog\", text: \"dog\" }," +
        "{ id: \"cat\", text: \"cat\" }," +
        "{ id: \"bird\", text: \"bird\" }" +
        "]," +
        "freeTextChoiceEnable: true," +
        "freeTextText: \"Other\"," +
        "freeTextName: \"pet_type_other\"," +
        "freeTextId: \"other\"" +
        "}";

    public void testParseMultipleChoiceQuestion() {
        JSONObject obj = null;
        try {
            obj = new JSONObject(multipleQuestionJson);
            FormParser formParser = new FormParser();
            formParser.parseMultipleChoiceQuestion(obj);

            assertNotNull(formParser.getForm().getQuestion(3));

            MultipleChoiceQuestion q = (MultipleChoiceQuestion) formParser.getForm().getQuestion(3);
            assertEquals(3, q.getId());
            assertEquals("Kind of pet", q.getTitle());
            assertEquals("pet_type", q.getName());
            assertEquals(DataType.STRING, q.getDataType());

            assertEquals(4, q.getItems().size()); // 3 + 1 (from other choice

            assertTrue(q.isFreeTextChoiceEnable());
            assertEquals("Other", q.getFreeTextText());
            assertEquals("pet_type_other", q.getFreeTextName());
            assertEquals("other", q.getFreeTextId());

        } catch (JSONException e) {
            fail();
        }
    }

    public void testParseForm() throws IOException, PackageManager.NameNotFoundException {
        AssetManager assetManager = getInstrumentation().getContext().getAssets();
        InputStream stream = assetManager.open("form.json");
        assertNotNull(stream);

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        FormParser formParser = new FormParser();
        try {
            formParser.parse(new JSONObject(builder.toString()));
        } catch (JSONException e) {
            fail();
        }

    }

    public void testParseForm2() throws IOException, PackageManager.NameNotFoundException {
        AssetManager assetManager = getInstrumentation().getContext().getAssets();
        InputStream stream = assetManager.open("podd.json");
        assertNotNull(stream);

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        FormParser formParser = new FormParser();
        try {
            formParser.parse(new JSONObject(builder.toString()));
        } catch (JSONException e) {
            fail();
        }

        Form form = formParser.getForm();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(form);
        os.flush();
        bos.close();

        FormIterator iterator = new FormIterator(form);
        iterator.nextPage();

        bos = new ByteArrayOutputStream();
        os = new ObjectOutputStream(bos);
        os.writeObject(iterator);
        os.flush();
        bos.close();

    }
}
