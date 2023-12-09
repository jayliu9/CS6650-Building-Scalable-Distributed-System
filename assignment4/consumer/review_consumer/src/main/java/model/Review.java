package model;

import java.sql.Timestamp;
import java.util.UUID;

public class Review {
    private UUID reviewID;
    private int albumID;
    private boolean likeDislikeFlag;
    private Timestamp timestamp;

    // Constructors, Getters and Setters
    public Review() {
    }

    public Review(UUID reviewID, int albumID, boolean likeDislikeFlag) {
        this.reviewID = reviewID;
        this.albumID = albumID;
        this.likeDislikeFlag = likeDislikeFlag;
    }

    // Getters and setters for each field
    public UUID getReviewID() {
        return reviewID;
    }

    public void setReviewID(UUID reviewID) {
        this.reviewID = reviewID;
    }

    public int getAlbumID() {
        return albumID;
    }

    public void setAlbumID(int albumID) {
        this.albumID = albumID;
    }

    public boolean isLikeDislikeFlag() {
        return likeDislikeFlag;
    }

    public void setLikeDislikeFlag(boolean likeDislikeFlag) {
        this.likeDislikeFlag = likeDislikeFlag;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

