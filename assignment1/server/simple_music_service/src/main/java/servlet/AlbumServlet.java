package servlet;

import com.google.gson.Gson;
import dao.AlbumDAO;
import model.AlbumInfo;
import model.AlbumPostResponse;
import model.ErrorMsg;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "Servlet.AlbumServlet", value = "/albums")
@MultipartConfig
public class AlbumServlet extends HttpServlet {
    private AlbumDAO albumDAO;
    private Gson gson;

    @Override
    public void init() {
        albumDAO = new AlbumDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String urlPath = req.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ErrorMsg noParameterError = new ErrorMsg("missing parameters");
            res.getWriter().write(gson.toJson(noParameterError));
            return;
        }
        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)

        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ErrorMsg invalidUrlError = new ErrorMsg("invalid URL format");
            res.getWriter().write(gson.toJson(invalidUrlError));
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // process url params in `urlParts`
            String albumId = urlParts[1];
            AlbumInfo albumInfo = albumDAO.getAlbumByKey(albumId);
            String albumJson = gson.toJson(albumInfo);

            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write(albumJson);
        }
    }

    private boolean isUrlValid(String[] urlPath) {
        // urlPath  = "/album123"
        // urlParts = [, album123]
        return urlPath.length == 2 && "".equals(urlPath[0]);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Part imagePart = req.getPart("image");
        long imageSize = imagePart.getSize();

        Part profilePart = req.getPart("profile");
        System.out.println(imagePart);
        System.out.println(profilePart);
        InputStream inputStream = profilePart.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        String profileData = sb.toString();
        AlbumInfo newAlbum = gson.fromJson(profileData, AlbumInfo.class);

        AlbumPostResponse albumPostResponse = new AlbumPostResponse("newAlbumId", String.valueOf(imageSize));

        res.getWriter().write(gson.toJson(albumPostResponse));
    }
}


