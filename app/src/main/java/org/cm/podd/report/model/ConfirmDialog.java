package org.cm.podd.report.model;

import java.io.Serializable;

/**
 * Created by pphetra on 17/1/2018 AD.
 */

public class ConfirmDialog implements Serializable {

    public static final String TAG_NAME = "confirmDialog";

    private String message;
    private String condition;

    public ConfirmDialog(String message, String condition) {
        this.message = message;
        this.condition = condition;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
