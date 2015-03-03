package org.cm.podd.report.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Created by siriwat on 2/17/15.
 */
public class DashboardFeedFragment extends SwipeRefreshFragment implements FeedAdapter.OnItemClickListener {

    private static final String TAG = "DashboardFeedFragment";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected FeedAdapter mAdapter;
    protected ProgressBar mProgressBar;

    private final static int DEFAULT_PAGE_SIZE = 800;

    private FeedItemDataSource feedItemDataSource;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Receiving action " + intent.getAction());
            refreshAdapter();
            onRefreshComplete();
        }
    };

    private BroadcastReceiver mFlagSetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Long reportId = intent.getLongExtra("reportId", 0);
            Long flag = intent.getLongExtra("flag", 0);

            FeedAdapter.ViewHolder viewHolder = mAdapter.viewHolderHashMap.get(reportId);
            if (viewHolder != null) {
                viewHolder.getFlagView().setImageResource(FeedAdapter.flagColors[flag.intValue()]);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        feedItemDataSource = new FeedItemDataSource(getActivity().getApplicationContext());
        getActivity().registerReceiver(mReceiver, new IntentFilter(FilterService.ACTION_QUERY_DONE));
        getActivity().registerReceiver(mFlagSetReceiver, new IntentFilter(ReportService.ACTION_FLAG_SET_DONE));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dashboard_feed, container, false);
        rootView.setTag(TAG);

        // set fragmentView to let super class know what to do next.
        mFragmentView = rootView;

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.dashboard_feed_list);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new FeedAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Define to let super class setOnRefreshListener() for us.
        mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh");
                setRefreshing(true);
                FilterService.doQuery(container.getContext(), "negative:true", null);
            }
        };

        View wrappedView = super.onCreateView(inflater, container, savedInstanceState);

        feedItemDataSource.clear();
        mRefreshListener.onRefresh();
        return wrappedView;
    }

    private void onRefreshComplete() {
        Log.d(TAG, "onRefreshComplete");
        setRefreshing(false);
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mReceiver);
        getActivity().unregisterReceiver(mFlagSetReceiver);
    }

    @Override
    public void onClick(View view, int position) {
        selectItem(position);
    }

    private void selectItem(int position) {
        Log.d(TAG, "Clicked on position: " + Integer.toString(position));

        Intent intent = new Intent(getActivity(), ReportViewActivity.class);
        intent.putExtra("id", mAdapter.mDataSet.get(position).getItemId());
        startActivity(intent);
    }

    protected boolean canViewScrollUp(View view) {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            // For ICS and above we can call canScrollVertically() to determine this
            return ViewCompat.canScrollVertically(mRecyclerView, -1);
        } else {
            // Pre-ICS we need to manually check the first visible item and the child view's top
            // value
            return (mRecyclerView.getVerticalScrollbarPosition() > 0
                    || mRecyclerView.getTop() < mRecyclerView.getPaddingTop());
        }
    }

    private void refreshAdapter() {
        mAdapter.mDataSet = feedItemDataSource.latest(DEFAULT_PAGE_SIZE);
        mAdapter.notifyDataSetChanged();
    }

}
