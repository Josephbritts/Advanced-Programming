package comp1549.task2.logging;

import comp1549.task2.model.User;
import comp1549.task2.model.resource.Resource;
import comp1549.task2.security.policy.AccessDecision;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLog {
    private final List<LogEntry> entries = new ArrayList<>();

    public void record(User user, Resource resource, String operation, AccessDecision decision) {
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