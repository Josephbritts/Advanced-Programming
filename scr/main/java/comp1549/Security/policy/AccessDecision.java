package comp1549.task2.security.policy;

import comp1549.task2.model.Decision;

public class AccessDecision {
    private final Decision decision;
    private final String reason;

    public AccessDecision(Decision decision, String reason) {
        this.decision = decision;
        this.reason = reason;
    }

    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public static AccessDecision allow(String reason) {
        return new AccessDecision(Decision.ALLOW, reason);
    }

    public static AccessDecision refuse(String reason) {
        return new AccessDecision(Decision.REFUSE, reason);
    }
}