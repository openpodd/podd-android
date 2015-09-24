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

package org.cm.podd.report.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.cm.podd.report.R;
import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.FormIterator;
import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.MultipleChoiceSelection;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.Transition;
import org.cm.podd.report.model.validation.RequireValidation;
import org.cm.podd.report.model.validation.ValidationResult;
import org.cm.podd.report.model.view.PageView;

import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class TestFormActivity extends ActionBarActivity {

    private Form form;
    private FormIterator formIterator;

    int mBackStackSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        form = createSampleForm1();
        formIterator = new FormIterator(form);

        setContentView(R.layout.activity_test_form);

        Button prevBtn = (Button) findViewById(R.id.prevBtn);
        Button nextBtn = (Button) findViewById(R.id.nextBtn);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Page currentPage = formIterator.getCurrentPage();
                if (formIterator.nextPage()) {
                    setPage(currentPage, true);
                } else {
                    if (! formIterator.isAtLastPage()) {
                        // validation case
                        List<ValidationResult> validateResults = currentPage.validate(formIterator);
                        StringBuffer buff = new StringBuffer();
                        for (ValidationResult vr : validateResults) {
                            buff.append(vr.getMessage()).append("\n");
                        }
                        final Crouton crouton = Crouton.makeText(TestFormActivity.this, buff.toString(), Style.ALERT);
                        crouton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Crouton.hide(crouton);
                            }
                        });
                        crouton.show();
                    }
                }
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        setPage(formIterator.getCurrentPage(), false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("---", String.format("back stack count = %d", getSupportFragmentManager().getBackStackEntryCount()));
        if (getSupportFragmentManager().getBackStackEntryCount() >= 0) {
            formIterator.previousPage();
            Log.d("----", String.format("back to page %d", formIterator.getCurrentPage().getId()));
        }
    }

    private void setPage(Page page, boolean replaceFlag) {
        Fragment f = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("page", page);
        f.setArguments(bundle);

        mBackStackSize++;

        if (replaceFlag) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, f)
                    .addToBackStack(Integer.toString(page.getId()))
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, f)
                    .commit();
        }

    }

    private Form createSampleForm1() {
        Form form = new Form();
        form.setStartPageId(1);

        Question<Integer> q1 = new Question<Integer>();
        q1.setTitle("how old are your?");
        q1.setName("age");
        q1.setId(1);
        q1.addValidation(new RequireValidation<Integer>("Age is required"));

        q1.setDataType(DataType.INTEGER);
        form.addQuestion(q1);

        Question<String> q2 = new Question<String>();
        q2.setTitle("What is your name");
        q2.setName("name");
        q2.setId(2);
        q2.setDataType(DataType.STRING);
        form.addQuestion(q2);

        MultipleChoiceQuestion q3 = new MultipleChoiceQuestion(MultipleChoiceSelection.SINGLE);
        q3.setTitle("Type of animal");
        q3.setName("animal_type");
        q3.setDataType(DataType.STRING);
        q3.setId(3);
        q3.addItem("chicken", "chicken");
        q3.addItem("cow", "cow");
        q3.addItem("bird", "bird");
        form.addQuestion(q3);

        MultipleChoiceQuestion q4 = new MultipleChoiceQuestion(MultipleChoiceSelection.MULTIPLE);
        q4.setTitle("symptom");
        q4.setName("animal_type");
        q4.setDataType(DataType.STRING);
        q4.setId(4);
        q4.addItem("cough", "cough");
        q4.addItem("fever", "fever");
        q4.addItem("pain", "pain");
        form.addQuestion(q4);

        Page p1 = new Page(1);
        p1.addQuestion(q1);
        form.addPage(p1);
        Page p2 = new Page(2);
        p2.addQuestion(q2);
        form.addPage(p2);
        Page p3 = new Page(3);
        p3.addQuestion(q3);
        form.addPage(p3);
        Page p4 = new Page(4);
        form.addPage(p4);
        p4.addQuestion(q4);

        form.addTransition(new Transition(1, 2, "true"));
        form.addTransition(new Transition(2, 3, "true"));
        form.addTransition(new Transition(3, 4, "true"));

        return form;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.test_form, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private Page page;

        public PlaceholderFragment() {
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            Page page = (Page) arguments.get("page");
            return new PageView(getActivity(), page, false);
        }

    }
}
