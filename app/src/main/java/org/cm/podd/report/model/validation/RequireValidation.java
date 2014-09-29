package org.cm.podd.report.model.validation;

/**
 * Created by pphetra on 9/25/14 AD.
 */
public class RequireValidation<T> implements IValidation<T> {

    private String message;

    public RequireValidation() {
        this.message = "%s must be required";
    }

    public RequireValidation(String message) {
        this.message = message;
    }

    @Override
    public ValidationResult validate(T value) {
        if (value == null) {
            return new ValidationResult(false, message);
        }
        return SUCCESS;
    }
}
