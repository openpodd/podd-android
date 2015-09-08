package org.cm.podd.report.model;

/**
 * Created by sudarat on 9/8/15 AD.
 */
public class State {

    private String name;
    private String code;
    private int color;

    public State(String code, int color) {
        this.code = code;
        this.color = color;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public int getColor() {
        return color;
    }

}
