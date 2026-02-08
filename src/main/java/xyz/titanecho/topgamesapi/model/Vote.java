package xyz.titanecho.topgamesapi.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a vote cast by a user.
 */
public class Vote {
    private String id;
    private String username;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("claimed")
    private boolean isClaimed;

    @SerializedName("ip_address")
    private String ipAddress;

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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isClaimed() {
        return isClaimed;
    }

    public void setClaimed(boolean claimed) {
        isClaimed = claimed;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", isClaimed=" + isClaimed +
                '}';
    }
}
