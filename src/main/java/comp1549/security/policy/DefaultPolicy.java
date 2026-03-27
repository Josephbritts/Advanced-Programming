package comp1549.security.policy;

import comp1549.model.AccessScope;
import comp1549.model.Role;
import comp1549.model.User;
import comp1549.model.resource.Resource;
import comp1549.security.capability.Capability;
import comp1549.security.capability.Operation;
import comp1549.security.capability.ReadPermission;
import comp1549.security.capability.WritePermission;

public class DefaultPolicy implements Policy {

    @Override
    public AccessDecision decide(User user, Resource resource, Capability<? extends Operation> capability) {
        if (capability.getOperationType().equals(ReadPermission.class)) {
            return decideRead(user, resource);
        }
        if (capability.getOperationType().equals(WritePermission.class)) {
            return decideWrite(user, resource);
        }
        return AccessDecision.refuse("Unknown capability");
    }

    private AccessDecision decideRead(User user, Resource resource) {
        Role role = user.getRole();
        AccessScope scope = resource.getScope();

        return switch (scope) {
            case PUBLIC -> AccessDecision.allow("PUBLIC resources are readable by everyone");
            case INTERNAL -> {
                if (role == Role.STUDENT || role == Role.STAFF || role == Role.ADMIN) {
                    yield AccessDecision.allow("INTERNAL resource readable by STUDENT/STAFF/ADMIN");
                }
                yield AccessDecision.refuse("GUEST cannot read INTERNAL resources");
            }
            case CONFIDENTIAL -> {
                if (role == Role.STAFF || role == Role.ADMIN) {
                    yield AccessDecision.allow("CONFIDENTIAL resource readable by STAFF/ADMIN");
                }
                yield AccessDecision.refuse("Only STAFF/ADMIN can read CONFIDENTIAL resources");
            }
        };
    }

    private AccessDecision decideWrite(User user, Resource resource) {
        Role role = user.getRole();

        if (role == Role.ADMIN) {
            return AccessDecision.allow("ADMIN can write all resources");
        }

        if (role == Role.STAFF && resource.isStaffWritable()) {
            return AccessDecision.allow("STAFF can write selected resources");
        }

        return AccessDecision.refuse(role + " is not allowed to write this resource");
    }
}