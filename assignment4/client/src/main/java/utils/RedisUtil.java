package utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
  private static JedisPool jedisPool;

  static {
    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    jedisPoolConfig.setMaxTotal(50);
    jedisPoolConfig.setMinIdle(10);
    jedisPool = new JedisPool(jedisPoolConfig, "localhost", 6379);
  }

  private RedisUtil() {}

  public static Jedis getJedis() {
    return jedisPool.getResource();
  }

  public static void returnJedis(Jedis jedis) {
    if (jedis != null) {
      jedis.close();
    }
  }
}
