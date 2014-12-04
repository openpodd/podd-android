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
package org.cm.podd.report.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;

public class SettingActivity extends ActionBarActivity {

    SharedPrefUtil sharedPrefUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefUtil = new SharedPrefUtil(getApplicationContext());

        setContentView(R.layout.activity_setting);

        Typeface face = StyleUtil.getDefaultTypeface(getAssets(), Typeface.NORMAL);

        final EditText userText = (EditText) findViewById(R.id.username);
        userText.setText(sharedPrefUtil.getFullName());
        userText.setTypeface(face);

        final EditText versionCodeText = (EditText) findViewById(R.id.version_code);
        versionCodeText.setText(String.valueOf(BuildConfig.VERSION_CODE));
        versionCodeText.setTypeface(face);

        final EditText versionNameText = (EditText) findViewById(R.id.version_name);
        versionNameText.setText(BuildConfig.VERSION_NAME);
        versionNameText.setTypeface(face);

        ((TextView) findViewById(R.id.username_label)).setTypeface(face);
        ((TextView) findViewById(R.id.app_version_code_label)).setTypeface(face);
        ((TextView) findViewById(R.id.app_version_name_label)).setTypeface(face);

        final SettingActivity me = this;

        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(me)
                    .setTitle("ยืนยันการออกจากระบบ")
                    .setMessage("ข้อมูลรายงานทั้งหมดจะถูกลบ และถูกยกเลิกการส่ง\n\n"
                            + "คุณต้องการออกจากระบบหรือไม่")
                    .setPositiveButton("ตกลง", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            logout();
                        }
                    })
                    .setNegativeButton("ยกเลิก", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, getString(R.string.title_activity_setting));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(0);
        actionBar.setLogo(R.drawable.arrow_left_with_pad);

        return true;
    }

    private void logout() {
        // clear access token
        sharedPrefUtil.clearAllData();

        // clear all report data
        ReportDataSource db = new ReportDataSource(this);
        db.clearAllData();

        // Back to home, then redirect to login
        finish();
    }
}
