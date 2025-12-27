package ru.kpfu.itis.Kolodeznikova.util;

public class Logs {

    public static void logOperation(String operation, Long postId) {
        String timestamp = java.time.LocalDateTime.now().toString();
        System.out.printf("Operation: %s, Post ID: %s, Time: %s, Status: SUCCESS", operation, postId, timestamp);
    }

}
