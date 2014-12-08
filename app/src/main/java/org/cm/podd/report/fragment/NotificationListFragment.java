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


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.db.NotificationDataSource;

public class NotificationListFragment extends ListFragment {

    private static final String TAG = "NotificationListFragment";

    NotificationDataSource notificationDataSource;
    private NotificationCursorAdapter adapter;

    public NotificationListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationDataSource = new NotificationDataSource(getActivity());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new NotificationCursorAdapter(getActivity(), notificationDataSource.getAll(), false);
        setListAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationDataSource.close();
    }

    /**
     * List Adapter
     */
    private class NotificationCursorAdapter extends CursorAdapter {

        private NotificationCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View rootView = LayoutInflater.from(context).inflate(R.layout.notification_list_item, parent, false);
            return rootView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView titleTextView = (TextView) view.findViewById(R.id.title);
            titleTextView.setText("xxk");
        }
    }
}
