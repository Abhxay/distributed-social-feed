package com.showcase.feed.posts;

public class NotPostAuthorException extends RuntimeException {
    public NotPostAuthorException(String message) {
        super(message);
    }
}
