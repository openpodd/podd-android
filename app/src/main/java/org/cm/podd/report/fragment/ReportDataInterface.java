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

package org.cm.podd.report.fragment;

import java.util.Date;

/**
 * Created by pphetra on 10/25/14 AD.
 */
public interface ReportDataInterface {

    public static final int TEST_ACTION = 3;
    public static final int CONFIRM_ACTION = 1;
    public static final int DRAFT_ACTION = 2;
    public static final int CANCEL_ACTION = 0;

    public Date getDate();

    public void setDate(Date date);

    public long getRegionId();

    public void setRegionId(long regionId);

    public String getRemark();

    public void setRemark(String remark);

    public boolean isDoneSubmit();

    public boolean isTestReport();

    public String getIncidentDateLabel();

    public void setDomainId(long domainId);

}
