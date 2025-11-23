// src/main/java/in/bored/api/config/FirebaseAuthenticationFilter.java
package in.bored.api.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@Order(20)
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // If no bearer token, just continue (anonymous access)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractBearerToken(header);

            // Verify token with Firebase Admin SDK
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);

            String uid = decoded.getUid();   // 🔑 this is our main key
            // String email = decoded.getEmail(); // use if needed

            // principal = uid, no roles/authorities
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(uid, null, Collections.emptyList());

            // Optional: attach full decoded token as details
            auth.setDetails(decoded);

            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("❌ Firebase authentication failed: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String extractBearerToken(String header) {
        // "Bearer <token>"
        return header.substring("Bearer ".length()).trim();
    }
}