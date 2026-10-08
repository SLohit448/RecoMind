package com.recomind.model;

public class RegularUser extends User {
    public RegularUser() {
        super();
        setRoleId((long) Role.USER.ordinal());
    }
}
