package io.github.anirudhk_tech.janus.auth;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString()); 
        body.put("status", 401); 
        body.put("error", "unauthorized"); 
        body.put("message", authException.getMessage()); 
        body.put("path", request.getRequestURI()); 

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); 
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
