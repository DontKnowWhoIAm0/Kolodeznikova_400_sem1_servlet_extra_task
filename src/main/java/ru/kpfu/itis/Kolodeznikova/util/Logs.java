package ru.kpfu.itis.Kolodeznikova.util;

import java.time.LocalDateTime;

public class Logs {

    public enum Status {
        SUCCESS,
        FAILURE,
        TIMEOUT
    }

    public static void logOperation(String operation, Long postId, Status status, String message) {
        String timestamp = LocalDateTime.now().toString();
        System.out.printf("Operation: %s, Post ID: %s, Time: %s, Status: %s, Message: %s", operation, postId, timestamp, status, message);
    }

    public static void logSuccess(String operation, Long postId) {
        logOperation(operation, postId, Status.SUCCESS, "");
    }

    public static void logFailure(String operation, Long postId, String message) {
        logOperation(operation, postId, Status.FAILURE, message);
    }

    public static void logTimeout(String operation, Long postId, String message) {
        logOperation(operation, postId, Status.TIMEOUT, message);
    }

}
