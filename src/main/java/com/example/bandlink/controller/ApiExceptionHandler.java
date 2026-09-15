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
    @ExceptionHandler({PostService.RuleViolationException.class, MessageService.RuleViolationException.class, ProfileService.RuleViolationException.class})
    ResponseEntity<Error> rule(RuntimeException e) { return response(HttpStatus.CONFLICT, "RULE_VIOLATION", e.getMessage()); }
    @ExceptionHandler(AuthService.EmailAlreadyUsedException.class)
    ResponseEntity<Error> duplicate(RuntimeException e) { return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", e.getMessage()); }
    @ExceptionHandler(AuthService.InvalidTokenException.class)
    ResponseEntity<Error> token(RuntimeException e) { return response(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", e.getMessage()); }
    @ExceptionHandler(AuthService.TooManyAttemptsException.class)
    ResponseEntity<Error> tooManyAttempts(RuntimeException e) { return response(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", e.getMessage()); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Error> auth(Exception e) { return response(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "メールアドレスとパスワードを確認してください。"); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Error> denied(Exception e) { return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作は許可されていません。"); }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Error> missing(Exception e) { return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "対象が見つかりません。"); }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Error> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(new Error("REQUEST_REJECTED", e.getReason() == null ? "処理できませんでした。" : e.getReason())); }
    // NFT-013: the database's own NOT NULL/FK/UNIQUE/CHECK constraints are the last line of defense
    // against a race an application-level check cannot fully close (e.g. two concurrent requests
    // both passing a check-then-insert check before either commits - the same class of bug NFT-003,
    // NFT-004 and NFT-005 each found and fixed one instance of, with a specific recovery for each).
    // This is the generic backstop for every OTHER case that is not worth a bespoke recovery (a
    // losing request here really should just be rejected, e.g. two people racing to register the
    // same email, or to block the same user twice): without this handler, any constraint violation
    // that reaches this point uncaught falls through to Spring Boot's default error handling as a
    // raw 500, which is exactly what NFT-013 requires never happens - every DB-level rejection must
    // surface to the caller as a 4xx with a safe, generic message, never a raw 500 or SQL detail.
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<Error> dataIntegrity(org.springframework.dao.DataIntegrityViolationException e) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT", "この操作は完了できませんでした。時間をおいて再度お試しください。");
    }
    // A handful of endpoints (e.g. POST /api/messages/images) declare produces=text/plain for their
    // success body and the frontend sends a matching Accept: text/plain. Error bodies here are still
    // JSON, so without forcing the content type explicitly, Spring's content negotiation finds no
    // acceptable representation for that Accept header and fails a second time while trying to report
    // the first error - the caller sees an empty, unhandled 500 instead of the intended 4xx with a
    // readable message. Forcing JSON here makes every error response independent of what the
    // matching success response happens to produce.
    private ResponseEntity<Error> response(HttpStatus status, String code, String message) { return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new Error(code, message)); }
}
