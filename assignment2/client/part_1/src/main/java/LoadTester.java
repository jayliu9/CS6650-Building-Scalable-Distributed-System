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
import java.util.concurrent.atomic.AtomicInteger;

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

        // Initialize variables to count the number of successful and failed requests
        AtomicInteger globalSuccessCount = new AtomicInteger(0);
        AtomicInteger globalFailureCount = new AtomicInteger(0);

        // Create a thread pool executor
        ExecutorService executor = Executors.newFixedThreadPool(numThreadGroups * threadGroupSize);

        // Initialization phase: run some threads to "warm-up" the system
        CountDownLatch initLatch = new CountDownLatch(INIT_THREADS);
        runThreads(executor, apiInstance, INIT_THREADS, INIT_REQUEST_PAIRS, initLatch, null, null);

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
            runThreads(executor, apiInstance, threadGroupSize, REQUEST_PAIRS_PER_THREAD, totalLatch, globalSuccessCount, globalFailureCount);
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
        double wallTime = (endTime - startTime) / 1000.0;
        int totalSuccessfulRequests = globalSuccessCount.get();
        int totalFailedRequests = globalFailureCount.get();
        double throughput = totalSuccessfulRequests / wallTime;

        System.out.println("threadGroupSize: " + threadGroupSize + ", numThreadGroups: " + numThreadGroups + ", delay: " + delay / 1000);
        System.out.println("Total Successful Requests: " + totalSuccessfulRequests);
        System.out.println("Total Failed Requests: " + totalFailedRequests);
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + throughput + " requests/second");
    }


    /**
     * Schedules a specified number of threads to execute API call tasks.
     * If global counters (globalSuccessCount and globalFailureCount) are provided,
     * this method will track and record the number of global successful and failed API calls.
     * Otherwise, it will execute the tasks without updating global counters.
     *
     * @param executor      The executor service used for managing thread execution.
     * @param apiInstance   The API instance used for making the actual calls.
     * @param numOfThreads  The number of threads to be scheduled for the task.
     * @param apiPairCount  The number of API pairs (POST followed by GET) each thread should execute.
     * @param latch         The countdown latch used to synchronize the completion of the threads.
     * @param globalSuccessCount   An atomic counter to track the total number of successful API calls across all threads.
     *                             If null, successful API calls won't be tracked globally.
     * @param globalFailureCount   An atomic counter to track the total number of failed API calls across all threads.
     *                             If null, failed API calls won't be tracked globally.
     */
    private static void runThreads(ExecutorService executor, DefaultApi apiInstance, int numOfThreads, int apiPairCount, CountDownLatch latch, AtomicInteger globalSuccessCount, AtomicInteger globalFailureCount) {
        for (int i = 0; i < numOfThreads; i++) {
            executor.submit(() -> {
                apiCallTask(apiInstance, apiPairCount, globalSuccessCount, globalFailureCount);
                latch.countDown();
            });
        }
    }

    /**
     * Performs the API calls, including a retry logic if the call fails.
     * If global counters (globalSuccessCount and globalFailureCount) are provided,
     * the method will update these counters with the number of successful and failed API calls respectively.
     * Otherwise, it will execute the API calls without updating global counters.
     *
     * @param apiInstance   The API instance used for making the actual calls.
     * @param apiPairCount  The number of API pairs (POST followed by GET) to be executed.
     * @param globalSuccessCount   An atomic counter to track the total number of successful API calls across all threads.
     *                             If null, successful API calls won't be tracked globally.
     * @param globalFailureCount   An atomic counter to track the total number of failed API calls across all threads.
     *                             If null, failed API calls won't be tracked globally.
     */
    private static void apiCallTask(DefaultApi apiInstance, int apiPairCount, AtomicInteger globalSuccessCount, AtomicInteger globalFailureCount) {
        AlbumsProfile profile = new AlbumsProfile();

        int localSuccessCount = 0;
        int localFailureCount = 0;

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
                            localFailureCount++;
                            // Log or handle max retry attempts reached
                            System.out.println("Max retry attempts reached for POST call.");
                        }
                    } else {
                        successPost = true;
                        localSuccessCount++;
                    }
                } catch (Exception e) {
                    retryCountPost++;
                    if (retryCountPost >= 5) {
                        localFailureCount++;
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
                            localFailureCount++;
                            // Log or handle max retry attempts reached
                            System.out.println("Max retry attempts reached for GET call.");
                        }
                    } else {
                        successGet = true;
                        localSuccessCount++;
                    }
                } catch (Exception e) {
                    retryCountGet++;
                    if (retryCountGet >= 5) {
                        localFailureCount++;
                        e.printStackTrace();
                    }
                }
            }
        }

        // Update the global counters if it is not null
        if (globalSuccessCount != null)
            globalSuccessCount.addAndGet(localSuccessCount);
        if (globalFailureCount != null)
            globalFailureCount.addAndGet(localFailureCount);
    }
}
