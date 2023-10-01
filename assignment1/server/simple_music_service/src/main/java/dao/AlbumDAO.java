package dao;

import model.AlbumInfo;

public class AlbumDAO {
    private static final AlbumInfo TEST_OBJECT = new AlbumInfo("Artist", "Title", "1988");
    public AlbumInfo getAlbumByKey(String albumId) {
        return TEST_OBJECT;
    }
}
