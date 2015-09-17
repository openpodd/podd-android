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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by pphetra on 9/29/14 AD.
 */
public enum DataType {

    STRING() {
        @Override
        public Object parseFromString(String value) {
            return value;
        }

        @Override
        public String toString(Object value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }
    },

    INTEGER() {
        @Override
        public Object parseFromString(String value) {
            if (value.equals("")) {
                return 0;
            }
            return Integer.parseInt(value);
        }

        @Override
        public String toString(Object value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }
    },

    DATE() {
        @Override
        public Object parseFromString(String value) {
            // support format yyyy-mm-dd
            if (value.equals("")) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                return sdf.parse(value);
            } catch (ParseException e) {
                return null;
            }
        }

        @Override
        public String toString(Object value) {
            if (value == null || value.equals("")) {
                return "";
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format((Date) value);
        }
    },

    DOUBLE() {
        @Override
        public Object parseFromString(String value) {
            if (value.equals("")) {
                return 0.00;
            }
            return Double.parseDouble(value);
        }

        @Override
        public String toString(Object value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }
    };

    public abstract Object parseFromString(String value);
    public abstract String toString(Object value);

}
