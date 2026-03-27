package comp1549.security.capability;

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
        return operationType.getSimpleName().replace("Permission", "").toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Capability<?> other)) return false;
        return operationType.equals(other.operationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationType);
    }

    @Override
    public String toString() {
        return "Capability[" + getOperationName() + "]";
    }
}