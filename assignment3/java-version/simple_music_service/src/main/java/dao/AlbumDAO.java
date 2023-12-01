package dao;

import model.Album;
import util.UUIDUtil;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class AlbumDAO {


    public Album getAlbumByKey(Connection conn, UUID albumId) {
        String query = "SELECT * FROM Album WHERE albumID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBytes(1, UUIDUtil.uuidToBytes(albumId));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Album(
                            albumId,
                            rs.getString("artist"),
                            rs.getString("title"),
                            rs.getString("year"));
                }
            }
        } catch (Exception e) {
            // handle exception
            e.printStackTrace();
        }
        return null;
    }

    public void insertAlbum(Connection conn, Album album, InputStream imageDataStream, long imageSize) {
        String insertSQL = "INSERT INTO Album(albumID, artist, title, year, imageData) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {

            stmt.setBytes(1, UUIDUtil.uuidToBytes(album.getAlbumID()));
            stmt.setString(2, album.getArtist());
            stmt.setString(3, album.getTitle());
            stmt.setString(4, album.getYear());
            stmt.setBinaryStream(5, imageDataStream, imageSize);

            stmt.executeUpdate();
        } catch (Exception e) {
            // handle exception
            e.printStackTrace();
        }
    }
}
