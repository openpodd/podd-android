package org.cm.podd.report.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.model.FeedAdapter;
import org.cm.podd.report.model.State;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;

import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by siriwat on 2/17/15.
 */
public class DashboardFeedFragment extends SwipeRefreshFragment implements FeedAdapter.OnItemClickListener {

    private static final String TAG = "DashboardFeedFragment";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected FeedAdapter mAdapter;
    protected RelativeLayout mEmpty;
    protected Button mEmptyRetryButton;
    protected SharedPrefUtil sharedPrefUtil;

    private final static int DEFAULT_PAGE_SIZE = 800;

    private FeedItemDataSource feedItemDataSource;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Receiving action " + intent.getAction());
            if (intent.getBooleanExtra("error", false)) {
                Crouton.makeText(getActivity(), R.string.fail_refresh_dashboard, Style.ALERT, R.id.errorArea).show();
            } else {
                refreshAdapter();
            }
            onRefreshComplete();
        }
    };

    private BroadcastReceiver mFlagSetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Long reportId = intent.getLongExtra("reportId", 0);
            Long flag = intent.getLongExtra("flag", 0);

            FeedAdapter.ViewHolder viewHolder = mAdapter.getViewHolderHashMap().get(reportId);
            if (viewHolder != null) {
                viewHolder.getFlagView().setImageResource(FeedAdapter.flagColors[flag.intValue()]);
            }
        }
    };


    private BroadcastReceiver mStateSetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Long reportId = intent.getLongExtra("reportId", 0);
            String stateCode = intent.getStringExtra("stateCode");

            FeedAdapter.ViewHolder viewHolder = mAdapter.getViewHolderHashMap().get(reportId);
            if (viewHolder != null) {

                int stateImage = R.drawable.blank;
                State[] stateColors = FeedAdapter.stateColors;
                for (int i = 0; i < stateColors.length; i++ ){
                    if (stateColors[i].getCode().equals(stateCode)) {
                        stateImage = stateColors[i].getColor();
                        break;
                    }
                }
                viewHolder.getFlagView().setImageResource(stateImage);
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        feedItemDataSource = new FeedItemDataSource(getActivity().getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mReceiver, new IntentFilter(FilterService.ACTION_QUERY_DONE));
        getActivity().registerReceiver(mFlagSetReceiver, new IntentFilter(ReportService.ACTION_FLAG_SET_DONE));
        getActivity().registerReceiver(mStateSetReceiver, new IntentFilter(ReportService.ACTION_STATE_SET_DONE));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        getActivity().unregisterReceiver(mFlagSetReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().unregisterReceiver(mStateSetReceiver);
        } catch (Exception ex) {}
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dashboard_feed, container, false);
        rootView.setTag(TAG);

        sharedPrefUtil = new SharedPrefUtil(getContext());

        // set fragmentView to let super class know what to do next.
        mFragmentView = rootView;

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

                if (RequestDataUtil.hasNetworkConnection(getActivity())) {
                    feedItemDataSource.clear();
                    setRefreshing(true);
                    String query = "negative:true AND testFlag: false AND date:last 15 days " + getFilterReportTypeQueryString();
                    FilterService.doQuery(container.getContext(), query, "0");
                } else {
                    refreshAdapter();
                    onRefreshComplete();
                }
            }
        };

        mEmpty = (RelativeLayout) rootView.findViewById(R.id.empty);
        mEmpty.setVisibility(View.GONE);
        mEmptyRetryButton = (Button) rootView.findViewById(R.id.empty_retry_button);
        mEmptyRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEmpty.setVisibility(View.GONE);
                mRefreshListener.onRefresh();
            }
        });

        FontUtil.overrideFonts(getActivity(), mEmpty);

        View wrappedView = super.onCreateView(inflater, container, savedInstanceState);
        mRefreshListener.onRefresh();

        return wrappedView;
    }

    private String getFilterReportTypeQueryString() {
        String types = sharedPrefUtil.getFilterReportType();
        ArrayList<String> bag = new ArrayList<String>();
        for (String type : types.split(",")) {
            bag.add("\"" + type + "\"");
        }
        return "typeName:(" + TextUtils.join(" OR ", bag) + ")";
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh");
        if (mFragmentView != null)
            mFragmentView.setVisibility(View.VISIBLE);
    }

    public void onPauseRefresh() {
        Log.d(TAG, "onPauseRefresh");
        setRefreshing(false);
        mFragmentView.setVisibility(View.GONE);
    }

    public void onRefreshComplete() {
        Log.d(TAG, "onRefreshComplete");
        setRefreshing(false);
        if (mAdapter.getmDataSet().size() == 0) {
            mEmpty.setVisibility(View.VISIBLE);
        } else {
            mEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View view, int position) {
        selectItem(position);
    }

    private void selectItem(int position) {
        Log.d(TAG, "Clicked on position: " + Integer.toString(position));

        Intent intent = new Intent(getActivity(), ReportViewActivity.class);
        intent.putExtra("id", mAdapter.getmDataSet().get(position).getItemId());
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
        mAdapter.setmDataSet(feedItemDataSource.latest(DEFAULT_PAGE_SIZE));
        mAdapter.notifyDataSetChanged();
    }
}
