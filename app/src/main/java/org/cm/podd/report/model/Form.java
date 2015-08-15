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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public class Form implements Serializable {

    private List<Question> questions = new ArrayList<Question>();;
    private List<Page> pages = new ArrayList<Page>();;
    private List<Transition> transitions = new ArrayList<Transition>();

    private Map<Integer, Question> questionMap = new HashMap<Integer, Question>();
    private Map<Integer, Page> pageMap = new HashMap<Integer, Page>();
    private Map<String, Question> questionMapByName = new HashMap<String, Question>();

    private Trigger trigger;
    private int startPageId;

    public void addQuestion(Question question) {
        questions.add(question);
        questionMap.put(question.getId(), question);
        questionMapByName.put(question.getName(), question);
    }

    public void addPage(Page page) {
        pages.add(page);
        pageMap.put(page.getId(), page);
    }

    public void addTransition(Transition transition) {
        transitions.add(transition);
    }

    public Question getQuestion(int id) {
        return questionMap.get(id);
    }

    public Question getQuestion(String name) { return questionMapByName.get(name); }

    public Page getPage(int id) {
        return pageMap.get(id);
    }

    public int getStartPageId() {
        return startPageId;
    }

    public void setStartPageId(int startPageId) {
        this.startPageId = startPageId;
    }

    public List<Transition> getTransitionsForPage(int pageId) {
        ArrayList<Transition> results = new ArrayList<Transition>();
        for (Transition t: transitions) {
            if (t.getFromPage() == pageId) {
                results.add(t);
            }
        }
        return results;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public void setStartWithTrigger(boolean flag) {
        if (flag) {
            startPageId = getTrigger().getPageId();
        }
    }
}
