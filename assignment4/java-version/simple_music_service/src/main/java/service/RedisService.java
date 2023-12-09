package service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisService {
    private static JedisPool jedisPool;

    static {
        jedisPool = new JedisPool(System.getenv("REDIS_ADDRESS"), 6379);
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }

    public static void returnJedis(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}

