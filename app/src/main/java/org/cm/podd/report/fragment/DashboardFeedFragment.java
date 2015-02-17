package org.cm.podd.report.fragment;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
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
public class DashboardFeedFragment extends SwipeRefreshListFragment {

    private static final int LIST_ITEM_COUNT = 20;

    private Random random = new Random();

    public ArrayList<String> randomList(int count) {
        HashSet<String> items = new HashSet<String>();

        // Make sure that don't infinity loop
        count = Math.min(count, 100);

        while (items.size() < count) {
            items.add("Row #" + Integer.toString(random.nextInt(100)));
        }

        return new ArrayList<String>(items);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void onRefreshComplete(List<String> result) {
        // Remove all items from the ListAdapter, and then replace them with the new items
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) getListAdapter();
        adapter.clear();
        adapter.addAll(result);

        // Stop the refreshing indicator
        setRefreshing(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListAdapter adapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                randomList(LIST_ITEM_COUNT));

        // Set the adapter between the ListView and its backing data.
        setListAdapter(adapter);

        // Set color schemes.
        setColorScheme(R.color.color_scheme_1_1, R.color.color_scheme_1_2,
                R.color.color_scheme_1_3, R.color.color_scheme_1_4);

        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshComplete(randomList(LIST_ITEM_COUNT));
            }
        });
    }

}
