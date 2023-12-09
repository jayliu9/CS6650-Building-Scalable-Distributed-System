package dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ReviewDAO {

    public Map<String, Integer> getLikesDislikesForAlbum(Connection conn, int albumId) throws SQLException {
        String query = "SELECT "
                + "SUM(CASE WHEN likeDislikeFlag = TRUE THEN 1 ELSE 0 END) AS likes, "
                + "SUM(CASE WHEN likeDislikeFlag = FALSE THEN 1 ELSE 0 END) AS dislikes "
                + "FROM Review WHERE albumID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, albumId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Integer> likesDislikes = new HashMap<>();
                    likesDislikes.put("likes", rs.getInt("likes"));
                    likesDislikes.put("dislikes", rs.getInt("dislikes"));
                    return likesDislikes;
                }
            }
        }
        Map<String, Integer> emptyResult = new HashMap<>();
        emptyResult.put("likes", 0);
        emptyResult.put("dislikes", 0);
        return emptyResult;
    }
}
