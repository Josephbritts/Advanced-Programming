package comp1549.logging;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import comp1549.model.User;
import comp1549.model.resource.Resource;
import comp1549.security.policy.AccessDecision;

public class AuditLog {
    private final List<LogEntry> entries = new ArrayList<>();

    public void record(User user, Resource resource, String operation, AccessDecision decision) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");
        Objects.requireNonNull(decision, "decision cannot be null");

        entries.add(new LogEntry(
                LocalDateTime.now(),
                user.getId(),
                user.getRole(),
                resource.getName(),
                operation,
                decision.getDecision(),
                decision.getReason()
        ));
    }

    public List<LogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void printAll() {
        for (LogEntry entry : entries) {
            System.out.println(entry);
        }
    }
}