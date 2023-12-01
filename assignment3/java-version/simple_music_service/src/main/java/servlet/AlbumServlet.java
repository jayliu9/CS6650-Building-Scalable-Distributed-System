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

@WebServlet(name = "Servlet.AlbumServlet", value = "/albums")
@MultipartConfig
public class AlbumServlet extends HttpServlet {
    private AlbumService albumService;
    private Gson gson;
    private AlbumMapper albumMapper;

    @Override
    public void init() {
        albumMapper = AlbumMapper.INSTANCE;
        albumService = new AlbumService();
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
            UUID albumId = UUID.fromString(urlParts[1]);
            Album album = null;
            try {
                album = albumService.getAlbum(albumId);
            } catch (SQLException e) {
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
        // urlPath  = "/album123"
        // urlParts = [, album123]
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
        UUID albumId = UUID.randomUUID();
        Album newAlbum = albumMapper.albumProfileToAlbum(albumProfile, albumId);

        // Extract image part from the request
        Part imagePart = req.getPart("image");
        if (imagePart == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ErrorMsg("Missing image data")));
            return;
        }
        long imageSize = imagePart.getSize();
        try (InputStream imageInputStream = imagePart.getInputStream()) {
            albumService.createAlbum(newAlbum, imageInputStream, imageSize);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write(gson.toJson(new ErrorMsg("Failed to save album and image")));
            return;
        }

        // Create a response
        ImageMetaData responseData = new ImageMetaData(String.valueOf(albumId), String.valueOf(imageSize));
        // Send the response back as JSON
        res.getWriter().write(gson.toJson(responseData));
    }
}


