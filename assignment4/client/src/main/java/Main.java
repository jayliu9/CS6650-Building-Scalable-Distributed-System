import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.AlbumInfo;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;
import java.io.File;

public class Main {
  public static void main(String[] args) throws InterruptedException, ApiException {
    /**
     * NOTE: Unblock any part of the codes for your test cases
     */

//    LikeApi likeApi = new LikeApi();
//    likeApi.getApiClient().setBasePath("http://localhost:8080/CS6650Assignment3_Server_war_exploded");
//    likeApi.review("like", "10");

//     Local
//    MultiRequestExecutor executor = new MultiRequestExecutor(10, 10,
//        2, "http://localhost:8080/CS6650Assignment3_Server_war_exploded");
//    executor.execute();

    // Java Server
//    MultiRequestExecutor executor = new MultiRequestExecutor(10, 30,
//        2, "http://cs6650LoadBalancer-1902338145.us-west-2.elb.amazonaws.com:80/CS6650Assignment2-Server_war");
//    executor.execute();

    // Go Server
//    MultiRequestExecutor executor = new MultiRequestExecutor(10, 20,
//        2, "http://34.219.119.104:8080");
//    executor.execute();
  }
}
