package org.cm.podd.report.model;

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
    private String detail;
    private String explanation;
    private String flag;
    private String follow;
    private Date date;
    private Date createdAt;
    private Date updatedAt;
    private int state;
    private String stateCode;

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

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getFlag() {
        return flag;
    }

    public void setFollow(String follow) {
        this.follow = follow;
    }

    public String getFollow() {
        return follow;
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

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getStateCode() {
        return stateCode;
    }

    public JSONObject getJSONObject() {
        try {
            return new JSONObject(explanation);
        } catch (JSONException e) {
            return null;
        }
    }

}
