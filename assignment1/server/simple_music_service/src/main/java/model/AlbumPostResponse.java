package model;

public class AlbumPostResponse {
    private String albumId;
    private String imageSize;

    public AlbumPostResponse(String albumId, String imageSize) {
        this.albumId = albumId;
        this.imageSize = imageSize;
    }

    public String getAlbumId() {
        return albumId;
    }

    public String getImageSize() {
        return imageSize;
    }
}
