package org.cm.podd.report.model;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.cm.podd.report.R;
import org.cm.podd.report.fragment.DashboardFeedFragment;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by siriwat on 2/17/15.
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private static final String TAG = "FeedAdapter";
    private OnItemClickListener mListener;

    private ArrayList<FeedItem> mDataSet = new ArrayList<FeedItem>();
    private HashMap<Long, ViewHolder> viewHolderHashMap = new HashMap<Long, ViewHolder>();

//    public static final int[] flagColors = new int[]{
//        R.drawable.blank,
//        R.drawable.flag_ignore,
//        R.drawable.flag_ok,
//        R.drawable.flag_contact,
//        R.drawable.flag_follow,
//        R.drawable.flag_case,
//        R.drawable.flag_case
//    };

    public static final State[] stateColors = new State[]{
        new State("report", R.drawable.blank),
        new State("false-report", R.drawable.flag_ignore),
        new State("no-outbreak-identified", R.drawable.flag_ignore),
        new State("case", R.drawable.flag_contact),
        new State("3", R.drawable.flag_contact),
        new State("follow", R.drawable.flag_follow),
        new State("4", R.drawable.flag_follow),
        new State("suspect-outbreak", R.drawable.flag_case),
        new State("outbreak", R.drawable.flag_case),
        new State("5", R.drawable.flag_case),
        new State("finish", R.drawable.flag_ok),
    };

    public FeedAdapter(OnItemClickListener listener) {
        mListener = listener;
    }

    public ArrayList<FeedItem> getmDataSet() {
        return mDataSet;
    }

    public void setmDataSet(ArrayList<FeedItem> mDataSet) {
        this.mDataSet = mDataSet;
    }

    public HashMap<Long, ViewHolder> getViewHolderHashMap() {
        return viewHolderHashMap;
    }

    public void setViewHolderHashMap(HashMap<Long, ViewHolder> viewHolderHashMap) {
        this.viewHolderHashMap = viewHolderHashMap;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final CardView cardView;
        private final ImageView flagView;
        private final TextView reportTypeView;
        private final TextView timeagoView;
        private final TextView descriptionView;
        private final TextView addressView;
        private final RelativeLayout thumbnailViewWrapper;
        private final ImageView thumbnailView;

        public ViewHolder(View v) {
            super(v);

            context = v.getContext();
            cardView = (CardView) v.findViewById(R.id.feed_card);

            FontUtil.overrideFonts(context, v);

            flagView = (ImageView) v.findViewById(R.id.df_item_flag);
            reportTypeView = (TextView) v.findViewById(R.id.df_item_report_type);
            timeagoView = (TextView) v.findViewById(R.id.df_item_timeago);
            descriptionView = (TextView) v.findViewById(R.id.df_item_description);
            addressView = (TextView) v.findViewById(R.id.df_item_address);
            thumbnailViewWrapper = (RelativeLayout) v.findViewById(R.id.df_item_thumbnail_wrapper);
            thumbnailView = (ImageView) v.findViewById(R.id.df_item_thumbnail);
        }

        public CardView getCardView() {
            return cardView;
        }

        public Context getContext() {
            return context;
        }

        public ImageView getFlagView() {
            return flagView;
        }

        public TextView getReportTypeView() {
            return reportTypeView;
        }

        public TextView getDescriptionView() {
            return descriptionView;
        }

        public TextView getTimeagoView() {
            return timeagoView;
        }

        public TextView getAddressView() {
            return addressView;
        }

        public ImageView getThumbnailView() {
            return thumbnailView;
        }

        public RelativeLayout getThumbnailViewWrapper() {
            return thumbnailViewWrapper;
        }
    }

    public static String stripHTMLTags(String html) {
        String modifiedHtml = Html.fromHtml(html).toString();
        modifiedHtml = modifiedHtml.replaceAll("[\r\n]$", "");
        modifiedHtml = modifiedHtml.replaceAll("^[\r\n]", "");
        return modifiedHtml;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        FeedItem feedItem = getmDataSet().get(position);
        JSONObject report = feedItem.getJSONObject();

        try {

//            String flagString = report.getString("flag");
//            int flag = flagString.equals("") ? 0 : Integer.parseInt(flagString);
//            viewHolder.getFlagView().setImageResource(flagColors[flag]);

            // state
            String stateCode = report.getString("stateCode");

            int stateImage = R.drawable.blank;
            for (int i = 0; i < stateColors.length; i++ ){
                if (stateColors[i].getCode().equals(stateCode)) {
                    stateImage = stateColors[i].getColor();
                    break;
                }
            }
            viewHolder.getFlagView().setImageResource(stateImage);

            // report type
            viewHolder.getReportTypeView().setText(report.getString("reportTypeName"));
            // time ago
            Date date = DateUtil.fromJsonDateString(report.getString("date"));
            String dateStr = DateUtil.convertToThaiDate(date);
            viewHolder.getTimeagoView().setText(dateStr);
            // form data explanation
            viewHolder.getDescriptionView().setText(
                    stripHTMLTags(report.getString("formDataExplanation")));
            // address
            viewHolder.getAddressView().setText(report.getString("administrationAreaAddress"));
            
            // set on click listener
            viewHolder.getCardView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onClick(view, position);
                }
            });

            // set thumbnail.
            RelativeLayout thumbnailViewWrapper = viewHolder.getThumbnailViewWrapper();
            ImageView thumbnailView = viewHolder.getThumbnailView();
            String thumbnailUrl;
            try {
                thumbnailUrl = report.getString("firstImageThumbnail");
                if (!thumbnailUrl.isEmpty()) {
                    thumbnailViewWrapper.setVisibility(View.VISIBLE);

                    Picasso.with(viewHolder.getContext())
                            .load(thumbnailUrl)
                            .fit()
                            .centerCrop()
                            .into(thumbnailView);
                } else {
                    thumbnailViewWrapper.setVisibility(View.GONE);
                }
            } catch (JSONException e) {
                thumbnailViewWrapper.setVisibility(View.GONE);
            }

            getViewHolderHashMap().put(feedItem.getItemId(), viewHolder);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
        }
    }

    @Override
    public int getItemCount() {
        return getmDataSet().size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int position) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.dashboard_feed_item, viewGroup, false);

        return new ViewHolder(v);
    }

    /**
     * Interface for receiving click events from cells.
     */
    public interface OnItemClickListener {
        public void onClick(View view, int position);
    }

}
