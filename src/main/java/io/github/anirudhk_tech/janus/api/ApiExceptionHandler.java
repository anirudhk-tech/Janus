package io.github.anirudhk_tech.janus.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.anirudhk_tech.janus.federation.FederationExecutionException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        
        body.put("timestamp", Instant.now().toString());
        body.put("status", 400);
        body.put("error", "bad_request");
        body.put("message", "validation_failed");
        body.put("path", request.getRequestURI());

        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (a, b) -> a,
                LinkedHashMap::new
            ));

        body.put("field_errors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(FederationExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleFederationExecution(
        FederationExecutionException ex,
        HttpServletRequest request
    ) {
        boolean isTimeout =
            hasCause(ex, TimeoutException.class) ||
            Optional.ofNullable(ex.getMessage()).orElse("").contains("deadline_exceeded");

        HttpStatus status = isTimeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
        String error = isTimeout ? "gateway_timeout" : "bad_gateway";

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", "query_execution_failed");
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }

    private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable cur = ex;
        while (cur != null) {
            if (type.isInstance(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
