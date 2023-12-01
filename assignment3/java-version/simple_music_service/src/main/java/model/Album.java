package model;

import java.util.UUID;

public class Album {

    private UUID albumID;
    private String artist;
    private String title;
    private String year;

    public Album(UUID albumID, String artist, String title, String year) {
        this.albumID = albumID;
        this.artist = artist;
        this.title = title;
        this.year = year;
    }

    public UUID getAlbumID() {
        return albumID;
    }

    public void setAlbumID(UUID albumID) {
        this.albumID = albumID;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
