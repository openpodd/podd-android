package org.cm.podd.report.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import org.cm.podd.report.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Created by siriwat on 2/17/15.
 */
public class DashboardFeedFragment extends SwipeRefreshFragment {

    private static final String TAG = "DashboardFeedFragment";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected ReportAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dashboard_feed, container, false);
        rootView.setTag(TAG);

        // set fragmentView to let super class know what to do next.
        mFragmentView = rootView;

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.dashboard_feed_list);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new ReportAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Define to let super class setOnRefreshListener() for us.
        mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh");
                onRefreshComplete();
            }
        };

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void onRefreshComplete() {
        Log.d(TAG, "onRefreshComplete");
        setRefreshing(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}
