package org.cm.podd.report.model;

/**
 * Created by sudarat on 2/24/15 AD.
 */
public class Volunteer {
    private long id;
    private String name;
    private int totalReport;
    private int positiveReport;
    private int negativeReport;
    private String grade;

    public Volunteer(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDetail(int totalReport,  int positiveReport,  int negativeReport, String grade) {
        this.totalReport = totalReport;
        this.positiveReport = positiveReport;
        this.negativeReport = negativeReport;
        this.grade = grade;
    }

    @Override
    public String toString() {
        return name;
    }

}
