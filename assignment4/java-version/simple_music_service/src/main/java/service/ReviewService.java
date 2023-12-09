package service;

import dao.ReviewDAO;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class ReviewService {
    private ReviewDAO reviewDAO;

    public ReviewService() {
        this.reviewDAO = new ReviewDAO();
    }

    public void initializeAlbumLikesDislikesInCache(String albumId) {
        try (Jedis jedis = RedisService.getJedis()) {
            jedis.hsetnx(albumId, "like", "0");
            jedis.hsetnx(albumId, "dislike", "0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAlbumLikesDislikesInCache(String albumId, String likeOrNot, int amount) {
        try (Jedis jedis = RedisService.getJedis()) {
            jedis.hincrBy(albumId, likeOrNot, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getAlbumLikesDislikes(String albumId, String likeOrNot) throws SQLException {
        try (Jedis jedis = RedisService.getJedis()) {
            String count = jedis.hget(albumId, likeOrNot);
            if (count != null) {
                return Integer.parseInt(count);
            } else {
                // If not found in Redis, search Database
                try (Connection conn = DatabaseService.getConnection()) {
                    Map<String, Integer> likesDislikes = reviewDAO.getLikesDislikesForAlbum(conn, Integer.parseInt(albumId));
                    jedis.hset(albumId, "like", likesDislikes.get("likes").toString());
                    jedis.hset(albumId, "dislike", likesDislikes.get("dislikes").toString());
                    return likesDislikes.get(likeOrNot);
                }
            }
        } catch (JedisException e) {
            System.out.println("Error in Redis operation");
            e.printStackTrace();
            return -1;
        }
    }

}

