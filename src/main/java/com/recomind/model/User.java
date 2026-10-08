package com.recomind.model;

public class User {
    private long id;
    private String email;
    private String passwordHash;
    private boolean active;
    private long roleId;

    public User() {}

    public User(String email, String passwordHash, boolean active, long roleId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.roleId = roleId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getRoleId() { return roleId; }
    public void setRoleId(long roleId) { this.roleId = roleId; }
}
