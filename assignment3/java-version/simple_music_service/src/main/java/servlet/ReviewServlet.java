package servlet;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import dto.ErrorMsg;
import rmqpool.RMQChannelFactory;
import rmqpool.RMQChannelPool;
import service.AlbumService;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "Servlet.ReviewServlet", value = "/review")
public class ReviewServlet extends HttpServlet {
    private Connection connection;
    private AlbumService albumService;
    private Gson gson;
    private RMQChannelPool channelPool;

    @Override
    public void init() throws ServletException {
        super.init();
        gson = new Gson();
        albumService = new AlbumService();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv("RABBITMQ_ADDRESS"));
        try {
            connection = factory.newConnection();
            RMQChannelFactory channelFactory = new RMQChannelFactory(connection);
            int poolSize = 30;
            channelPool = new RMQChannelPool(poolSize, channelFactory);
        } catch (IOException | TimeoutException e) {
            throw new ServletException("Failed to create connection or channel pool", e);
        }
    }

    /**
     * Validates the URL format.
     *
     * @param pathParts An array of URL segments.
     * @return true if the URL is valid, false otherwise.
     */
    private boolean isUrlValid(String[] pathParts) {
        // urlPath  = /{likeornot}/{albumID}
        // urlParts = [, likeornot, albumId]
        return pathParts.length == 3 && "".equals(pathParts[0]) && (pathParts[1].equals("like") || pathParts[1].equals("dislike")) && !pathParts[2].isEmpty();
    }

    private boolean albumExists(String albumId) throws SQLException {
        return albumService.getAlbum(UUID.fromString(albumId)) != null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String pathInfo = req.getPathInfo(); // /{likeornot}/{albumID}
        // Check if we have a URL path
        if (pathInfo == null || pathInfo.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ErrorMsg noParameterError = new ErrorMsg("Missing parameters");
            res.getWriter().write(gson.toJson(noParameterError));
            return;
        }
        String[] pathParts = pathInfo.split("/");
        if (!isUrlValid(pathParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ErrorMsg("Invalid URL format")));
            return;
        }
        String likeOrNot = pathParts[1];
        String albumId = pathParts[2];

        try {
            if (!albumExists(albumId)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().write(gson.toJson(new ErrorMsg("Album not found")));
                return;
            }
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write(gson.toJson(new ErrorMsg("Failed to check AlbumId")));
            e.printStackTrace();
            return;
        }
        try {
            Channel channel = channelPool.borrowObject();
            try {
                channel.queueDeclare("reviewQueue", false, false, false, null);
                // publish to MQ
                String message = likeOrNot + "," + albumId;
                channel.basicPublish("", "reviewQueue", null, message.getBytes());
                res.setStatus(HttpServletResponse.SC_CREATED);
            } finally {
                channelPool.returnObject(channel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (channelPool != null) {
            channelPool.close();
        }
    }

}

