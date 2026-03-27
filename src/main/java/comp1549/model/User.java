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

    public String getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }

    public User withRole(Role newRole) {
        return new User(this.id, this.name, newRole);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + " (" + role + ")";
    }
}