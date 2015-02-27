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

public class Comment {

    private long id;
    private long reportId;
    private String message;
    private String fileUrl;
    private String avatarCreatedBy;
    private String createdBy;
    private String createdAt;

    public Comment(long id, long reportId, String message, String fileUrl, String avatarCreatedBy, String createdBy, String createdAt) {
        this.id = id;
        this.reportId = reportId;
        this.message = message;
        this.fileUrl = fileUrl;
        this.avatarCreatedBy = avatarCreatedBy;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getReportId() {
        return reportId;
    }

    public void setReportId(long reportId) {
        this.reportId = reportId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl;}

    public String getAvatarCreatedBy() {
        return avatarCreatedBy;
    }

    public void setAvatarCreatedBy(String avatarCreatedBy) { this.avatarCreatedBy = avatarCreatedBy;}

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy;}

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) { this.createdAt = createdAt;}

    @Override
    public String toString() {
        return message;
    }

}
