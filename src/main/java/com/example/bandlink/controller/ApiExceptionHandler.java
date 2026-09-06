package com.example.bandlink.controller;

import com.example.bandlink.service.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.NoSuchElementException;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {
    public record Error(String code, String message) {}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Error> validation(MethodArgumentNotValidException e) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "入力内容を確認してください。必須項目や文字数制限に合わない項目があります。");
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<Error> invalid(Exception e) { return response(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "入力内容を確認してください。"); }
    @ExceptionHandler({PostService.RuleViolationException.class, MessageService.RuleViolationException.class})
    ResponseEntity<Error> rule(RuntimeException e) { return response(HttpStatus.CONFLICT, "RULE_VIOLATION", e.getMessage()); }
    @ExceptionHandler(AuthService.EmailAlreadyUsedException.class)
    ResponseEntity<Error> duplicate(RuntimeException e) { return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", e.getMessage()); }
    @ExceptionHandler(AuthService.InvalidTokenException.class)
    ResponseEntity<Error> token(RuntimeException e) { return response(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", e.getMessage()); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Error> auth(Exception e) { return response(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "メールアドレスとパスワードを確認してください。"); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Error> denied(Exception e) { return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作は許可されていません。"); }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Error> missing(Exception e) { return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "対象が見つかりません。"); }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Error> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(new Error("REQUEST_REJECTED", e.getReason() == null ? "処理できませんでした。" : e.getReason())); }
    private ResponseEntity<Error> response(HttpStatus status, String code, String message) { return ResponseEntity.status(status).body(new Error(code, message)); }
}
