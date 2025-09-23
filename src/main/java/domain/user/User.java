package domain.user;

public final class User {
    private final long id;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final String fullName;
    private final Role role;

    public User(long id, String username, String passwordHash, String email, String fullName, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    // Constructor for backward compatibility with existing authentication-focused usage
    public User(long id, String username, String passwordHash, String email, Role role) {
        this(id, username, passwordHash, email, null, role);
    }

    public long id() { return id; }
    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public String email() { return email; }
    public String fullName() { return fullName; }
    public Role role() { return role; }
}