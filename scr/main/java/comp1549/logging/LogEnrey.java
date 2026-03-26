package comp1549.task2.logging;

import comp1549.task2.model.Decision;
import comp1549.task2.model.Role;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final LocalDateTime timestamp;
    private final String userId;
    private final Role role;
    private final String resourceName;
    private final String operation;
    private final Decision decision;
    private final String reason;

    public LogEntry(LocalDateTime timestamp,
                    String userId,
                    Role role,
                    String resourceName,
                    String operation,
                    Decision decision,
                    String reason) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.role = role;
        this.resourceName = resourceName;
        this.operation = operation;
        this.decision = decision;
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getOperation() {
        return operation;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return FORMATTER.format(timestamp) + ", " +
                userId + ", " +
                role + ", " +
                resourceName + ", " +
                operation + ", " +
                decision + " (" + reason + ")";
    }
}