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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.activity.WebContentActivity;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.StyleUtil;

import java.util.Date;

public class NotificationListFragment extends ListFragment {

    private static final String TAG = "NotificationListFragment";

    NotificationDataSource notificationDataSource;
    private NotificationCursorAdapter adapter;

    private NotificationInterface notificationInterface;

    public NotificationListFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        notificationInterface = (NotificationInterface) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationDataSource = new NotificationDataSource(getActivity());
        Tracker tracker = ((PoddApplication) getActivity().getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
        tracker.setScreenName("NotificationList");
        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    public void refreshAdapter() {
        adapter = new NotificationCursorAdapter(getActivity(), notificationDataSource.getAll(), false);
        setListAdapter(adapter);

        notificationInterface.refreshNotificationCount();
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
        intent.putExtra("id", id);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationDataSource.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup parent = (ViewGroup) listView.getParent();
        TextView emptyText = (TextView) getActivity().getLayoutInflater().inflate(R.layout.empty_text, null);

        emptyText.setTypeface(StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL));
        emptyText.setText(R.string.no_news_update_text);
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
            int seen = cursor.getInt(cursor.getColumnIndex("seen"));

            TextView titleTextView = (TextView) view.findViewById(R.id.title);
            titleTextView.setText(title);
            titleTextView.setTypeface(typeFace);

            TextView dateTextView = (TextView) view.findViewById(R.id.date);
            Date date = new Date(time);
            dateTextView.setText(DateUtil.formatLocaleDateTime(date));
            dateTextView.setTypeface(typeFace);

            TextView newTextView = (TextView) view.findViewById(R.id.new_label);
            newTextView.setTypeface(typeFace);
            newTextView.setVisibility(seen == Report.TRUE ? View.GONE : View.VISIBLE);
        }
    }
}
