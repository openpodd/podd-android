package org.cm.podd.report.model;

import java.io.Serializable;

/**
 * Created by sudarat on 8/6/15 AD.
 */

public class Trigger implements Serializable {
    private String pattern;
    private int pageId;
    private boolean merge = false;

    private String notificationText;

    public Trigger(String pattern, int pageId, String notificationText) {
        this(pattern, pageId, notificationText, false);
    }

    public Trigger(String pattern, int pageId, String notificationText, boolean merge) {
        this.pattern = pattern;
        this.pageId = pageId;
        this.notificationText = notificationText;
        this.merge = merge;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public String getNotificationText() {
        return notificationText;
    }

    public void setNotificationText(String notificationText) {
        this.notificationText = notificationText;
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }
}
