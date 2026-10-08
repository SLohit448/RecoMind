package com.recomind.model;

import java.sql.Timestamp;

public class AuditLog {
    private long id;
    private long userId;
    private String action;
    private Timestamp timestamp;
    private String details;

    public AuditLog() {}

    public AuditLog(long userId, String action, Timestamp timestamp, String details) {
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
