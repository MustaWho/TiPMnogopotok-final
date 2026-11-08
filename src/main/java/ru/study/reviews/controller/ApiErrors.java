package ru.study.reviews.controller;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> badRequest(IllegalArgumentException e) { return error(400, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        return error(400, "urls: от 1 до 20 непустых HTTP(S) ссылок");
    }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> missing(NoSuchElementException e) { return error(404, e.getMessage()); }
    @ExceptionHandler(RejectedExecutionException.class)
    ResponseEntity<?> busy(RejectedExecutionException e) { return error(429, e.getMessage()); }
    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
