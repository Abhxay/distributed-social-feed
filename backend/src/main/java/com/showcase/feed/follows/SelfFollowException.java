package com.showcase.feed.follows;

public class SelfFollowException extends RuntimeException {
    public SelfFollowException(String message) {
        super(message);
    }
}
