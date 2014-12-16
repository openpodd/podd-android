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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

/**
 * Created by pphetra on 10/10/14 AD.
 */
public class Report {

    public static int TRUE = 1;
    public static int FALSE = 0;

    private long id;
    private long type;
    private java.util.Date date;
    private java.util.Date startDate;
    private long regionId;
    private String remark;
    private int negative;
    private int draft;
    private int submit;

    private String guid;

    private double latitude = 0.00;
    private double longitude = 0.00;

    private String formData;
    private int reportTypeVersion;

    public Report(long id, long type, Date date, int negative, int draft, int submit) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.negative = negative;
        this.draft = draft;
        this.submit = submit;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getNegative() {
        return negative;
    }

    public void setNegative(int negative) {
        this.negative = negative;
    }

    public int getDraft() {
        return draft;
    }

    public void setDraft(int draft) {
        this.draft = draft;
    }

    public int getSubmit() {
        return submit;
    }

    public void setSubmit(int submit) {
        this.submit = submit;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public long getRegionId() {
        return regionId;
    }

    public void setRegionId(long regionId) {
        this.regionId = regionId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public JSONObject getSubmitJSONFormData() throws JSONException {
        JSONObject src = new JSONObject(getFormData());
        JSONObject target = new JSONObject();

        Iterator<String> keys = src.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            // split prefix path
            String[] tmp = key.split("@@@");
            String targetName = tmp[1];

            // check if target already has targetName
            if (target.has(targetName)) {
                // merge case
                String oldValue = target.getString(targetName);
                String newValue = src.getString(key);
                if (newValue != null && newValue.length() > 0) {
                    target.put(targetName, oldValue + "," + newValue);
                }

            } else {
                Object newValue = src.get(key);
                if (newValue instanceof String) {
                    String stringValue = (String) newValue;
                    if (stringValue != null && stringValue.length() > 0) {
                        target.put(targetName, newValue);
                    }
                } else {
                    target.put(targetName, newValue);
                }
            }

        }

        return target;
    }

    public int getReportTypeVersion() {
        return reportTypeVersion;
    }

    public void setReportTypeVersion(int reportTypeVersion) {
        this.reportTypeVersion = reportTypeVersion;
    }

}
