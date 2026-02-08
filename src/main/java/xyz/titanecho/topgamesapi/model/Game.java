package xyz.titanecho.topgamesapi.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a game object from the Top-Games API.
 * This class models the data structure of a game as returned by the API.
 */
public class Game {
    private String id;
    private String name;
    private int rank;
    private String publisher;
    private double score;

    @SerializedName("cover_image_url")
    private String coverImageUrl;

    // Getters and Setters

    /**
     * @return The unique identifier of the game.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The name of the game.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The ranking of the game.
     */
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * @return The publisher of the game.
     */
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * @return The score or rating of the game.
     */
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * @return The URL of the game's cover image.
     */
    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    @Override
    public String toString() {
        return "Game{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", rank=" + rank +
                '}';
    }
}
