package service;

import dao.AlbumDAO;
import model.Album;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public class AlbumService {
    private AlbumDAO albumDAO;

    public AlbumService() {
        this.albumDAO = new AlbumDAO();
    }

    public Album getAlbum(int albumId) throws SQLException {
        try (Connection conn = DatabaseService.getConnection()) {
            return albumDAO.getAlbumByKey(conn, albumId);
        }
    }

    public int createAlbum(Album album, InputStream inputStream, long imageSize) throws SQLException {
        try (Connection conn = DatabaseService.getConnection()) {
            return albumDAO.insertAlbum(conn, album, inputStream, imageSize);
        }
    }

}
