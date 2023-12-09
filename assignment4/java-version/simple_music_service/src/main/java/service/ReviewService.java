package service;

import redis.clients.jedis.Jedis;

public class ReviewService {

    public void initializeAlbumLikesDislikes(String albumId) {
        try (Jedis jedis = RedisService.getJedis()) {
            jedis.hsetnx(albumId, "like", "0");
            jedis.hsetnx(albumId, "dislike", "0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAlbumLikesDislikes(String albumId, String likeOrNot, int amount) {
        try (Jedis jedis = RedisService.getJedis()) {
            String key = albumId;
            jedis.hincrBy(albumId, likeOrNot, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getAlbumLikesDislikes(String albumId, String likeOrNot) {
        try (Jedis jedis = RedisService.getJedis()) {
            String count = jedis.hget(albumId, likeOrNot);
            return count != null ? Integer.parseInt(count) : -1;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error processing get from Redis");
            return -1;
        }
    }

}

