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

import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WebContentUtil {

    public static void launch(WebView webView, String title, String body) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        WebSettings webSettings = webView.getSettings();

        // For hide zoom button: must use android API at least level 11
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.loadDataWithBaseURL("file:///android_asset",
                buildHtml(title, body), "text/html; charset=UTF-8", "utf-8", null);

        // keep compat
        webView.setInitialScale((int) (100 * webView.getScale()));
    }

    private static String buildHtml(String title, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html lang=\"th\">");
        sb.append("<head>");
        sb.append("<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=2.0; user-scalable=yes\" />");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
        sb.append("<link href=\"file:///android_asset/style.css\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div class=\"wrapper\">");
        sb.append(content);
        sb.append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
