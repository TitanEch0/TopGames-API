package xyz.titanecho.topgamesapi.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a review or advice for a server.
 */
public class Advice {
    private String id;
    private String username;
    private String advice;
    private int rating;

    @SerializedName("created_at")
    private String createdAt;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
