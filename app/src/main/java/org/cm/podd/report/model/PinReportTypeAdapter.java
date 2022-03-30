package org.cm.podd.report.model;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.cm.podd.report.R;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.SharedPrefUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class PinReportTypeAdapter extends ArrayAdapter<ReportType> {

    Context context;

    public PinReportTypeAdapter(Context context, List<ReportType> items) {
        super(context, 0, items);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_pin_report_type, null);
        }

        TextView title = (TextView) view.findViewById(android.R.id.text1);
        ReportType type = (ReportType) getItem(position);
        title.setText(type.getName());

        TextView versionView = (TextView) view.findViewById(android.R.id.text2);
        versionView.setText("v" + type.getVersion());

        FontUtil.overrideFonts(context, view);
        view.setBackgroundResource(R.color.white);

        ImageView icon = (ImageView) view.findViewById(R.id.pin_report_type_image);
        icon.setBackgroundResource(R.drawable.ic_etc_report);

        return view;
    }
}
