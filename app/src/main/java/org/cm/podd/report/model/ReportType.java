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

/**
 * Created by pphetra on 10/10/14 AD.
 */
public class ReportType {

    private long id;
    private String name;
    private int version;
    private String definition;
    private int nextVersion;
    private double weight = 0.0;
    private int followable;
    private int followDay;
    private int isFollowAction;

    public ReportType(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
        this.nextVersion = version;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public int getNextVersion() {
        return nextVersion;
    }

    public void setNextVersion(int nextVersion) {
        this.nextVersion = nextVersion;
    }

    public boolean hasNextVersion() {
        return this.nextVersion > this.version;
    }
    @Override
    public String toString() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getFollowable() {
        return followable;
    }

    public void setFollowable(int followable) {
        this.followable = followable;
    }

    public void setFollowable(boolean followable) {
        if (followable) {
            this.followable = 1;
        } else {
            this.followable = 0;
        }
    }

    public boolean isFollowable() {
        return this.followable == 1;
    }

    public int getFollowDay() {
        return followDay;
    }

    public void setFollowDay(int followDay) {
        this.followDay = followDay;
    }

    public void setIsFollowAction(boolean followAction) {
        if (followAction) {
            this.isFollowAction = 1;
        } else {
            this.isFollowAction = 0;
        }
    }

    public void setIsFollowAction(int action) {
        this.isFollowAction = action;
    }

    public boolean isFollowAction() { return this.isFollowAction == 1; }

    public int getIsFollowAction() {
        return this.isFollowAction;
    }
}
