package ru.kpfu.itis.Kolodeznikova.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.kpfu.itis.Kolodeznikova.entity.Post;
import ru.kpfu.itis.Kolodeznikova.util.Logs;

@WebServlet(name="Posts Page", urlPatterns = "/posts")
public class MainServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final Gson gson = new Gson();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        Logs.logOperation("READ", null);

        req.setAttribute("order", order);
        req.setAttribute("posts", pagePosts);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/WEB-INF/views/posts.jsp").forward(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("create".equals(action)) {
            String title = req.getParameter("title");
            String body = req.getParameter("body");
            String userId = req.getParameter("userId");

            Post newPost = createPost(title, body, Long.parseLong(userId));
            Logs.logOperation("CREATE", newPost.getId());

            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(newPost));
            return;
        }
        resp.sendRedirect(req.getRequestURI() + "?page=1&order=ASC");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BufferedReader reader = req.getReader();
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }

        Post updatedPost = gson.fromJson(json.toString(), Post.class);
        updatePost(updatedPost);
        Logs.logOperation("UPDATE", updatedPost.getId());

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(updatedPost));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String postIdStr = req.getParameter("id");
        Long postId = Long.parseLong(postIdStr);
        deletePost(postId);
        Logs.logOperation("DELETE", postId);
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"status\":\"deleted\"}");
    }

    private List<Post> getAllPosts(String order) throws IOException {
        URL url = new URL("https://jsonplaceholder.typicode.com/posts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        String response = readResponse(conn);
        List<Post> posts = gson.fromJson(response, new TypeToken<List<Post>>(){}.getType());

        if ("DESC".equalsIgnoreCase(order)) {
            posts.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));
        } else {
            posts.sort(Comparator.comparing(Post::getId));
        }
        return posts;
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

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        String response = readResponse(conn);
        return gson.fromJson(response, Post.class);
    }

    private void updatePost(Post post) throws IOException {
        String json = gson.toJson(post);
        URL url = new URL("https://jsonplaceholder.typicode.com/posts/" + post.getId());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        String response = readResponse(conn);
    }

    private void deletePost(Long id) throws IOException {
        URL url = new URL("https://jsonplaceholder.typicode.com/posts/" + id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.connect();
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        conn.connect();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
