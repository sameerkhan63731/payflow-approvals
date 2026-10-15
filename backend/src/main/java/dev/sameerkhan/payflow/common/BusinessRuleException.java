package dev.sameerkhan.payflow.common;

/**
 * A request that is well-formed but breaks a workflow rule (for example,
 * an initiator trying to approve their own payment).
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
