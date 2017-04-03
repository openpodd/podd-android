package org.cm.podd.report.model;

import android.graphics.Bitmap;

/**
 * Created by sudarat on 4/3/2017 AD.
 */

public class Config {
    private String system;
    private String key;
    private String value;

    public Config(String system, String key, String value) {
        this.system = system;
        this.key = key;
        this.value = value;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
