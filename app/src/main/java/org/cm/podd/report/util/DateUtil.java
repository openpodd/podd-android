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
package org.cm.podd.report.util;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    final static String[] THAI_MONTH = {
            "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
            "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."
    };

    public static String convertToThaiDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dateNum = cal.get(Calendar.DATE);
        int month = cal.get(Calendar.MONTH);
        String thaiMonth = THAI_MONTH[month];
        int year = cal.get(Calendar.YEAR) + 543;
        return String.format("%d %s %d", dateNum, thaiMonth, year);
    }

    public static String convertToThaiDateTime(Date date) {
        String thaiDate = convertToThaiDate(date);
        String time = formatTime(date);
        return String.format("%s เวลา %s", thaiDate, time);
    }

    private static String formatTime(Date date) {
        return new SimpleDateFormat("HH:mm").format(date);
    }

    public static Date fromJsonDateString(String dateStr) {
        String[] formats = new String[]{
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'z'",
                "yyyy-MM-dd",
        };
        Locale defaultLocale = Locale.getDefault();
        Date date = null;

        for (String format : formats) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(format, defaultLocale);
                date = parser.parse(dateStr);
                break;
            } catch (ParseException e) {
                // Do nothing.
            }
        }

        return date;
    }
}
