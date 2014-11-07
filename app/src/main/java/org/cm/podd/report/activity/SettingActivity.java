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
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.util.SharedPrefUtil;

public class SettingActivity extends ActionBarActivity {

    SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        sharedPrefs = SharedPrefUtil.getPrefs(getApplicationContext());

        final EditText serverText = (EditText) findViewById(R.id.server_address);
        serverText.setText(SharedPrefUtil.getServerAddress());
        serverText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                sharedPrefs.edit().putString(
                        SharedPrefUtil.SERVER_ADDRESS, serverText.getText().toString());
                sharedPrefs.edit().commit();
            }
        });

        final EditText userText = (EditText) findViewById(R.id.username);
        userText.setText(SharedPrefUtil.getUserName());

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

    private void logout() {
        // clear access token
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(SharedPrefUtil.ACCESS_TOKEN_KEY);
        editor.commit();

        // clear all report data
        ReportDataSource db = new ReportDataSource(this);
        db.clearAllData();

        // Back to home, then redirect to login
        finish();
    }
}
