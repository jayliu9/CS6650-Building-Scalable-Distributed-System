package dao;

import model.Album;
import util.UUIDUtil;

import java.io.InputStream;
import java.sql.*;
import java.util.UUID;

public class AlbumDAO {


    public Album getAlbumByKey(Connection conn, int albumId) throws SQLException {
        String query = "SELECT * FROM Album WHERE albumID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, albumId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Album(
                            rs.getString("artist"),
                            rs.getString("title"),
                            rs.getString("year"));
                }
            }
        }
        return null;
    }

    public int insertAlbum(Connection conn, Album album, InputStream imageDataStream, long imageSize) throws SQLException {
        String insertSQL = "INSERT INTO Album(artist, title, year, imageData) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, album.getArtist());
            stmt.setString(2, album.getTitle());
            stmt.setString(3, album.getYear());
            stmt.setBinaryStream(4, imageDataStream, imageSize);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating album failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating album failed, no ID obtained.");
                }
            }
        }
    }
}
