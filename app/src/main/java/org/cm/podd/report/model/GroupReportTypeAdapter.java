package org.cm.podd.report.model;

import android.content.Context;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.SharedPrefUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pphetra on 4/18/2017 AD.
 */

public class GroupReportTypeAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<ReportType> humanReportTypes = new ArrayList<ReportType>();
    private List<ReportType> animalReportTypes = new ArrayList<ReportType>();
    private List<ReportType> environmentReportTypes = new ArrayList<ReportType>();

    public GroupReportTypeAdapter(Context context, List<ReportType> items) {
        this.context = context;
        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(context);
        HashMap<Long, String> categoryMap = sharedPrefUtil.getCategoryMap();

        for (ReportType type : items) {

            String catCode = categoryMap.get(type.getId());
            boolean foundGroupInCatCode = false;
            if (catCode != null) {
                switch (catCode) {
                    case "animal":
                        animalReportTypes.add(type);
                        foundGroupInCatCode = true;
                        break;
                    case "human":
                        humanReportTypes.add(type);
                        foundGroupInCatCode = true;
                        break;
                    case "environment":
                        environmentReportTypes.add(type);
                        foundGroupInCatCode = true;
                        break;
                    case "pin":
                        // do nothing, all pinned report types will be used in PinReportTypeAdapter
                        foundGroupInCatCode = true;
                        break;
                }
            }

            if (!foundGroupInCatCode) {
                if (type.getName().matches("(.*)สัตว(.*)")) {
                    animalReportTypes.add(type);
                } else if (type.getName().matches("สิ่งแวดล้อม")) {
                    environmentReportTypes.add(type);
                } else if (type.getName().matches("(.*)ธรรมชาติ(.*)") ||
                        type.getName().matches(".*ลูกน้ำ.*")) {
                    environmentReportTypes.add(type);
                } else if (type.getName().matches("ไฟป่า")) {
                    environmentReportTypes.add(type);
                } else {
                    humanReportTypes.add(type);
                }
            }
        }
        WeightComparator comparator = new WeightComparator();
        Collections.sort(humanReportTypes, comparator);
        Collections.sort(animalReportTypes, comparator);
        Collections.sort(environmentReportTypes, comparator);

    }




    @Override
    public int getGroupCount() {
        return 4;
    }

    @Override
    public int getChildrenCount(int i) {
        switch (i) {
            case 1:
                return humanReportTypes.size();
            case 2:
                return animalReportTypes.size();
            case 3:
                return environmentReportTypes.size();
            case 0:
                return 0;
        }
        return 0;
    }

    @Override
    public Object getGroup(int i) {
        switch(i) {
            case 1:
                return context.getString(R.string.report_type_category_1);
            case 2:
                return context.getString(R.string.report_type_category_2);
            case 3:
                return context.getString(R.string.report_type_category_3);
            case 0:
                return context.getString(R.string.normal_case);
        }
        return "";
    }

    @Override
    public Object getChild(int i, int i1) {
        switch (i) {
            case 1:
                return humanReportTypes.get(i1);
            case 2:
                return animalReportTypes.get(i1);
            case 3:
                return environmentReportTypes.get(i1);
            case 0:
                return new ArrayList();
        }
        return null;
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return ((ReportType) getChild(i, i1)).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.group_report_type_parent, null);
        }
        view.setBackgroundColor(ContextCompat.getColor(context, getColor(i)));
        TextView text = (TextView) view.findViewById(R.id.group_report_type_title);
        text.setText(getGroup(i).toString());
        FontUtil.overrideFonts(context, view);

        ImageView icon = (ImageView) view.findViewById(R.id.group_report_type_image);
        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            icon.setBackgroundDrawable(ContextCompat.getDrawable(context, getDrawable(i)));
        } else {
            icon.setBackground(ContextCompat.getDrawable(context, getDrawable(i)));
        }
        return view;
    }

    private int getDrawable(int i) {
        switch(i) {
            case 1:
                return R.drawable.people;
            case 2:
                return R.drawable.animal;
            case 3:
                return R.drawable.tree;
            case 0:
                return R.drawable.zero;
        }
        return R.drawable.ic_cat_human;
    }

    private int getColor(int i) {
        switch(i) {
            case 1:
                return R.color.white;
            case 2:
                return R.color.white;
            case 3:
                return R.color.white;
            case 0:
                return R.color.white;
        }
        return R.color.white;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_report_type, null);
        }
        TextView title = (TextView) view.findViewById(android.R.id.text1);
        ReportType type = (ReportType) getChild(i, i1);
        title.setText(type.getName());

        TextView versionView = (TextView) view.findViewById(android.R.id.text2);
        versionView.setText("v" + type.getVersion());

        FontUtil.overrideFonts(context, view);

        if (i1 % 2 == 1) {
            view.setBackgroundResource(R.color.list_item_separator);
        } else {
            view.setBackgroundResource(R.color.white);
        }

        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    class WeightComparator implements Comparator<ReportType>{

        @Override
        public int compare(ReportType left, ReportType right) {
            return left.getWeight() > right.getWeight() ? 1 : (left.getWeight() < right.getWeight() ? -1 : 0);
        }
    }
}
