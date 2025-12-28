package io.github.anirudhk_tech.janus.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
