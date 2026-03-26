package comp1549.task2.security.capability;

import java.util.Objects;

public final class Capability<T extends Operation> {
    private final Class<T> operationType;

    private Capability(Class<T> operationType) {
        this.operationType = Objects.requireNonNull(operationType);
    }

    public static Capability<ReadPermission> read() {
        return new Capability<>(ReadPermission.class);
    }

    public static Capability<WritePermission> write() {
        return new Capability<>(WritePermission.class);
    }

    public Class<T> getOperationType() {
        return operationType;
    }

    public String getOperationName() {
        String name = operationType.getSimpleName();
        return name.replace("Permission", "").toUpperCase();
    }
}