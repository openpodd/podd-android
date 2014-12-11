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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.WebContentActivity;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.Date;

public class NotificationListFragment extends ListFragment {

    private static final String TAG = "NotificationListFragment";
    public static final String RECEIVE_MESSAGE_ACTION = "podd.receive_message_action";

    NotificationDataSource notificationDataSource;
    private NotificationCursorAdapter adapter;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Receiving action " + intent.getAction());
            refreshAdapter();
        }
    };

    public NotificationListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationDataSource = new NotificationDataSource(getActivity());
        getActivity().registerReceiver(mMessageReceiver, new IntentFilter(RECEIVE_MESSAGE_ACTION));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshAdapter();
    }

    private void refreshAdapter() {
        adapter = new NotificationCursorAdapter(getActivity(), notificationDataSource.getAll(), false);
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor cursor = (Cursor) adapter.getItem(position);
        String title = cursor.getString(cursor.getColumnIndex("title"));
        String content = cursor.getString(cursor.getColumnIndex("content"));

        Intent intent = new Intent(getActivity(), WebContentActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationDataSource.close();
        getActivity().unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText("ไม่มีรายการข่าวสาร");
        listView.setEmptyView(emptyText);

        emptyText.setVisibility(View.GONE);
        parent.addView(emptyText);

        return view;
    }

    /**
     * List Adapter
     */
    private class NotificationCursorAdapter extends CursorAdapter {

        private Typeface typeFace;

        private NotificationCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            typeFace = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View rootView = LayoutInflater.from(context).inflate(R.layout.notification_list_item, parent, false);
            return rootView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String title = cursor.getString(cursor.getColumnIndex("title"));
            long time = cursor.getLong(cursor.getColumnIndex("created_at"));

            TextView titleTextView = (TextView) view.findViewById(R.id.title);
            titleTextView.setText(title);
            titleTextView.setTypeface(typeFace);

            TextView dateTextView = (TextView) view.findViewById(R.id.date);
            Date date = new Date(time);
            dateTextView.setText(DateUtil.convertToThaiDate(date));
            dateTextView.setTypeface(typeFace);
        }
    }
}
