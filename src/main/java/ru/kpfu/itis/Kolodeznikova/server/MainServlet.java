package ru.kpfu.itis.Kolodeznikova.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.kpfu.itis.Kolodeznikova.entity.Post;
import ru.kpfu.itis.Kolodeznikova.util.Logs;

@WebServlet(name="Posts Page", urlPatterns = {"/posts", "/api/posts/*"})
public class MainServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final Gson gson = new Gson();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();

        if (uri.startsWith(req.getContextPath() + "/api/posts")) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            String pathInfo = req.getPathInfo();

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                String id = pathInfo.substring(1);
                List<Post> allPosts = getAllPosts("ASC");
                Post post = allPosts.stream()
                        .filter(p -> p.getId().equals(Long.parseLong(id)))
                        .findFirst().orElse(null);

                if (post != null) {
                    resp.getWriter().write(gson.toJson(post));
                } else {
                    resp.setStatus(404);
                }
                return;
            }

            String order = req.getParameter("order");
            if (!"ASC".equalsIgnoreCase(order) && !"DESC".equalsIgnoreCase(order)) {
                order = "ASC";
            }
            String pageStr = req.getParameter("page");
            int page = Integer.parseInt(pageStr != null ? pageStr : "1");
            int index = (page - 1) * PAGE_SIZE;

            List<Post> allPosts = getAllPosts(order);
            List<Post> pagePosts = getPostsForPage(allPosts, index);
            int totalPages = (int) Math.ceil((double) allPosts.size() / PAGE_SIZE);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", pagePosts);
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            response.put("order", order);

            resp.getWriter().write(gson.toJson(response));
            Logs.logSuccess("READ", null);
            return;
        }

        String order = req.getParameter("order");
        if (!"ASC".equalsIgnoreCase(order) && !"DESC".equalsIgnoreCase(order)) {
            order = "ASC";
        }
        String pageStr = req.getParameter("page");
        int page;
        try {
            page = Integer.parseInt(pageStr);
        } catch (NumberFormatException e) {
            page = 1;
        }
        int pageSize = 10;
        int index = (page - 1) * pageSize;
        List<Post> allPosts = getAllPosts(order);
        List<Post> pagePosts = getPostsForPage(allPosts, index);

        int totalPages = (int) Math.ceil((double) allPosts.size() / pageSize);

        Logs.logSuccess("READ", null);

        req.setAttribute("order", order);
        req.setAttribute("posts", pagePosts);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/WEB-INF/templates/posts/posts.ftl").forward(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("create".equals(action)) {
            try {
                String title = req.getParameter("title");
                String body = req.getParameter("body");
                String userId = req.getParameter("userId");

                Post newPost = createPost(title, body, Long.parseLong(userId));
                Logs.logSuccess("CREATE", newPost.getId());

                resp.setContentType("application/json");
                resp.getWriter().write(gson.toJson(newPost));
            } catch (SocketTimeoutException e) {
                Logs.logTimeout("CREATE", null, e.getMessage());
                resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            } catch (Exception e) {
                Logs.logFailure("CREATE", null, e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            return;
        }
        resp.sendRedirect(req.getRequestURI() + "?page=1&order=ASC");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            BufferedReader reader = req.getReader();
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            Post updatedPost = gson.fromJson(json.toString(), Post.class);
            updatePost(updatedPost);
            Logs.logSuccess("UPDATE", updatedPost.getId());

            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(updatedPost));
        } catch (SocketTimeoutException e) {
            Logs.logTimeout("UPDATE", null, e.getMessage());
            resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
        } catch (Exception e) {
            Logs.logFailure("UPDATE", null, e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String postIdStr = req.getParameter("id");
        Long postId = Long.parseLong(postIdStr);
        try {
            deletePost(postId);
            Logs.logSuccess("DELETE", postId);
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"status\":\"deleted\"}");
        } catch (SocketTimeoutException e) {
            Logs.logTimeout("DELETE", postId, e.getMessage());
            resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
        } catch (Exception e) {
            Logs.logFailure("DELETE", postId, e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private List<Post> getAllPosts(String order) throws IOException {
        URL url = new URL("https://jsonplaceholder.typicode.com/posts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);

        try {
            String response = readResponse(conn);
            List<Post> posts = gson.fromJson(response, new TypeToken<List<Post>>(){}.getType());

            if ("DESC".equalsIgnoreCase(order)) {
                posts.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));
            } else {
                posts.sort(Comparator.comparing(Post::getId));
            }
            Logs.logSuccess("READ", null);
            return posts;
        } catch (SocketTimeoutException e) {
            Logs.logTimeout("READ", null, e.getMessage());
            throw e;
        } catch (IOException e) {
            Logs.logFailure("READ", null, e.getMessage());
            throw e;
        } finally {
            conn.disconnect();
        }
    }

    private List<Post> getPostsForPage(List<Post> allPosts, int index) {
        int endIndex = Math.min(index + PAGE_SIZE, allPosts.size());
        return allPosts.subList(index, endIndex);
    }

    private Post createPost(String title, String body, long userId) throws IOException {
        Post post = new Post();
        post.setTitle(title);
        post.setBody(body);
        post.setUserId(userId);

        String json = gson.toJson(post);
        URL url = new URL("https://jsonplaceholder.typicode.com/posts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try {
            String response = readResponse(conn);
            return gson.fromJson(response, Post.class);
        } finally {
            conn.disconnect();
        }
    }

    private void updatePost(Post post) throws IOException {
        String json = gson.toJson(post);
        URL url = new URL("https://jsonplaceholder.typicode.com/posts/" + post.getId());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try {
            readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    private void deletePost(Long id) throws IOException {
        URL url = new URL("https://jsonplaceholder.typicode.com/posts/" + id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        conn.connect();
        conn.disconnect();
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        conn.connect();
        int status = conn.getResponseCode();

        InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
