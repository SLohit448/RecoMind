package com.recomind.util;

/** Names of the HttpSession attributes and security constants shared by filters and servlets. */
public final class SessionKeys {
    public static final String USER_ID = "userId";
    public static final String EMAIL = "email";
    public static final String ROLE = "role";
    public static final String CSRF = "csrfToken";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final int SESSION_TIMEOUT_SECONDS = 1800;

    private SessionKeys() { }
}