package dao;

import model.Review;
import util.UUIDUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ReviewDao {

    public void insertReview(Connection conn, Review review) {
        String insertSQL = "INSERT INTO Review (reviewID, albumID, likeDislikeFlag) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            stmt.setBytes(1, UUIDUtil.uuidToBytes(review.getReviewID()));
            stmt.setInt(2, review.getAlbumID());
            stmt.setBoolean(3, review.isLikeDislikeFlag());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}

