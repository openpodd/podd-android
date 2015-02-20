package org.cm.podd.report.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by siriwat on 2/19/15.
 */
public class FeedItem {

    private static final String TAG = "FeedItem";

    private long id = 0;
    private long itemId;
    private String type;
    private String jsonString;
    private Date date;
    private Date createdAt;
    private Date updatedAt;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return this.date;
    }

    public void setCreatedAt(Date date) {
        this.createdAt = date;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public JSONObject getJSONObject() {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            return null;
        }
    }

}
