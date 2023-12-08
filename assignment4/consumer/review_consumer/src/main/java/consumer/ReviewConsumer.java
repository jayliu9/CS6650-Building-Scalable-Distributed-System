package consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import model.Review;
import rmqpool.RMQChannelFactory;
import rmqpool.RMQChannelPool;
import service.ReviewService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReviewConsumer {
    private final static String QUEUE_NAME = "reviewQueue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv("RABBITMQ_ADDRESS"));
        final Connection connection = factory.newConnection();

        RMQChannelFactory channelFactory = new RMQChannelFactory(connection);
        int poolSize = 30;
        RMQChannelPool channelPool = new RMQChannelPool(poolSize, channelFactory);

        int numConsumers = Integer.parseInt(System.getenv("NUM_OF_CONSUMERS"));
        ExecutorService executorService = Executors.newFixedThreadPool(numConsumers);

        for (int i = 0; i < numConsumers; i++) {
            executorService.submit(() -> {
                Channel channel = null;
                try {
                    channel = channelPool.borrowObject();
                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                    channel.basicQos(1);
                    System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        processMessage(message);
                        System.out.println("Callback thread ID = " + Thread.currentThread().getId() + " Received '" + message + "'");
                    };
                    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
                } catch (IOException ex) {
                    Logger.getLogger(ReviewConsumer.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    if (channel != null) {
                        try {
                            channelPool.returnObject(channel);
                        } catch (Exception ex) {
                            Logger.getLogger(ReviewConsumer.class.getName()).log(Level.SEVERE, "Failed to return channel to pool", ex);
                        }
                    }
                }
            });
        }


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            channelPool.close();
        }));
    }

    private static void processMessage(String message) {
        ReviewService reviewService = new ReviewService();
        String[] parts = message.split(",");
        UUID reviewID = UUID.randomUUID();
        Review review = new Review(reviewID, UUID.fromString(parts[1]), "like".equals(parts[0]));
        try {
            reviewService.createReview(review);
            System.out.println("Inserted review: " + review);
        } catch (SQLException  e) {
            e.printStackTrace();
            // Handle exception - maybe log it or send a notification
        }
    }

}
