package com.showcase.feed.common;

import com.showcase.feed.auth.IncorrectPasswordException;
import com.showcase.feed.auth.InvalidTokenException;
import com.showcase.feed.auth.TokenReuseDetectedException;
import com.showcase.feed.auth.UsernameTakenException;
import com.showcase.feed.common.idempotency.IdempotencyConflictException;
import com.showcase.feed.follows.SelfFollowException;
import com.showcase.feed.posts.InvalidImageException;
import com.showcase.feed.posts.NotPostAuthorException;
import com.showcase.feed.posts.PostNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, String>> handleIdempotencyConflict(IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<Map<String, String>> handleSelfFollow(SelfFollowException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<Map<String, String>> handleUsernameTaken(UsernameTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TokenReuseDetectedException.class)
    public ResponseEntity<Map<String, String>> handleTokenReuse(TokenReuseDetectedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "refresh token reuse detected, session revoked; please log in again"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<Map<String, String>> handleIncorrectPassword(IncorrectPasswordException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePostNotFound(PostNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NotPostAuthorException.class)
    public ResponseEntity<Map<String, String>> handleNotPostAuthor(NotPostAuthorException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<Map<String, String>> handleInvalidImage(InvalidImageException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "image must be 5MB or smaller"));
    }
}
