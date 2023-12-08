package service;

import dao.ReviewDao;
import model.Review;

import java.sql.Connection;
import java.sql.SQLException;

public class ReviewService {
    private ReviewDao reviewDao;

    public ReviewService() {
        this.reviewDao = new ReviewDao();
    }

    public void createReview(Review review) throws SQLException {
        try (Connection conn = DatabaseService.getConnection()) {
            reviewDao.insertReview(conn, review);
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
