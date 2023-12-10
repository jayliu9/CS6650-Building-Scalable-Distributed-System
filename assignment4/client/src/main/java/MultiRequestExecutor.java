import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import model.RequestRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class MultiRequestExecutor {

  private final String RECORD_FILE_PATH = "src/main/java/generatedFile/requestRecord.csv";
  private final String THROUGHPUT_FILE_PATH = "src/main/java/generatedFile/throughputPerSec.csv";
  private final Integer loopCnt = 100;
  private Integer threadGroupSize;
  private Integer numThreadGroups;
  private Integer delay;
  private String ipAddress;
  private List<List<RequestRecord>> records;
  private AtomicLong success;
  private AtomicLong getReviewSuccess;
  private final AtomicBoolean threadGroupFinished = new AtomicBoolean(false);


  public MultiRequestExecutor(Integer threadGroupSize, Integer numThreadGroups,
      Integer delay, String ipAddress) {
    this.threadGroupSize = threadGroupSize;
    this.numThreadGroups = numThreadGroups;
    this.delay = delay;
    this.ipAddress = ipAddress;
    this.records = Collections.synchronizedList(new ArrayList<>());
    this.success = new AtomicLong(0L);
    this.getReviewSuccess = new AtomicLong(0L);
  }

  public void execute() throws InterruptedException {
    FileWriter fileWriter;
    CSVPrinter csvPrinter;
    try {
      fileWriter = new FileWriter(RECORD_FILE_PATH, false);
      csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);
      csvPrinter.printRecord("startTime", "requestType", "latency", "responseCode");
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    CountDownLatch countDownLatch = new CountDownLatch(10);
    for (int i = 0; i < 10; i++) {
      AlbumRequestThread albumRequestThread = new AlbumRequestThread(100, this.ipAddress,
          countDownLatch, this.records, this.success);
      Thread thread = new Thread(albumRequestThread);
      thread.start();
    }

    countDownLatch.await();

    long startTime = System.currentTimeMillis();
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    System.out.println("START");
    countDownLatch = new CountDownLatch(this.threadGroupSize * this.numThreadGroups);
    for (int i = 0; i < this.numThreadGroups; i++) {
      for (int j = 0; j < this.threadGroupSize; j++) {
        AlbumRequestThread albumRequestThread = new AlbumRequestThread(loopCnt, this.ipAddress,
            countDownLatch, this.records, this.success);
        Thread thread = new Thread(albumRequestThread);
        thread.start();
      }

      if (i == 0) {
        // start 3 review query threads when the first group is complete
        for (int k = 0; k < 3; k++) {
          AlbumReviewThread albumReviewThread = new AlbumReviewThread(
              this.ipAddress, this.getReviewSuccess, this.threadGroupFinished);
          new Thread(albumReviewThread).start();
        }
      }
      Thread.sleep(delay * 1000);
    }

    countDownLatch.await();
    // stop 3 review query threads when all the groups are complete
    this.threadGroupFinished.set(true);

    long endTime = System.currentTimeMillis();

    List<RequestRecord> allRecords = new ArrayList<>();
    for (List<RequestRecord> record : records) {
      allRecords.addAll(record);
    }
    writeRequestRecord(csvPrinter, allRecords);

    double wallTime = (endTime - startTime) / 1000d - delay * numThreadGroups;
    Collections.sort(allRecords, (a, b) -> (int)(a.getLatency() - b.getLatency()));
    long meanLatency = -1, medianLatency = -1, p99Latency = -1, minLatency = -1, maxLatency = -1;
    int recordCnt = allRecords.size();

    // stat calc
    if (recordCnt > 0) {
      meanLatency = (allRecords.stream().mapToLong(a -> a.getLatency()).sum() / recordCnt);
      medianLatency = allRecords.get(recordCnt / 2).getLatency();
      p99Latency = allRecords.get(recordCnt / 100 * 99).getLatency();
      minLatency = allRecords.get(0).getLatency();
      maxLatency = allRecords.get(recordCnt - 1).getLatency();
    }

    // throughput per sec calc, output path: THROUGHPUT_FILE_PATH
    Collections.sort(allRecords, (a, b) -> (int)(a.getStartTime() - b.getStartTime()));
    try {
      fileWriter = new FileWriter(THROUGHPUT_FILE_PATH, false);
      csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);
      calcThroughputPerSec(csvPrinter, allRecords, startTime);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    try {
      csvPrinter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Wall Time: " + wallTime + "s");
    System.out.println("Throughput: " +
        ((long)this.numThreadGroups * this.threadGroupSize * this.loopCnt * 4 + this.getReviewSuccess.get()) / wallTime);
    System.out.println("Mean Response Time: " + meanLatency + "ms");
    System.out.println("Median Response Time: " + medianLatency + "ms");
    System.out.println("P99 Response Time: " + p99Latency + "ms");
    System.out.println("Minimum Response Time: " + minLatency + "ms");
    System.out.println("Maximum Response Time: " + maxLatency + "ms");
    System.out.println("Success Album Request Count: " + this.success.get());
    System.out.println("Success Get Review Request Count: " + this.getReviewSuccess.get());
    System.out.println("Failure Request Count: " +
        ((long)(this.numThreadGroups * this.threadGroupSize * loopCnt + 1000) * 4 - this.success.get()));
    System.out.println("END");
  }

  private void writeRequestRecord(final CSVPrinter csvPrinter, final List<RequestRecord> records) {
    for (RequestRecord record : records) {
      try {
        csvPrinter.printRecord(
            record.getStartTime(), record.getRequestType(),
            record.getLatency(), record.getResponseCode());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void calcThroughputPerSec(
      final CSVPrinter csvPrinter, final List<RequestRecord> records, final long initStartTime)
      throws IOException {
    csvPrinter.printRecord("Time(S)", "Throughput");
    long startTime = records.get(0).getStartTime();
    int sec = 1;
    long throughput = 0;

    for (RequestRecord record : records) {
      long curStartTime = record.getStartTime();
      if (curStartTime < initStartTime) {
        startTime = curStartTime;
        continue;
      }

      if (curStartTime - startTime < 1000) {
        throughput++;
      } else {
        csvPrinter.printRecord(sec, throughput);
        throughput = 1;
        sec++;
        startTime = curStartTime;
      }
    }
  }
}
