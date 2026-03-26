package comp1549.task2.security.policy;

import comp1549.task2.model.User;
import comp1549.task2.model.resource.Resource;
import comp1549.task2.security.capability.Capability;
import comp1549.task2.security.capability.Operation;

public interface Policy {
    AccessDecision decide(User user, Resource resource, Capability<? extends Operation> capability);
}