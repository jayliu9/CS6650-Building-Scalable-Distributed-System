import io.swagger.client.ApiException;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.Likes;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import redis.clients.jedis.Jedis;
import utils.RedisUtil;

public class AlbumReviewThread implements Runnable{

  private String ipAddress;
  private AtomicLong getReviewSuccess;
  private LikeApi likeApi;
  private AtomicBoolean threadGroupFinished;

  public AlbumReviewThread(String ipAddress, AtomicLong getReviewSuccess, AtomicBoolean threadGroupFinished) {
    this.ipAddress = ipAddress;
    this.getReviewSuccess = getReviewSuccess;
    this.likeApi = new LikeApi();
    this.likeApi.getApiClient().setBasePath(this.ipAddress);
    this.threadGroupFinished = threadGroupFinished;
  }

  @Override
  public void run() {
    while (!this.threadGroupFinished.get()) {
      performReviewRequest();
    }
  }

  private void performReviewRequest() {
    try (Jedis jedis = RedisUtil.getJedis()) {
      Long totKeys = jedis.dbSize();
      long randomID = Math.abs(new Random().nextLong()) % totKeys;
      Likes likes = likeApi.getLikes(randomID + "");
      System.out.println("id: " + randomID + ", likes: " + likes.getLikes() + ", dislikes: " + likes.getDislikes());
      this.getReviewSuccess.incrementAndGet();
    } catch (ApiException e) {
      e.printStackTrace();
    }
  }
}
