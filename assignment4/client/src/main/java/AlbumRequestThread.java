import exception.RetryLimitException;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.AlbumInfo;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import model.RequestRecord;
import model.RequestType;

public class AlbumRequestThread implements Runnable{

  private final String RETRY_LIMIT_EXCEPTION_MSG = "Exceeded retry limit, marked as failure request";
  private final int RETRY_LIMIT = 5;
  private final String IMG_PATH = "src/main/java/image/testimg.png";
  private DefaultApi apiInstance;
  private LikeApi likeApi;
  File imageFile;
  private Integer loopCnt;
  private String ipAddress;
  private CountDownLatch countDownLatch;
  private List<List<RequestRecord>> records;
  private List<RequestRecord> threadRecords;
  private AtomicLong success;

  public AlbumRequestThread(
      Integer loopCnt, String ipAddress, CountDownLatch countDownLatch,
      List<List<RequestRecord>> records, AtomicLong success) {
    this.loopCnt = loopCnt;
    this.ipAddress = ipAddress;
    this.countDownLatch = countDownLatch;
    this.records = records;
    this.threadRecords = Collections.synchronizedList(new ArrayList<>());
    this.success = success;
    this.apiInstance = new DefaultApi();
    this.likeApi = new LikeApi();
    this.apiInstance.getApiClient().setBasePath(this.ipAddress);
    this.likeApi.getApiClient().setBasePath(this.ipAddress);
    this.imageFile = new File(IMG_PATH);
  }

  @Override
  public void run() {
    for (int i = 0; i < this.loopCnt; i++) {
      try {
        String albumID = performPostRequest();
        reviewAlbum(albumID, true);
        reviewAlbum(albumID, true);
        reviewAlbum(albumID, false);
      } catch (RetryLimitException e) {
        e.printStackTrace();
      }
    }
    this.records.add(this.threadRecords);
    this.countDownLatch.countDown();
  }

  private void reviewAlbum(String albumID, boolean like) {
    String likeOrNot = like ? "like" : "dislike";
    try {
      likeApi.reviewWithHttpInfo(likeOrNot, albumID);
      this.success.incrementAndGet();
    } catch (ApiException e) {
      e.printStackTrace();
    }
  }

  private boolean performGetRequest() throws RetryLimitException{
    int errCnt = 0;
    long start = System.currentTimeMillis();
    while (errCnt < RETRY_LIMIT) {
      try {
        ApiResponse<AlbumInfo> res = apiInstance.getAlbumByKeyWithHttpInfo("1");
        if (res.getStatusCode() < 300) {
          this.success.incrementAndGet();
          long end = System.currentTimeMillis();
          long latency = end - start;
          RequestRecord record = RequestRecord.builder()
              .startTime(start)
              .requestType(RequestType.GET)
              .latency(latency)
              .responseCode(res.getStatusCode())
              .build();
          this.threadRecords.add(record);
          return true;
        }
        errCnt++;
      } catch (ApiException e) {
        errCnt++;
        e.printStackTrace();
      }
    }
    throw new RetryLimitException(RETRY_LIMIT_EXCEPTION_MSG);
  }

  private String performPostRequest() throws RetryLimitException {
    int errCnt = 0;
    long start = System.currentTimeMillis();
      while (errCnt < RETRY_LIMIT) {
        try {
          ApiResponse<ImageMetaData> res = apiInstance.newAlbumWithHttpInfo(
              imageFile, new AlbumsProfile());
          String albumID = res.getData().getAlbumID();
          if (res.getStatusCode() < 300) {
            this.success.incrementAndGet();
            long end = System.currentTimeMillis();
            long latency = end - start;
            RequestRecord record = RequestRecord.builder()
                .startTime(start)
                .requestType(RequestType.POST)
                .latency(latency)
                .responseCode(res.getStatusCode())
                .build();
            this.threadRecords.add(record);
            return albumID;
          }
          errCnt++;
        } catch (ApiException e) {
          errCnt++;
          e.printStackTrace();
        }
      }
      throw new RetryLimitException(RETRY_LIMIT_EXCEPTION_MSG);
  }
}
