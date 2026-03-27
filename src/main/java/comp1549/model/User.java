package comp1549.model;

import java.util.Objects;

public class User {
    private final String id;
    private final String name;
    private final Role role;

    public User(String id, String name, Role role) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User ID must not be blank");
        }
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return id + " (" + role + ")";
    }
