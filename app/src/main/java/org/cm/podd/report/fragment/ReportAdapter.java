package org.cm.podd.report.fragment;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.model.FeedItem;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.util.DateUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by siriwat on 2/17/15.
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private static final String TAG = "ReportAdapter";

    protected ArrayList<FeedItem> mDataSet = new ArrayList<FeedItem>();

    private static final int[] flagColors = new int[]{
            R.drawable.icon_flag,
            R.drawable.icon_flag_ignore,
            R.drawable.icon_flag_ok,
            R.drawable.icon_flag_contact,
            R.drawable.icon_flag_follow,
            R.drawable.icon_flag_case
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final CardView cardView;
        private final ImageView flagView;
        private final TextView reportTypeView;
        private final TextView timeagoView;
        private final ImageView thumbView;
        private final TextView animalTypeView;
        private final TextView descriptionView;
        private final TextView addressView;

        public ViewHolder(View v) {
            super(v);

            context = v.getContext();
            cardView = (CardView) v.findViewById(R.id.feed_card);

            flagView = (ImageView) v.findViewById(R.id.df_item_flag);
            reportTypeView = (TextView) v.findViewById(R.id.df_item_report_type);
            timeagoView = (TextView) v.findViewById(R.id.df_item_timeago);
            thumbView = (ImageView) v.findViewById(R.id.df_item_thumbnail);
            animalTypeView = (TextView) v.findViewById(R.id.df_item_animal_type);
            descriptionView = (TextView) v.findViewById(R.id.df_item_description);
            addressView = (TextView) v.findViewById(R.id.df_item_address);
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

        public TextView getAnimalTypeView() {
            return animalTypeView;
        }

        public TextView getDescriptionView() {
            return descriptionView;
        }

        public ImageView getThumbView() {
            return thumbView;
        }

        public TextView getTimeagoView() {
            return timeagoView;
        }

        public TextView getAddressView() {
            return addressView;
        }
    }

    private String stripHTMLTags(String html) {
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
            // address
            viewHolder.getAddressView().setText(report.getString("administrationAreaAddress"));
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
                .inflate(R.layout.dashboard_feed_item, viewGroup, false);

        return new ViewHolder(v);
    }

}
