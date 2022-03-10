package org.cm.podd.report.model.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.cm.podd.report.model.Area;

import java.util.ArrayList;
import java.util.List;


public class AreaSearchAdapter extends ArrayAdapter<Area> {
    private final ArrayList<Area> items;
    private final ArrayList<Area> itemsAll;
    private ArrayList<Area> suggestions;
    private int viewResourceId;

    @SuppressWarnings("unchecked")
    public AreaSearchAdapter(Context context, int viewResourceId, ArrayList<Area> items) {
        super(context, viewResourceId, items);
        this.items = items;
        this.itemsAll = (ArrayList<Area>) items.clone();
        this.viewResourceId = viewResourceId;
        this.suggestions = new ArrayList<>();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            if (vi != null) {
                v = vi.inflate(viewResourceId, null);
            }
        }
        Area area = items.get(position);
        if (area != null && v != null) {
            TextView label1 = v.findViewById(android.R.id.text1);
            TextView label2 = v.findViewById(android.R.id.text2);
            if (label1 != null) {
                label1.setText(area.getAuthorityName());
            }
            if (label2 != null) {
                label2.setText(area.getDistrictAndProvinceName());
            }
        }
        assert v != null;
        return v;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    private Filter nameFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null && constraint.length() >= 3) {
                suggestions.clear();
                for (Area area: itemsAll) {
                    if (area.getAuthorityName().contains(constraint) ||
                    area.getDistrictName().contains(constraint) ||
                    area.getProvinceName().contains(constraint)) {
                        suggestions.add(area);
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            @SuppressWarnings("unchecked") List<Area> filterdList = (List<Area>) results.values;
            if (results.count > 0) {
                clear();
                for (Area area: filterdList) {
                    add(area);
                }
                notifyDataSetChanged();
            }
        }
    };
}
