package comp1549.task2.model.resource;

import comp1549.task2.logging.AuditLog;
import comp1549.task2.model.AccessScope;
import comp1549.task2.model.User;
import comp1549.task2.security.capability.Capability;
import comp1549.task2.security.capability.ReadPermission;
import comp1549.task2.security.capability.WritePermission;
import comp1549.task2.security.policy.AccessDecision;
import comp1549.task2.security.policy.Policy;

import java.util.Objects;

public abstract class Resource {
    private final String id;
    private final String name;
    private final AccessScope scope;
    private String content;

    protected Resource(String id, String name, AccessScope scope, String content) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.scope = Objects.requireNonNull(scope);
        this.content = Objects.requireNonNull(content);
    }

    public String read(User user,
                       Capability<ReadPermission> capability,
                       Policy policy,
                       AuditLog auditLog) {
        AccessDecision decision = policy.decide(user, this, capability);
        auditLog.record(user, this, capability.getOperationName(), decision);
        if (!decision.isAllowed()) {
            throw new SecurityException(decision.getReason());
        }
        return content;
    }

    public void write(User user,
                      String newContent,
                      Capability<WritePermission> capability,
                      Policy policy,
                      AuditLog auditLog) {
        AccessDecision decision = policy.decide(user, this, capability);
        auditLog.record(user, this, capability.getOperationName(), decision);
        if (!decision.isAllowed()) {
            throw new SecurityException(decision.getReason());
        }
        this.content = newContent;
    }

    public boolean isStaffWritable() {
        return false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AccessScope getScope() {
        return scope;
    }

    public String getContent() {
        return content;
    }
}