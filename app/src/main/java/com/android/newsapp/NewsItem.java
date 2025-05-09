package com.android.newsapp;

public class NewsItem {
    private String title;
    private String description;
    private String url;
    private String imageUrl;
    private String publishedAt;

    public NewsItem(String title, String description, String url, String imageUrl, String publishedAt) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
    }

    // Add getters for all the fields
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    // Optional: Add setters if you need to modify the data later
}