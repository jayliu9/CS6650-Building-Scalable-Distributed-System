package servlet;

import com.google.gson.Gson;
import dto.AlbumInfo;
import dto.AlbumProfile;
import dto.ErrorMsg;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;

import dto.ImageMetaData;
import mapper.AlbumMapper;
import model.Album;
import service.AlbumService;
import service.ReviewService;

@WebServlet(name = "Servlet.AlbumServlet", value = "/albums")
@MultipartConfig
public class AlbumServlet extends HttpServlet {
    private AlbumService albumService;
    private ReviewService reviewService;
    private Gson gson;
    private AlbumMapper albumMapper;

    @Override
    public void init() {
        albumMapper = AlbumMapper.INSTANCE;
        albumService = new AlbumService();
        reviewService = new ReviewService();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String urlPath = req.getPathInfo();

        // Check if we have a URL path
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ErrorMsg noParameterError = new ErrorMsg("missing parameters");
            res.getWriter().write(gson.toJson(noParameterError));
            return;
        }
        String[] urlParts = urlPath.split("/");

        // Validate URL path and return the appropriate response
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ErrorMsg invalidUrlError = new ErrorMsg("invalid URL format");
            res.getWriter().write(gson.toJson(invalidUrlError));
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // Retrieve album data using the provided ID in the URL
            int albumId = Integer.parseInt(urlParts[1]);
            Album album = null;
            try {
                album = albumService.getAlbum(albumId);
            } catch (SQLException e) {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().write(gson.toJson(new ErrorMsg("Failed to get album")));
                e.printStackTrace();
            }
            if (album == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().write(gson.toJson(new ErrorMsg("album not found")));
                return;
            }
            AlbumInfo albumInfo = albumMapper.albumToAlbumInfo(album);
            String albumJson = gson.toJson(albumInfo);

            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write(albumJson);
        }
    }

    /**
     * Validates the URL format.
     *
     * @param urlPath An array of URL segments.
     * @return true if the URL is valid, false otherwise.
     */
    private boolean isUrlValid(String[] urlPath) {
        // urlPath  = "/123"
        // urlParts = [, 123]
        return urlPath.length == 2 && "".equals(urlPath[0]);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Part profilePart = req.getPart("profile");
        if (profilePart == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ErrorMsg("Missing album profile")));
            return;
        }
        String profileJson = new String(profilePart.getInputStream().readAllBytes());
        AlbumProfile albumProfile = gson.fromJson(profileJson, AlbumProfile.class);
        if (albumProfile == null || albumProfile.getArtist() == null || albumProfile.getTitle() == null || albumProfile.getYear() == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ErrorMsg("Invalid album profile")));
            return;
        }
        Album newAlbum = albumMapper.albumProfileToAlbum(albumProfile);

        // Extract image part from the request
        Part imagePart = req.getPart("image");
        if (imagePart == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ErrorMsg("Missing image data")));
            return;
        }
        long imageSize = imagePart.getSize();
        int albumId = -1;
        try (InputStream imageInputStream = imagePart.getInputStream()) {
            albumId = albumService.createAlbum(newAlbum, imageInputStream, imageSize);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write(gson.toJson(new ErrorMsg("Failed to save album and image")));
            return;
        }
        // Initialize {albumId, like: 0, dislike: 0} in Redis
        reviewService.initializeAlbumLikesDislikes(String.valueOf(albumId));
        // Create a response
        ImageMetaData responseData = new ImageMetaData(String.valueOf(albumId), String.valueOf(imageSize));
        // Send the response back as JSON
        res.getWriter().write(gson.toJson(responseData));
    }
}


