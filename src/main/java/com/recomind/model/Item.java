package com.recomind.model;

public class Item {
    private long id;
    private String title;
    private long categoryId;
    private String tags;
    private String description;
    private String imageUrl;
    private double price;

    public Item() {}

    public Item(String title, long categoryId, double price) {
        this.title = title;
        this.categoryId = categoryId;
        this.price = price;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
