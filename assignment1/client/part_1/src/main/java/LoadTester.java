import io.swagger.client.ApiClient;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.AlbumInfo;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This program (Client1) simulates load testing on an API endpoint by dispatching multiple threads to make POST and GET calls.
 */
public class LoadTester {
    private static final int INIT_THREADS = 10;
    private static final int INIT_REQUEST_PAIRS = 100;
    private static final int REQUEST_PAIRS_PER_THREAD = 1000;
    private static final File IMAGE_FILE = new File("image_example.png");


    public static void main(String[] args) {
        // Check if the adequate number of command-line arguments are provided
        if (args.length < 4) {
            System.err.println("Usage: LoadTester <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
            return;
        }

        // Parse the test parameters from the command-line arguments
        int threadGroupSize = Integer.parseInt(args[0]);
        int numThreadGroups = Integer.parseInt(args[1]);
        int delay = Integer.parseInt(args[2]) * 1000;
        String ipAddr = args[3];

        // Initialize the API client and set its configurations
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(ipAddr);
        apiClient.setReadTimeout(60000);
        DefaultApi apiInstance = new DefaultApi(apiClient);

        // Create a thread pool executor
        ExecutorService executor = Executors.newFixedThreadPool(numThreadGroups * threadGroupSize);

        // Initialization phase: run some threads to "warm-up" the system
        CountDownLatch initLatch = new CountDownLatch(INIT_THREADS);
        runThreads(executor, apiInstance, INIT_THREADS, INIT_REQUEST_PAIRS, initLatch);

        // Wait for the initialization phase to complete
        try {
            initLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Begin the main testing phase
        CountDownLatch totalLatch = new CountDownLatch(numThreadGroups * threadGroupSize);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreadGroups; i++) {
            runThreads(executor, apiInstance, threadGroupSize, REQUEST_PAIRS_PER_THREAD, totalLatch);
            if (i < numThreadGroups - 1) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Await completion of all threads in the pool
        try {
            totalLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        // Properly shut down the executor service
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("The thread pool did not terminate properly");
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Compute and display test results
        double wallTime = (endTime - startTime) / 1000;
        long totalRequests = numThreadGroups * threadGroupSize * REQUEST_PAIRS_PER_THREAD * 2;
        double throughput = totalRequests / wallTime;
        System.out.println("threadGroupSize: " + threadGroupSize + ", numThreadGroups: " + numThreadGroups + ", delay: " + delay / 1000);
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + throughput + " requests/second");
    }


    /**
     * Schedules a specified number of threads to execute API call tasks.
     *
     * @param executor      The executor service used for managing thread execution.
     * @param apiInstance   The API instance used for making the actual calls.
     * @param numOfThreads  The number of threads to be scheduled for the task.
     * @param apiPairCount  The number of API pairs (POST followed by GET) each thread should execute.
     * @param latch         The countdown latch used to synchronize the completion of the threads.
     */
    private static void runThreads(ExecutorService executor, DefaultApi apiInstance, int numOfThreads, int apiPairCount, CountDownLatch latch) {
        for (int i = 0; i < numOfThreads; i++) {
            executor.submit(() -> {
                apiCallTask(apiInstance, apiPairCount);
                latch.countDown();
            });
        }
    }

    /**
     * Performs the API calls, including a retry logic if the call fails.
     *
     * @param apiInstance   The API instance used for making the actual calls.
     * @param apiPairCount  The number of API pairs (POST followed by GET) to be executed.
     */
    private static void apiCallTask(DefaultApi apiInstance, int apiPairCount) {
        AlbumsProfile profile = new AlbumsProfile();

        for (int i = 0; i < apiPairCount; i++) {
            // Retry for POST API call
            int retryCountPost = 0;
            boolean successPost = false;
            while (retryCountPost < 5 && !successPost) {
                try {
                    ApiResponse<ImageMetaData> responsePost = apiInstance.newAlbumWithHttpInfo(IMAGE_FILE, profile);
                    int postResStatusCode = responsePost.getStatusCode();
                    if (postResStatusCode >= 400 && postResStatusCode < 600) {
                        retryCountPost++;
                        if (retryCountPost >= 5) {
                            // Log or handle max retry attempts reached
                            System.out.println("Max retry attempts reached for POST call.");
                        }
                    } else {
                        successPost = true;
                    }
                } catch (Exception e) {
                    retryCountPost++;
                    if (retryCountPost >= 5) {
                        e.printStackTrace();
                    }
                }
            }

            // Retry for GET API call
            int retryCountGet = 0;
            boolean successGet = false;
            while (retryCountGet < 5 && !successGet) {
                try {
                    ApiResponse<AlbumInfo> responseGet = apiInstance.getAlbumByKeyWithHttpInfo("TestAlbumId");
                    int getResStatusCode = responseGet.getStatusCode();
                    if (getResStatusCode >= 400 && getResStatusCode < 600) {
                        retryCountGet++;
                        if (retryCountGet >= 5) {
                            // Log or handle max retry attempts reached
                            System.out.println("Max retry attempts reached for GET call.");
                        }
                    } else {
                        successGet = true;
                    }
                } catch (Exception e) {
                    retryCountGet++;
                    if (retryCountGet >= 5) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
