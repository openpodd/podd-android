package org.cm.podd.report.model;

/**
 * Created by sudarat on 2/24/15 AD.
 */
public class TimeRange {
    private int startTime;
    private int endTime;
    private int totalReport;

    public TimeRange(int startTime, int endTime, int totalReport) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalReport = totalReport;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) { this.endTime = endTime; }

    public int getTotalReport() { return totalReport; }

    public void setTotalReport(int deathCount) {this.totalReport = totalReport;}

    @Override
    public String toString() {
        return startTime + ":00 - " + endTime + ":00";
    }
}