package org.cm.podd.report.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.squareup.picasso.Picasso;

import org.cm.podd.report.R;
import org.cm.podd.report.TouchHighlightImageButton;
import org.cm.podd.report.activity.ImageActivity;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.ReportService;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private static final String TAG = "ReportAdapter";
    private OnItemClickListener mListener;

    public ArrayList<FeedItem> mDataSet = new ArrayList<FeedItem>();
    protected HashMap<Long, ViewHolder> viewHolderHashMap = new HashMap<Long, ViewHolder>();

    public static final int[] flagColors = new int[]{
            R.drawable.blank,
            R.drawable.flag_ignore,
            R.drawable.flag_ok,
            R.drawable.flag_contact,
            R.drawable.flag_follow,
            R.drawable.flag_case

    };

    public ReportAdapter(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final CardView cardView;
        private final ImageView flagView;
        private final TextView reportTypeView;
        private final TextView timeagoView;
        private final TextView descriptionView;

        public ViewHolder(View v) {
            super(v);

            context = v.getContext();
            cardView = (CardView) v.findViewById(R.id.feed_card);

            FontUtil.overrideFonts(context, v);

            flagView = (ImageView) v.findViewById(R.id.df_item_flag);
            reportTypeView = (TextView) v.findViewById(R.id.df_item_report_type);
            timeagoView = (TextView) v.findViewById(R.id.df_item_timeago);
            descriptionView = (TextView) v.findViewById(R.id.df_item_description);
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

        public TextView getTimeagoView() {
            return timeagoView;
        }

        public TextView getDescriptionView() {
            return descriptionView;
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
        FeedItem feedItem = mDataSet.get(position);
        JSONObject report = feedItem.getJSONObject();

        try {
            // flag
            String flagString = report.getString("flag");
            int flag = flagString.equals("") ? 0 : Integer.parseInt(flagString);
            viewHolder.getFlagView().setImageResource(flagColors[flag]);
            // report type
            viewHolder.getReportTypeView().setText(report.getString("reportTypeName"));
            // time ago
            Date date = DateUtil.fromJsonDateString(report.getString("date"));
            String dateStr = DateUtil.convertToThaiDate(date);
            viewHolder.getTimeagoView().setText(dateStr);
            // form data explanation
            viewHolder.getDescriptionView().setText(
                    stripHTMLTags(report.getString("formDataExplanation")));

            // set on click listener
            viewHolder.getCardView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onClick(view, position);
                }
            });

            viewHolderHashMap.put(feedItem.getItemId(), viewHolder);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON data", e);
        }
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int position) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.follow_up_item, viewGroup, false);

        return new ViewHolder(v);
    }

    /**
     * Interface for receiving click events from cells.
     */
    public interface OnItemClickListener {
        public void onClick(View view, int position);
    }

}