package org.cm.podd.report.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sudarat on 4/10/2017 AD.
 */

public class CustomFilterUtil {
    public static class FilterWord {
        String key;
        String value;

        public FilterWord(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            String[] values = key.split("\\|");
            key = key.replaceAll(" ", "");
            if (values.length > 1) {
                key = values[0].replaceAll(" ", "");
            }
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public ArrayList<String> getStringByKey(String json, String key, FilterWord[] filterLevels) {
        String[] values = key.split("\\|");
        key = key.replaceAll(" ", "");
        if (values.length > 1) {
            key = values[0].replaceAll(" ", "");
        }

        ArrayList<String> listData = new ArrayList<String>();
        if (json != null) {
            try {
                JSONArray items = new JSONArray(json);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    boolean checked = true;
                    if (filterLevels != null && filterLevels.length > 0) {
                        for (int j = 0; j < filterLevels.length; j++) {
                            if (filterLevels[j] != null) {
                                String _itemValue = item.getString(filterLevels[j].getKey());
                                String _realValue = filterLevels[j].getValue();
                                if (!_itemValue.equalsIgnoreCase(_realValue)) {
                                    checked = false;
                                }
                            }
                        }
                    }
                    if (checked) {
                        String name = item.getString(key);
                        if (!listData.contains(name)) {
                            listData.add(name);
                        }
                    }
                }

            } catch (JSONException e) {

            }
        }

        return listData;
    }
}
