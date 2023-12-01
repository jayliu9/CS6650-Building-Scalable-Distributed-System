import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.AlbumInfo;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;
import utils.ApiCommand;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This program (Client2) simulates load testing on an API endpoint by dispatching multiple threads to make POST and GET calls.
 */
public class LoadTester {
    private static final int INIT_THREADS = 10;
    private static final int INIT_REQUEST_PAIRS = 100;
    private static final int REQUEST_PAIRS_PER_THREAD = 100;
    private static final File IMAGE_FILE = new File("image_example.png");

    // Thread-safe queue for storing the records of API calls.
    private static final ConcurrentLinkedQueue<Record> recordsQueue = new ConcurrentLinkedQueue<>();


    public static void main(String[] args) throws IOException {
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
        LikeApi likeApiInstance = new LikeApi(apiClient);
        // Initialize variables to count the number of successful and failed requests
        AtomicInteger globalSuccessCount = new AtomicInteger(0);
        AtomicInteger globalFailureCount = new AtomicInteger(0);

        // Create a thread pool executor
        ExecutorService executor = Executors.newFixedThreadPool(numThreadGroups * threadGroupSize);

        // Initialization phase: run some threads to "warm-up" the system
        CountDownLatch initLatch = new CountDownLatch(INIT_THREADS);
        runThreads(executor,
                apiInstance,
                likeApiInstance,INIT_THREADS,
                INIT_REQUEST_PAIRS, initLatch,
                null,
                null,
                false);

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
            runThreads(executor,
                    apiInstance,
                    likeApiInstance,
                    threadGroupSize,
                    REQUEST_PAIRS_PER_THREAD,
                    totalLatch,
                    globalSuccessCount,
                    globalFailureCount,
                    true);
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

        // Calculate and print statistics after all requests
        printStatistics();
        writeRecordsToFile();
    }

    /**
     * Schedules a specified number of threads to execute API call tasks. Optionally adds the records of the API calls to a queue.
     * If global counters (globalSuccessCount and globalFailureCount) are provided,
     * this method will track and record the number of global successful and failed API calls.
     * Otherwise, it will execute the tasks without updating global counters.
     *
     * @param executor      The executor service used for managing thread execution.
     * @param apiInstance   The API instance used for making the actual calls to Albums endpoints.
     * @param likeApiInstance The API instance used for making the actual calls to Review endpoints
     * @param numOfThreads  The number of threads to be scheduled for the task.
     * @param apiPairCount  The number of API pairs (POST followed by GET) each thread should execute.
     * @param latch         The countdown latch used to synchronize the completion of the threads.
     * @param globalSuccessCount   An atomic counter to track the total number of successful API calls across all threads.
     *                             If null, successful API calls won't be tracked globally.
     * @param globalFailureCount   An atomic counter to track the total number of failed API calls across all threads.
     *                             If null, failed API calls won't be tracked globally.
     * @param recordWrites  Whether to add the records of the API calls to the records queue.
     */
    private static void runThreads(ExecutorService executor, DefaultApi apiInstance, LikeApi likeApiInstance, int numOfThreads, int apiPairCount, CountDownLatch latch, AtomicInteger globalSuccessCount, AtomicInteger globalFailureCount, boolean recordWrites) {
        for (int i = 0; i < numOfThreads; i++) {
            executor.submit(() -> {
                apiCallTask(apiInstance, likeApiInstance, apiPairCount, globalSuccessCount, globalFailureCount, recordWrites);
                latch.countDown();
            });
        }
    }

    /**
     * Performs the API calls, including a retry logic if the call fails. Optionally adds the records
     * of the API calls to a queue.
     * If global counters (globalSuccessCount and globalFailureCount) are provided,
     * the method will update these counters with the number of successful and failed API calls respectively.
     * Otherwise, it will execute the API calls without updating global counters.
     *
     * @param apiInstance   The API instance used for making the actual calls to Album endpoints
     * @param likeApiInstance The API instance used for making the actual calls to Review endpoints
     * @param apiPairCount  The number of API pairs (POST followed by GET) to be executed.
     * @param globalSuccessCount   An atomic counter to track the total number of successful API calls across all threads.
     *                             If null, successful API calls won't be tracked globally.
     * @param globalFailureCount   An atomic counter to track the total number of failed API calls across all threads.
     *                             If null, failed API calls won't be tracked globally.
     * @param recordWrites  Whether to add the records of the API calls to the records queue.
     */
    private static void apiCallTask(DefaultApi apiInstance, LikeApi likeApiInstance, int apiPairCount, AtomicInteger globalSuccessCount, AtomicInteger globalFailureCount, boolean recordWrites) {
        AlbumsProfile profile = new AlbumsProfile();
        profile.setYear("1980");    // Hard-code for the year of a testing album profile
        profile.setArtist("Artist");    // Hard-code for the artist of a testing album profile
        profile.setTitle("Moon");   // Hard-code for the title of a testing album profile

        int localSuccessCount = 0;
        int localFailureCount = 0;

        for (int i = 0; i < apiPairCount; i++) {
            // Retry for POST API call
            String albumId = null;
            ApiResponse<ImageMetaData> albumPostResponse = executeWithRetry(() -> apiInstance.newAlbumWithHttpInfo(IMAGE_FILE, profile),
                    "POST",
                    "albums",
                    5,
                    recordWrites);

            if (albumPostResponse == null) {
                localFailureCount++;
                continue;
            }
            localSuccessCount++;
            albumId = albumPostResponse.getData().getAlbumID();

            String finalAlbumId = albumId;
            ApiResponse<Void> reviewPostResponse = executeWithRetry(() -> likeApiInstance.reviewWithHttpInfo("like", finalAlbumId),
                    "Post",
                    "review",
                    5,
                    recordWrites);
            if (reviewPostResponse == null) {
                localFailureCount++;
            } else {
                localSuccessCount++;
            }
            reviewPostResponse = executeWithRetry(() -> likeApiInstance.reviewWithHttpInfo("like", finalAlbumId),
                    "Post",
                    "review",
                    5,
                    recordWrites);
            if (reviewPostResponse == null) {
                localFailureCount++;
            } else {
                localSuccessCount++;
            }
            reviewPostResponse = executeWithRetry(() -> likeApiInstance.reviewWithHttpInfo("dislike", finalAlbumId),
                    "Post",
                    "review",
                    5,
                    recordWrites);
            if (reviewPostResponse == null) {
                localFailureCount++;
            } else {
                localSuccessCount++;
            }
        }
        // Update the global counters if it is not null
        if (globalSuccessCount != null)
            globalSuccessCount.addAndGet(localSuccessCount);
        if (globalFailureCount != null)
            globalFailureCount.addAndGet(localFailureCount);
    }

    /**
     * Retry logic for api calls
     * @param command the function to make an API call
     * @param requestType the request type of this API call
     * @param apiEndPoint the endpoints of the API call
     * @param maxRetries the maximum retry times
     * @param recordWrites Whether to add the records of the API calls to the records queue.
     * @param <T> the return type of the response from the API call
     * @return The response of the API call
     */
    private static <T> ApiResponse<T> executeWithRetry(ApiCommand command, String requestType, String apiEndPoint, int maxRetries, boolean recordWrites) {
        int retryCount = 0;
        ApiResponse<T> response = null;
        boolean success = false;
        while (retryCount < maxRetries && !success) {
            try {
                long startTime = System.currentTimeMillis();
                response = command.execute();
                int statusCode = response.getStatusCode();
                if (statusCode >= 400 && statusCode < 600) {
                    retryCount++;
                } else {
                    long endTime = System.currentTimeMillis();
                    if (recordWrites) {
                        recordsQueue.add(new Record(startTime, requestType, endTime - startTime, statusCode, apiEndPoint));
                    }
                    success = true;
                }
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    e.printStackTrace();
                }
            }
        }
        if (!success) {
            System.out.println("Max retry attempts reached for " + requestType + " call.");
        }
        return response;
    }

    /**
     * Writes all the API call records to an output file.
     */
    private static void writeRecordsToFile() throws IOException {
        try (FileWriter writer = new FileWriter("output.csv")) {
            writer.append("StartTime,RequestType,Latency,ResponseCode,ApiEndpoint\n"); // 修改表头
            while (!recordsQueue.isEmpty()) {
                Record record = recordsQueue.poll();
                writer.append(record.toString()).append("\n");
            }
        }
    }

    /**
     * Calculates and displays statistics for the API calls.
     */
    private static void printStatistics() {
        Map<String, List<Long>> latenciesByEndpoint = new HashMap<>();

        for (Record record : recordsQueue) {
            latenciesByEndpoint.computeIfAbsent(record.apiEndPoint, k -> new ArrayList<>()).add(record.latency);
        }

        for (Map.Entry<String, List<Long>> entry : latenciesByEndpoint.entrySet()) {
            System.out.println("\nPost Statistics for " + entry.getKey() + ":");
            printLatencyStats(entry.getValue());
        }
    }


    /**
     * Displays latency statistics from a list of latencies.
     *
     * @param latencies A list of latencies for which to calculate and display statistics.
     */
    private static void printLatencyStats(List<Long> latencies) {
        Collections.sort(latencies);
        long sum = 0;
        for (long latency : latencies) {
            sum += latency;
        }

        double mean = sum / (double) latencies.size();
        long median = latencies.size() % 2 == 0 ?
                (latencies.get(latencies.size() / 2 - 1) + latencies.get(latencies.size() / 2)) / 2 : latencies.get(latencies.size() / 2);
        long p99 = latencies.get((int) (0.99 * latencies.size()));
        long min = latencies.get(0);
        long max = latencies.get(latencies.size() - 1);

        System.out.println("Mean response time: " + mean + " ms");
        System.out.println("Median response time: " + median + " ms");
        System.out.println("99th percentile response time: " + p99 + " ms");
        System.out.println("Min response time: " + min + " ms");
        System.out.println("Max response time: " + max + " ms");
    }

    /**
     * A representation of an API call record.
     */
    private static class Record {
        long startTime;
        String requestType;
        long latency;
        int responseCode;
        String apiEndPoint;

        public Record(long startTime, String requestType, long latency, int responseCode, String apiEndPoint) {
            this.startTime = startTime;
            this.requestType = requestType;
            this.latency = latency;
            this.responseCode = responseCode;
            this.apiEndPoint = apiEndPoint;
        }

        @Override
        public String toString() {
            return startTime + "," + requestType + "," + latency + "," + responseCode + "," + apiEndPoint;
        }
    }


}
