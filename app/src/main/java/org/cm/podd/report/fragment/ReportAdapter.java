package org.cm.podd.report.fragment;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cm.podd.report.R;

/**
 * Created by siriwat on 2/17/15.
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    protected String[] mDataSet = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            textView = (TextView) v.findViewById(R.id.df_item_text_view);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.getTextView().setText(mDataSet[Math.min(position, mDataSet.length - 1)]);
    }

    @Override
    public int getItemCount() {
        return mDataSet.length;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int position) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.dashboard_feed_item, viewGroup, false);

        return new ViewHolder(v);
    }

}
