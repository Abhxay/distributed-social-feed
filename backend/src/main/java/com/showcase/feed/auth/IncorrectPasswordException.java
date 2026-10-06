package com.showcase.feed.auth;

public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException() {
        super("current password is incorrect");
    }
}
