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

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;

import java.util.Locale;

public class StyleUtil {

    public static String TH_LANG = new Locale("th").getLanguage();

    public static Typeface getDefaultTypeface(AssetManager assets, int type) {
        String lang = Resources.getSystem().getConfiguration().locale.getLanguage();
        // Use Thai font for Thai locale, other locales use English font
        // If using Thai font for all other locales, English texts get very big on a screen
        if (lang.equals(TH_LANG)) {
            return Typeface.createFromAsset(assets, type == Typeface.BOLD ? "CSPraJad-bold.otf" : "CSPraJad.otf");
        } else {
            return getSecondTypeface(assets, type);
        }
    }

    public static void setActionBarTitle(AppCompatActivity activity, String title) {
        activity.getSupportActionBar().setTitle(title);
    }

    public static Typeface getSecondTypeface(AssetManager assets, int type) {
        return Typeface.createFromAsset(assets, type == Typeface.BOLD ? "Roboto-Bold.ttf" : "Roboto-Light.ttf");
    }

    public static int convertDpToPx(float dp, DisplayMetrics displayMetrics) {
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
        return Math.round(pixels);
    }
}
