package dto;

public class ImageMetaData {
    private String albumID;
    private String imageSize;


    public ImageMetaData(String albumID, String imageSize) {
        this.albumID = albumID;
        this.imageSize = imageSize;
    }

    public String getAlbumId() {
        return albumID;
    }

    public String getImageSize() {
        return imageSize;
    }
}
