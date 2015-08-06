package org.cm.podd.report.model;

/**
 * Created by sudarat on 8/6/15 AD.
 */

public class Trigger {
    private String pattern;
    private int pageId;
    private String notificationText;

    public Trigger(String pattern, int pageId, String notificationText) {
        this.pattern = pattern;
        this.pageId = pageId;
        this.notificationText = notificationText;
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

}
