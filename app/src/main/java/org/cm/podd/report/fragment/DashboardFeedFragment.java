package org.cm.podd.report.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.db.FeedItemDataSource;
import org.cm.podd.report.model.FeedAdapter;
import org.cm.podd.report.model.State;
import org.cm.podd.report.service.AdministrationAreaService;
import org.cm.podd.report.service.FilterService;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.service.SyncReportStateService;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

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
    protected ProgressBar mProgressBar;
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
                Crouton.makeText(getActivity(), R.string.fail_refresh_dashboard, Style.ALERT).show();
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
//                viewHolder.getFlagView().setImageResource(FeedAdapter.flagColors[flag.intValue()]);
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
        getActivity().registerReceiver(mReceiver, new IntentFilter(FilterService.ACTION_QUERY_DONE));
        getActivity().registerReceiver(mFlagSetReceiver, new IntentFilter(ReportService.ACTION_FLAG_SET_DONE));
        getActivity().registerReceiver(mStateSetReceiver, new IntentFilter(ReportService.ACTION_STATE_SET_DONE));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dashboard_feed, container, false);
        rootView.setTag(TAG);

        // update profile data.
        updateUserStatus();

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

                if (RequestDataUtil.hasNetworkConnection(getActivity())) {
                    startSyncReportStateService();

                    feedItemDataSource.clear();
                    setRefreshing(true);
                    FilterService.doQuery(container.getContext(), "negative:true AND testFlag: false AND date:last 15 days ", null);
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

    private void onRefreshComplete() {
        Log.d(TAG, "onRefreshComplete");
        setRefreshing(false);
        mProgressBar.setVisibility(View.GONE);

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

    private void updateUserStatus() {
        sharedPrefUtil = new SharedPrefUtil(getActivity().getApplicationContext());
        ProfileAsyncTask task = new ProfileAsyncTask() {
            @Override
            protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
                if (resp.getStatusCode() == 200) {
                    try {
                        JSONObject result = new JSONObject(resp.getRawData());

                        if (result.getString("status").equals("VOLUNTEER")) {
                            sharedPrefUtil.setIsVolunteer(true);
                        } else {
                            sharedPrefUtil.setIsVolunteer(false);
                        }
                        sharedPrefUtil.setCanSetFlag(result.getBoolean("canSetFlag"));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON data", e);
                    }
                }
            }
        };
        task.setContext(getActivity().getApplicationContext());
        task.execute();
    }

    public static class ProfileAsyncTask extends ReportService.ReportAsyncTask {
        private static final String ENDPOINT = "/users/profile";

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(String... params) {
            SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
            String accessToken = sharedPrefUtil.getAccessToken();

            return RequestDataUtil.get(ENDPOINT, "", accessToken);
        }
    }

    private void startSyncReportStateService() {
        Intent intent = new Intent(getActivity(), SyncReportStateService.class);
        getActivity().startService(intent);
    }
}
