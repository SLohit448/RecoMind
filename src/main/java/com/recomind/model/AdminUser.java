package com.recomind.model;

/**
 * Represents an admin user. Extends {@link User} and sets the role to ADMIN.
 */
public class AdminUser extends User {
    public AdminUser() {
        super();
        setRoleId(Role.ADMIN.ordinal());
    }

    public AdminUser(Long id, String email, String passwordHash, boolean active) {
        super(id, email, passwordHash, active, Role.ADMIN.ordinal());
    }
}
