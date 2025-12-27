package io.github.anirudhk_tech.janus.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final ApiKeyService apiKeyService;
    private final JsonAuthenticationEntryPoint entryPoint;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService, JsonAuthenticationEntryPoint entryPoint) {
        this.apiKeyService = apiKeyService;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-Key");

        if (!apiKeyService.isValid(apiKey)) {
            entryPoint.commence(request, response, new BadCredentialsException("Missing or invalid X-API-Key"));
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
            "api-key-user", 
            null, 
            List.of(new SimpleGrantedAuthority("ROLE_API"))
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
