package com.recomind.model;

import java.sql.Timestamp;

public class Interaction {
    private long id;
    private long userId;
    private long itemId;
    private String type; // VIEW, CLICK, LIKE, DISLIKE, RATING, PURCHASE
    private Integer rating; // nullable
    private Timestamp createdAt;

    public Interaction() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
