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
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.widget.TextView;

import org.cm.podd.report.R;

public class StyleUtil {

    public static Typeface getDefaultTypeface(AssetManager assets, int type) {
        return Typeface.createFromAsset(assets, type == Typeface.BOLD ? "CSPraJad-bold.otf" : "CSPraJad.otf");
    }

    public static void setActionBarTitle(Activity activity, String title) {
        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {
            TextView titleView = (TextView) (activity.findViewById(actionBarTitleId));
            if (titleView != null) {
                titleView.setText(title);
                titleView.setTypeface(getDefaultTypeface(activity.getAssets(), Typeface.NORMAL));
                if (title == null) {
                    titleView.setCompoundDrawablesWithIntrinsicBounds(
                            activity.getResources().getDrawable(R.drawable.logo_podd), null, null, null);
                }
            }
        }
    }

    public static Typeface getSecondTypeface(AssetManager assets, int type) {
        return Typeface.createFromAsset(assets, type == Typeface.BOLD ? "Roboto-Bold.ttf" : "Roboto-Light.ttf");
    }
}
